package cn.cnic.dataspace.api.elfinder.command;

import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import com.alibaba.fastjson.JSONObject;
import javax.servlet.http.HttpServletRequest;

public class AbortCommand extends AbstractJsonCommand implements ElfinderCommand {

    @Override
    protected void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, JSONObject json, ElfinderCommonService commonService) throws Exception {
        json.put("error", 0);
    }
}
