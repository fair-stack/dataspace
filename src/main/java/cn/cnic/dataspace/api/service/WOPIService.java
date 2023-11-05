package cn.cnic.dataspace.api.service;

import cn.cnic.dataspace.api.model.file.FileMapping;
import com.baomidou.mybatisplus.extension.api.R;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface WOPIService {

    R<Map<String, Object>> getOfficeInfo(HttpServletRequest request, HttpServletResponse response, String spaceId, String hash);

    Map<String, Object> getOfficeInfo(FileMapping target, String spaceId, String token);

    Map<String, Object> getFileInfo(String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl);

    void getContent(String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl);
}
