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

import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.VolumeHandler;
import cn.cnic.dataspace.api.elfinder.util.HttpUtil;
import cn.cnic.dataspace.api.util.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FileCommand extends AbstractCommand implements ElfinderCommand {

    public static final String STREAM = "1";

    @Override
    public void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, HttpServletResponse response, ElfinderCommonService elfinderCommonService, String linkId) throws Exception {
        String target = request.getParameter("target");
        boolean download = STREAM.equals(request.getParameter("download"));
        VolumeHandler fsi = super.findTarget(elfinderStorage, target);
        response.setCharacterEncoding("utf-8");
        response.setContentType(fsi.getMimeType());
        String fileName = fsi.getName();
        if (download) {
            if (elfinderCommonService.downloadRateLimiter(request)) {
                response.setHeader("Content-Disposition", "attachments; " + HttpUtil.getAttachementFileName(fileName, request.getHeader("USER-AGENT")));
                response.setHeader("Content-Transfer-Encoding", "binary");
                String spaceId = request.getParameter("spaceId");
                spaceId = Objects.isNull(spaceId) ? linkId : spaceId;
                // public space statistic document download
                // if (elfinderCommonService.isPublic(spaceId)) {
                elfinderCommonService.download(spaceId, "download", 1L);
                elfinderCommonService.download(spaceId, "downSize", fsi.getSize());
                elfinderCommonService.stateDownload(spaceId, fsi.getSize());
                // }
            }
            response.setContentLength((int) fsi.getSize());
            try (InputStream is = fsi.openInputStream();
                OutputStream out = response.getOutputStream()) {
                IOUtils.copy(is, out);
                out.flush();
            } finally {
                if (download) {
                    // remove temp generate folder.zip
                    Object dl = request.getSession().getAttribute("dl");
                    if (!Objects.isNull(dl)) {
                        File file = new File(dl.toString());
                        if (file.exists()) {
                            file.delete();
                            request.getSession().removeAttribute("dl");
                            if (file.exists()) {
                                Files.delete(file.toPath());
                            }
                        }
                    }
                }
            }
        } else {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            Map<String, Object> result = new HashMap<>(16);
            result.put("code", 202);
            result.put("data", CommonUtils.messageInternational("CONFIG_LIMIT"));
            response.getWriter().write(JSONObject.toJSONString(result));
            response.reset();
        }
    }
}
