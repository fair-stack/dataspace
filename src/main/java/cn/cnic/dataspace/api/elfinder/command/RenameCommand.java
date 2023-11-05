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
import javax.servlet.http.HttpServletRequest;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

public class RenameCommand extends AbstractJsonCommand implements ElfinderCommand {

    @Override
    protected void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, JSONObject json, ElfinderCommonService commonService) throws Exception {
        final String target = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_TARGET);
        final String newName = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_NAME);
        if (specialSymbolCheck(newName)) {
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, messageInternational("COMMAND_FILE_CHAR") + CommonUtils.takeOutChar(newName));
        } else if (commonService.sensitive(newName)) {
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, messageInternational("COMMAND_FILE_FAIL"));
        } else {
            VolumeHandler volumeHandler = findTarget(elfinderStorage, target);
            VolumeHandler destination = new VolumeHandler(volumeHandler.getParent(), newName);
            volumeHandler.renameTo(destination);
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, new Object[] { getTargetInfo(destination) });
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_REMOVED, new String[] { target });
        }
    }
}
