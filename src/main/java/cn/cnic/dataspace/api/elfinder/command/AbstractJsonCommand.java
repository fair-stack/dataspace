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
import cn.cnic.dataspace.api.exception.CommonException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public abstract class AbstractJsonCommand extends AbstractCommand {

    private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";

    protected abstract void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, JSONObject json, ElfinderCommonService elfinderCommonService) throws Exception;

    @Override
    final public void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, HttpServletResponse response, ElfinderCommonService elfinderCommonService, String linkId) throws Exception {
        JSONObject json = new JSONObject();
        PrintWriter writer = response.getWriter();
        try {
            execute(elfinderStorage, request, json, elfinderCommonService);
            response.setContentType(APPLICATION_JSON_CHARSET_UTF_8);
            writer.write(JSONObject.toJSONString(json, SerializerFeature.DisableCircularReferenceDetect));
            writer.flush();
        } catch (CommonException commonException) {
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, commonException.getMessage());
            writer.write(JSONObject.toJSONString(json, SerializerFeature.DisableCircularReferenceDetect));
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_ERROR, e.getMessage());
            writer.write(JSONObject.toJSONString(json, SerializerFeature.DisableCircularReferenceDetect));
            writer.flush();
        } finally {
            writer.close();
        }
    }
}
