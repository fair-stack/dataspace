package cn.cnic.dataspace.api.elfinder.command;

import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import com.alibaba.fastjson.JSONObject;
import javax.servlet.http.HttpServletRequest;

public class EditorCommand extends AbstractJsonCommand implements ElfinderCommand {

    @Override
    protected void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, JSONObject json, ElfinderCommonService elfinderCommonService) throws Exception {
        json.put("OnlineConvert", false);
        json.put("ZipArchive", false);
        json.put("ZohoOffice", false);
    }
}
