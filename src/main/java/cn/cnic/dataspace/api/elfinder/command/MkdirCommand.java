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
import cn.cnic.dataspace.api.elfinder.util.HashesUtil;
import cn.cnic.dataspace.api.util.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MkdirCommand extends AbstractJsonCommand implements ElfinderCommand {

    @Override
    protected void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, JSONObject json, ElfinderCommonService elfinderCommonService) throws Exception {
        String target = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_TARGET);
        String dirName = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_NAME);
        if (dirName == null) {
            String[] paramMap = request.getParameterMap().get(ElFinderConstants.ELFINDER_PARAMETER_DIRS);
            List<Map<String, Object>> list = new ArrayList<>();
            Map<String, Object> hashes = new HashMap<>(16);
            for (String dir : paramMap) {
                VolumeHandler directory = new VolumeHandler(findTarget(elfinderStorage, target), dir);
                final String directoryName = directory.getName();
                if (specialSymbolCheck(directoryName)) {
                    String msg = CommonUtils.messageInternational("COMMAND_FOLDER_CHAR") + CommonUtils.takeOutChar(directoryName);
                    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, msg.replace("directoryName", directoryName));
                } else if (elfinderCommonService.sensitive(directoryName)) {
                    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, CommonUtils.messageInternational("COMMAND_FOLDER_FAIL"));
                } else {
                    if (directory.exists()) {
                        json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, CommonUtils.messageInternational("COMMAND_FOLDER_EXIST"));
                    } else {
                        directory.createFolder(true, StringUtils.equals(paramMap[0], dir));
                        json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, new Object[] { getTargetInfo(directory) });
                        list.add(getTargetInfo(directory));
                        hashes.put(dir, target + HashesUtil.encode(dir));
                        json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, list);
                        json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_HASHES, hashes);
                    }
                }
            }
        } else {
            if (specialSymbolCheck(dirName)) {
                String msg = CommonUtils.messageInternational("COMMAND_FOLDER_CHAR") + CommonUtils.takeOutChar(dirName);
                json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, msg.replace("directoryName", dirName));
            } else if (elfinderCommonService.sensitive(dirName)) {
                json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, CommonUtils.messageInternational("COMMAND_FOLDER_FAIL"));
            } else {
                try {
                    VolumeHandler volumeHandler = findTarget(elfinderStorage, target);
                    VolumeHandler directory = new VolumeHandler(volumeHandler, dirName);
                    if (directory.exists()) {
                        json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, CommonUtils.messageInternational("COMMAND_FOLDER_EXIST"));
                    } else {
                        directory.createFolder(false, true);
                        json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, new Object[] { getTargetInfo(directory) });
                    }
                } catch (Exception e) {
                    json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, CommonUtils.messageInternational("INTERNAL_ERROR"));
                }
            }
        }
    }
}
