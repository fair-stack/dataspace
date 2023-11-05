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
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;

public class PasteCommand extends AbstractJsonCommand implements ElfinderCommand {

    private static final String INT_CUT = "1";

    @Override
    protected void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, JSONObject json, ElfinderCommonService commonService) throws Exception {
        final String[] targets = request.getParameterValues(ElFinderConstants.ELFINDER_PARAMETER_TARGETS);
        final String destination = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_FILE_DESTINATION);
        final boolean cut = INT_CUT.equals(request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_CUT));
        List<VolumeHandler> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> removedRe = new ArrayList<>();
        VolumeHandler vhDst = findTarget(elfinderStorage, destination);
        try {
            for (String target : targets) {
                VolumeHandler vhTarget = findTarget(elfinderStorage, target);
                final String name = vhTarget.getName();
                VolumeHandler newFile = new VolumeHandler(vhDst, name);
                added.add(newFile);
                removed.add(elfinderStorage.fromHash(vhTarget.getHash()).toString());
                if (cut) {
                    // move
                    move(vhTarget, newFile);
                } else {
                    // File copying
                    createAndCopy(vhTarget, newFile, elfinderStorage.fromHash(destination).toString());
                }
            }
            // vhDst.paste(fileName, removed, elfinderStorage.fromHash(destination).toString(), isFolder, targets.length,cut);
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ADDED, buildJsonFilesArray(added));
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_REMOVED, removedRe.toArray());
        } catch (FileAlreadyExistsException e) {
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, CommonUtils.messageInternational("FILE_EXIST"));
        }
    }
}
