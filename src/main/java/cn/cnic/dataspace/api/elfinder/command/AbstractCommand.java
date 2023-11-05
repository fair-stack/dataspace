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

import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.elfinder.config.ElFinderConstants;
import cn.cnic.dataspace.api.elfinder.core.ElfinderContext;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.VolumeHandler;
import cn.cnic.dataspace.api.elfinder.support.archiver.ArchiverOption;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Slf4j
public abstract class AbstractCommand implements ElfinderCommand {

    private static final String CMD_TMB_TARGET = "?cmd=tmb&target=%s&userCode=%s";

    private Map<String, Object> options = new HashMap<>();

    protected void addChildren(Map<String, VolumeHandler> map, VolumeHandler target) throws IOException {
        for (VolumeHandler f : target.listChildren()) {
            map.put(f.getHash(), f);
        }
    }

    protected void addChildren(Map<String, VolumeHandler> map, VolumeHandler target, String fileName) throws IOException {
        for (VolumeHandler f : target.listChildren()) {
            if (StringUtils.equals(f.getName(), fileName)) {
                map.put(f.getHash(), f);
            }
        }
    }

    protected void addSubFolders(Map<String, VolumeHandler> map, VolumeHandler target) throws IOException {
        for (VolumeHandler f : target.listChildren()) {
            if (f.isFolder()) {
                map.put(f.getHash(), f);
            }
        }
    }

    protected void createAndCopy(VolumeHandler src, VolumeHandler dst, String dent) throws IOException {
        dst.webCreateAndCopy(src, dst, dent);
        // if (src.isFolder()) {
        // createAndCopyFolder(src, dst);
        // } else {
        // createAndCopyFile(src, dst);
        // }
    }

    protected void move(VolumeHandler src, VolumeHandler dst) throws IOException {
        dst.move(src.getTarget(), dst.getTarget());
    }

    // private void createAndCopyFile(VolumeHandler src, VolumeHandler dst) throws IOException {
    // dst.createFile();
    // InputStream is = src.openInputStream();
    // OutputStream os = dst.openOutputStream();
    // IOUtils.copy(is, os);
    // is.close();
    // os.close();
    // dst.createFileRecord();
    // }
    // 
    // private void createAndCopyFolder(VolumeHandler src, VolumeHandler dst) throws IOException {
    // dst.createFolder(false, true);
    // for (VolumeHandler c : src.listChildren()) {
    // if (c.isFolder()) {
    // createAndCopyFolder(c, new VolumeHandler(dst, c.getName()));
    // } else {
    // createAndCopyFile(c, new VolumeHandler(dst, c.getName()));
    // }
    // }
    // }
    @Override
    public void execute(ElfinderContext context, ElfinderCommonService elfinderCommonService, String linkId) {
        ElfinderStorage elfinderStorage = context.getVolumeSourceFactory().getVolumeSource();
        HttpServletRequest request = context.getRequest();
        request.setAttribute(SpaceSvnLog.METHOD, SpaceSvnLog.ELFINDER);
        request.getSession().setAttribute("authorization", FileOperationFactory.getJwtTokenUtils().getEmail(request.getHeader("Authorization")));
        try {
            execute(elfinderStorage, request, context.getResponse(), elfinderCommonService, linkId);
        } catch (ClientAbortException e) {
            log.info("请求已终止");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, HttpServletResponse response, ElfinderCommonService elfinderCommonService, String linkId) throws Exception;

    protected Object[] buildJsonFilesArray(Collection<VolumeHandler> list) {
        List<Map<String, Object>> jsonFileList = new ArrayList<>();
        for (VolumeHandler itemHandler : list) {
            try {
                jsonFileList.add(getTargetInfo(itemHandler));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsonFileList.toArray();
    }

    protected VolumeHandler findCwd(ElfinderStorage elfinderStorage, String target) {
        VolumeHandler cwd = null;
        if (target != null) {
            cwd = findTarget(elfinderStorage, target);
        }
        if (cwd == null) {
            cwd = new VolumeHandler(elfinderStorage.getVolumes().get(0).getRoot(), elfinderStorage);
        }
        return cwd;
    }

    protected VolumeHandler findTarget(ElfinderStorage elfinderStorage, String hash) {
        Target target = elfinderStorage.fromHash(hash);
        if (target == null) {
            return null;
        }
        return new VolumeHandler(target, elfinderStorage);
    }

    protected List<Target> findTargets(ElfinderStorage elfinderStorage, String[] targetHashes) {
        if (elfinderStorage != null && targetHashes != null) {
            List<Target> targets = new ArrayList<>(targetHashes.length);
            for (String targetHash : targetHashes) {
                Target target = elfinderStorage.fromHash(targetHash);
                if (target != null) {
                    targets.add(target);
                }
            }
            return targets;
        }
        return Collections.emptyList();
    }

    protected Map<String, Object> getTargetInfo(final VolumeHandler target) throws IOException {
        Map<String, Object> info = new HashMap<>(16);
        info.put(ElFinderConstants.ELFINDER_PARAMETER_HASH, target.getHash());
        info.put(ElFinderConstants.ELFINDER_PARAMETER_MIME, target.getMimeType());
        info.put(ElFinderConstants.ELFINDER_PARAMETER_TIMESTAMP, target.getLastModified());
        info.put(ElFinderConstants.ELFINDER_PARAMETER_SIZE, target.getSize());
        info.put(ElFinderConstants.ELFINDER_PARAMETER_READ, target.isReadable() ? ElFinderConstants.ELFINDER_TRUE_RESPONSE : ElFinderConstants.ELFINDER_FALSE_RESPONSE);
        info.put(ElFinderConstants.ELFINDER_PARAMETER_WRITE, target.isWritable() ? ElFinderConstants.ELFINDER_TRUE_RESPONSE : ElFinderConstants.ELFINDER_FALSE_RESPONSE);
        info.put(ElFinderConstants.ELFINDER_PARAMETER_LOCKED, target.isLocked() ? ElFinderConstants.ELFINDER_TRUE_RESPONSE : ElFinderConstants.ELFINDER_FALSE_RESPONSE);
        info.put(ElFinderConstants.ELFINDER_PARAMETER_LOCATION, "/" + target.getVolume().getPath(target.getTarget()));
        if (target.isRoot()) {
            info.put(ElFinderConstants.ELFINDER_PARAMETER_DIRECTORY_FILE_NAME, target.getVolumeAlias());
            info.put(ElFinderConstants.ELFINDER_PARAMETER_VOLUME_ID, target.getVolumeId());
        } else {
            info.put(ElFinderConstants.ELFINDER_PARAMETER_DIRECTORY_FILE_NAME, target.getName());
            info.put(ElFinderConstants.ELFINDER_PARAMETER_PARENTHASH, target.getParent().getHash());
        }
        if (target.isFolder()) {
            info.put(ElFinderConstants.ELFINDER_PARAMETER_HAS_DIR, target.hasChildFolder() ? ElFinderConstants.ELFINDER_TRUE_RESPONSE : ElFinderConstants.ELFINDER_FALSE_RESPONSE);
        }
        return info;
    }

    protected Map<String, Object> getOptions(VolumeHandler cwd) {
        String[] emptyArray = {};
        options.put(ElFinderConstants.ELFINDER_PARAMETER_PATH, cwd.getName());
        options.put(ElFinderConstants.ELFINDER_PARAMETER_COMMAND_DISABLED, emptyArray);
        options.put(ElFinderConstants.ELFINDER_PARAMETER_FILE_SEPARATOR, ElFinderConstants.ELFINDER_PARAMETER_FILE_SEPARATOR);
        options.put(ElFinderConstants.ELFINDER_PARAMETER_OVERWRITE_FILE, ElFinderConstants.ELFINDER_TRUE_RESPONSE);
        options.put(ElFinderConstants.ELFINDER_PARAMETER_ARCHIVERS, ArchiverOption.JSON_INSTANCE());
        return options;
    }

    /**
     * special symbol check
     * true: include
     * false: not include
     */
    boolean specialSymbolCheck(String str) {
        return StringUtils.isNotBlank(str) && CommonUtils.isFtpChar(str);
    }
}
