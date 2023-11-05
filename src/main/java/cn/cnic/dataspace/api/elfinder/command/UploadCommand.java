/*
 * #%L
 * %%
 * Copyright (C) 2015 Trustsystems Desenvolvimento de Sistemas, LTDA.
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the Trustsystems Desenvolvimento de Sistemas, LTDA. nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package cn.cnic.dataspace.api.elfinder.command;

import cn.cnic.dataspace.api.elfinder.config.ElFinderConstants;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.VolumeHandler;
import cn.cnic.dataspace.api.util.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class UploadCommand extends AbstractJsonCommand implements ElfinderCommand {

    // large file will be splitted into many parts
    class Part {

        long _start;

        long _size;

        FileItemStream _content;

        public Part(long start, long size, FileItemStream fileItemStream) {
            super();
            this._start = start;
            this._size = size;
            this._content = fileItemStream;
        }
    }

    // a large file with many parts
    static class Parts {

        public static synchronized Parts getOrCreate(HttpServletRequest request, String chunkId, String fileName, long total, long totalSize) {
            // chunkId is not an unique number for files uploaded in one upload form
            String key = String.format("chunk_%s_%s", chunkId, fileName);
            // stores chunks in application context
            Parts parts = (Parts) request.getServletContext().getAttribute(key);
            if (parts == null) {
                parts = new Parts(chunkId, fileName, total, totalSize);
                request.getServletContext().setAttribute(key, parts);
            }
            return parts;
        }

        private String _chunkId;

        // number of parts
        private long _numberOfParts;

        private long _totalSize;

        private String _fileName;

        // all chunks
        Map<Long, Part> _parts = new HashMap<Long, Part>();

        public Parts(String chunkId, String fileName, long numberOfParts, long totalSize) {
            _chunkId = chunkId;
            _fileName = fileName;
            _numberOfParts = numberOfParts;
            _totalSize = totalSize;
        }

        public synchronized void addPart(long partIndex, Part part) {
            _parts.put(partIndex, part);
        }

        public boolean isReady() {
            return _parts.size() == _numberOfParts;
        }

        public InputStream openInputStream() throws IOException {
            return new InputStream() {

                long partIndex = 0;

                Part part = _parts.get(partIndex);

                InputStream is = part._content.openStream();

                @Override
                public int read() throws IOException {
                    while (true) {
                        // current part is not read completely
                        int c = is.read();
                        if (c != -1) {
                            return c;
                        }
                        // next part?
                        if (partIndex == _numberOfParts - 1) {
                            is.close();
                            return -1;
                        }
                        part = _parts.get(++partIndex);
                        is.close();
                        is = part._content.openStream();
                    }
                }
            };
        }

        public void checkParts() throws IOException {
            long totalSize = 0;
            for (long i = 0; i < _numberOfParts; i++) {
                Part part = _parts.get(i);
                totalSize += part._size;
            }
            if (totalSize != _totalSize) {
                throw new IOException(String.format("invalid file size: excepted %d, but is %d", _totalSize, totalSize));
            }
        }

        public void removeFromApplicationContext(HttpServletRequest request) {
            String key = String.format("chunk_%s_%s", _chunkId, _fileName);
            request.getServletContext().removeAttribute(key);
        }
    }

    interface FileWriter {

        VolumeHandler createAndSave(String fileName, InputStream is) throws IOException;
    }

    @Override
    protected void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, JSONObject json, ElfinderCommonService commonService) throws Exception {
        List<FileItemStream> files = (List<FileItemStream>) request.getAttribute(FileItemStream.class.getName());
        List<VolumeHandler> added = new ArrayList<>();
        String target = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_TARGET);
        // file = 1 ===== folder = 0
        String uploadDir = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_UPLOAD_DIR);
        FileWriter fw = (fileName, is) -> {
            VolumeHandler newFile = new VolumeHandler(findTarget(elfinderStorage, target), fileName);
            newFile.uploadFile(StringUtils.equals(uploadDir, "1"));
            OutputStream os = newFile.openOutputStream();
            IOUtils.copy(is, os);
            os.close();
            is.close();
            newFile.stateUpload(newFile.getSize());
            added.add(newFile);
            return newFile;
        };
        boolean flag;
        if (request.getParameter("cid") != null) {
            flag = processChunkUpload(request, files, fw, commonService);
        } else {
            flag = processUpload(files, fw, commonService);
        }
        if (flag) {
            List<String> specialChar = new ArrayList<>();
            List<String> fileList = new ArrayList<>();
            for (FileItemStream file : files) {
                for (String s : file.getName().split("/")) {
                    if (StringUtils.isNotBlank(s)) {
                        final String s1 = CommonUtils.takeOutChar(s);
                        if (StringUtils.isNotBlank(s1) && CommonUtils.REGX.contains(s1)) {
                            specialChar.add(s1);
                            fileList.add(s);
                        }
                    }
                }
            }
            StringBuilder filePath = new StringBuilder();
            StringBuilder specialCharCollection = new StringBuilder();
            if (fileList.size() > 0) {
                for (String s : fileList) {
                    filePath.append(s).append(",");
                }
                for (String s : specialChar) {
                    specialCharCollection.append(s).append(",");
                }
            }
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, CommonUtils.messageInternational("COMMAND_FILE") + filePath.substring(0, filePath.length() - 1) + CommonUtils.messageInternational("COMMAND_FILE_CHAR_SEN") + specialCharCollection.substring(0, specialCharCollection.length() - 1));
        } else {
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, buildJsonFilesArray(added));
        }
    }

    // not this one
    private boolean processChunkUpload(HttpServletRequest request, List<FileItemStream> files, FileWriter fw, ElfinderCommonService commonService) throws NumberFormatException, IOException {
        // cid : unique id of chunked uploading file
        String cid = request.getParameter("cid");
        // solr-5.5.2.tgz.48_65.part
        String chunk = request.getParameter("chunk");
        // 100270176,2088962,136813192
        String range = request.getParameter("range");
        String[] tokens = range.split(",");
        Matcher m = Pattern.compile("(.*)\\.([0-9]+)\\_([0-9]+)\\.part").matcher(chunk);
        boolean flag = false;
        if (m.find()) {
            String fileName = m.group(1);
            if (specialSymbolCheck(fileName) || commonService.sensitive(fileName)) {
                flag = true;
            } else {
                long index = Long.parseLong(m.group(2));
                long total = Long.parseLong(m.group(3));
                Parts parts = Parts.getOrCreate(request, cid, fileName, total + 1, Long.parseLong(tokens[2]));
                long start = Long.parseLong(tokens[0]);
                long size = Long.parseLong(tokens[1]);
                parts.addPart(index, new Part(start, size, files.get(0)));
                if (parts.isReady()) {
                    parts.checkParts();
                    fw.createAndSave(fileName, parts.openInputStream());
                    // remove from application context
                    parts.removeFromApplicationContext(request);
                }
            }
        }
        return flag;
    }

    private boolean processUpload(List<FileItemStream> files, FileWriter fw, ElfinderCommonService commonService) throws IOException {
        boolean flag = false;
        for (FileItemStream fis : files) {
            final String name = fis.getName();
            for (String subName : name.split("/")) {
                if (flag = (specialSymbolCheck(subName) || commonService.sensitive(subName))) {
                    break;
                }
            }
            if (flag) {
                break;
            } else {
                fw.createAndSave(name, fis.openStream());
            }
        }
        return flag;
    }
}
