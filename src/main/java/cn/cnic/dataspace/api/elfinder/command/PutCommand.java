package cn.cnic.dataspace.api.elfinder.command;

import cn.cnic.dataspace.api.elfinder.config.ElFinderConstants;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.VolumeHandler;
import cn.cnic.dataspace.api.elfinder.util.ImageUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import javax.servlet.http.HttpServletRequest;
import java.io.OutputStream;

/**
 * PutCommand
 *
 * @author wangCc
 * @date 2021-06-16 15:19
 */
public class PutCommand extends AbstractJsonCommand implements ElfinderCommand {

    public static final String ENCODING = "utf-8";

    public static final String IMAGE_BASE64_FLAG = "data:image/jpeg;base64,";

    @Override
    protected void execute(ElfinderStorage elfinderStorage, HttpServletRequest request, JSONObject json, ElfinderCommonService elfinderCommonService) throws Exception {
        final String target = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_TARGET);
        VolumeHandler file = findTarget(elfinderStorage, target);
        OutputStream os = file.openOutputStream();
        String content = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_CONTENT);
        if (content.contains(IMAGE_BASE64_FLAG)) {
            byte[] bytes = ImageUtil.decodeImageStr(content.substring(IMAGE_BASE64_FLAG.length()));
            IOUtils.write(bytes, os);
        } else {
            file.write(elfinderStorage.fromHash(target).toString(), content, os, ENCODING);
        }
        os.close();
        json.put(ElFinderConstants.ELFINDER_JSON_RESPONSE_CHANGED, new Object[] { getTargetInfo(file) });
    }
}
