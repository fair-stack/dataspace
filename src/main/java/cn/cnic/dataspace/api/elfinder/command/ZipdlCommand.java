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
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.VolumeHandler;
import cn.cnic.dataspace.api.elfinder.support.archiver.Archiver;
import cn.cnic.dataspace.api.elfinder.support.archiver.ArchiverType;
import cn.cnic.dataspace.api.elfinder.util.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

public class ZipdlCommand extends AbstractCommand implements ElfinderCommand {

    private HttpSession session;

    @Override
    public void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, HttpServletResponse response, ElfinderCommonService commonService, String linkId) throws Exception {
        String spaceId = request.getParameter("spaceId");
        if (StringUtils.isNotBlank(spaceId)) {
            session = request.getSession();
            session.setAttribute("spaceId", spaceId);
        } else {
            spaceId = linkId;
            // spaceId = session.getAttribute("spaceId").toString();
        }
        final String[] targets = request.getParameterValues(ElFinderConstants.ELFINDER_PARAMETER_TARGETS);
        boolean download = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_DOWNLOAD) != null;
        if (download) {
            String archiveFileTarget = targets[0];
            String downloadFileName = targets[2];
            String mime = targets[3];
            VolumeHandler archiveTarget = findTarget(elfinderStorage, archiveFileTarget);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(mime);
            response.setHeader("Content-Disposition", "attachments; " + HttpUtil.getAttachementFileName(downloadFileName, request.getHeader("USER-AGENT")));
            response.setHeader("Content-Transfer-Encoding", "binary");
            String path = archiveTarget.getVolume().getPath(archiveTarget.getTarget());
            String targetZip = commonService.spacePath(spaceId) + "/" + path + ".zip";
            File file = new File(targetZip);
            try (InputStream fis = new BufferedInputStream(new FileInputStream(file.getPath()));
                OutputStream toClient = new BufferedOutputStream(response.getOutputStream())) {
                byte[] buffer = new byte[fis.available()];
                fis.read(buffer);
                response.reset();
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "UTF-8"));
                toClient.write(buffer);
                toClient.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            JSONObject json = new JSONObject();
            List<Target> targetList = findTargets(elfinderStorage, targets);
            Archiver archiver = ArchiverType.of("application/zip").getStrategy();
            PrintWriter writer = response.getWriter();
            try {
                // judge whether the last operation was successful
                if (!Objects.isNull(request.getSession().getAttribute("dl"))) {
                    new File(request.getSession().getAttribute("dl").toString()).delete();
                    request.getSession().removeAttribute("dl");
                }
                Target targetArchive = archiver.compress(targetList.toArray(new Target[] {}));
                json.put(ElFinderConstants.ELFINDER_PARAMETER_ZIPDL, getTargetInfo(new VolumeHandler(targetArchive, elfinderStorage)));
                response.setContentType("application/json; charset=UTF-8");
                writer.write(json.toJSONString());
                writer.flush();
                // remove temp generate folder.zip set session
                request.getSession().setAttribute("dl", commonService.spacePath(spaceId) + "/" + targetArchive.getVolume().getPath(targetArchive));
            } catch (Exception e) {
                e.printStackTrace();
                json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, e.getMessage());
                writer.write(json.toJSONString());
                writer.flush();
            } finally {
                writer.close();
            }
        }
    }
}
