package cn.cnic.dataspace.api.controller.office;

import cn.cnic.dataspace.api.datax.admin.aop.HasSpacePermission;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.filehandle.ControlImpl;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.service.WOPIService;
import cn.cnic.dataspace.api.service.space.SettingService;
import cn.cnic.dataspace.api.util.*;
import com.baomidou.mybatisplus.extension.api.R;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping
@Slf4j
public class WOPIController {

    @Resource
    private WOPIService wopiService;

    // @PostMapping("/getOfficeInfo")
    // @ResponseBody
    // @HasSpacePermission
    // public R<Map<String, Object>> getOfficeInfo(HttpServletRequest request,
    // HttpServletResponse response,
    // String spaceId,
    // String hash) {
    // return wopiService.getOfficeInfo(request, response, spaceId, hash);
    // 
    // }
    @ResponseBody
    @GetMapping("/wopi/files/{fileid}")
    public Map<String, Object> getFileInfo(@PathVariable("fileid") String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl) {
        return wopiService.getFileInfo(fileid, request, response, access_token, access_token_ttl);
    }

    @GetMapping("/wopi/files/{fileid}/contents")
    public void getContent(@PathVariable("fileid") String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl) throws IOException {
        wopiService.getContent(fileid, request, response, access_token, access_token_ttl);
    }
}
