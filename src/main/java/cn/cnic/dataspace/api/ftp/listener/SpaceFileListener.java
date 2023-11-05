package cn.cnic.dataspace.api.ftp.listener;

import cn.cnic.dataspace.api.ftp.minimalftp.api.SpaceListener;
import cn.cnic.dataspace.api.util.CaffeineUtil;
import cn.cnic.dataspace.api.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SpaceFileListener implements SpaceListener {

    @Override
    public Map<String, String> saveSpaceId(String username, String spaceId) {
        Map<String, String> result = new HashMap<>();
        result.put("code", "500");
        result.put("path", "error");
        if (spaceId.equals("/")) {
            return result;
        }
        String[] split = spaceId.split("/");
        for (String s : split) {
            if (!s.equals("")) {
                Map<String, String> shortChain = CaffeineUtil.getShortChain(username);
                if (shortChain.containsKey(s)) {
                    result.put("code", "200");
                    result.put("path", "/" + s);
                    String space = shortChain.get(s);
                    // result.put("spaceId",spacePath.substring(spacePath.lastIndexOf("/")+1));
                    result.put("spaceId", space);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public String auth(String username, String path) {
        return "";
        // return space;
    }

    @Override
    public boolean renameFile(String username, String sourceFileName, String targetFileName) {
        return CommonUtils.isFtpChar(targetFileName);
    }

    @Override
    public boolean mkdirFile(String username, String fileName) {
        return CommonUtils.isFtpChar(fileName);
    }

    @Override
    public void deleteFile(String username, String fileName, String type) {
    }

    @Override
    public boolean uploadFile(String username, String fileName) {
        return CommonUtils.isFtpChar(fileName);
    }

    @Override
    public void downloadFile(String username, String fileName) {
    }
}
