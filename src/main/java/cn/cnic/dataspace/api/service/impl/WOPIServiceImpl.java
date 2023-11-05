package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.filehandle.ControlImpl;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.service.WOPIService;
import cn.cnic.dataspace.api.service.space.SettingService;
import cn.cnic.dataspace.api.util.*;
import com.baomidou.mybatisplus.extension.api.R;
import com.github.benmanes.caffeine.cache.Cache;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class WOPIServiceImpl implements WOPIService {

    private static final String VIEW_PARAM = "&ui=zh-cn&embed=true&dchat=1";

    public static final Map<String, String> ACTION_URLS = Collections.unmodifiableMap(new HashMap<String, String>() {

        {
            put("doc", "/hosting/wopi/word/view?wopisrc=%s" + VIEW_PARAM);
            put("docx", "/hosting/wopi/word/view?wopisrc=%s" + VIEW_PARAM);
            put("pdf", "/hosting/wopi/word/view?wopisrc=%s" + VIEW_PARAM);
            // put("txt", "/hosting/wopi/word/view?wopisrc=%s" + VIEW_PARAM);
            put("xls", "/hosting/wopi/cell/view?wopisrc=%s" + VIEW_PARAM);
            put("xlsx", "/hosting/wopi/cell/view?wopisrc=%s" + VIEW_PARAM);
            put("csv", "/hosting/wopi/cell/view?wopisrc=%s" + VIEW_PARAM);
            put("ppt", "/hosting/wopi/slide/view?wopisrc=%s" + VIEW_PARAM);
            put("pptx", "/hosting/wopi/slide/view?wopisrc=%s" + VIEW_PARAM);
        }
    });

    private static final Map<String, String> FILE_VERSION = new HashMap<>();

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    // File control layer
    private final Control control = new ControlImpl();

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Resource
    private ElfinderStorageService elfinderStorageService;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private SettingService settingService;

    @Resource
    private SpaceUrl spaceUrl;

    @Resource
    private SpaceRepository spaceRepository;

    @Override
    public R<Map<String, Object>> getOfficeInfo(HttpServletRequest request, HttpServletResponse response, String spaceId, String hash) {
        // OnlineOffice openOnlineOffice = settingService.getOpenOnlineOffice();
        // if (openOnlineOffice == null) {
        // Throw new CommonException (500, "Online office preview service not enabled");
        // }
        Token usersToken = getToken(request, response);
        ElfinderStorage elfinderStorage = null;
        String realPath = "";
        try {
            elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
            realPath = elfinderStorage.fromHash(hash).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StringUtils.isEmpty(realPath)) {
            throw new CommonException("输入的文件hash无效");
        }
        // Local operation
        realPath = realPath.replace("D:", "");
        realPath = realPath.replace("\\", "/");
        // Verify if you have file download permissions
        control.validateFileOtherPermissions(usersToken.getEmailAccounts(), spaceId, realPath, "down");
        String[] split = new File(realPath).getName().split("\\.");
        String suffix = "";
        if (split.length > 1) {
            suffix = split[split.length - 1];
        }
        String actionU = ACTION_URLS.get(suffix);
        if (actionU == null) {
            throw new CommonException(500, "该文件格式暂不支持预览");
        }
        // actionU = openOnlineOffice.getServiceUrl() + actionU;
        Query query = new Query().addCriteria(Criteria.where("path").is(realPath));
        FileMapping fileMapping = mongoTemplate.findOne(query, FileMapping.class, spaceId);
        Map<String, Object> ret = new HashMap<>();
        String callHost = spaceUrl.getCallHost() + "/api";
        String downloadFileUrl = String.format("%s/wopi/files/%s-%s/", callHost, spaceId, fileMapping.getId());
        ret.put("fileId", fileMapping.getId());
        ret.put("actionUrl", String.format(actionU, downloadFileUrl));
        return R.ok(ret);
    }

    @Override
    public Map<String, Object> getOfficeInfo(FileMapping target, String spaceId, String token) {
        // OnlineOffice openOnlineOffice = settingService.getOpenOnlineOffice();
        // if (openOnlineOffice == null) {
        // Throw new CommonException (-1, "The online office preview service is not installed or enabled");
        // }
        String realPath = target.getPath();
        realPath = realPath.replace("\\", "/");
        String[] split = new File(realPath).getName().split("\\.");
        String suffix = "";
        if (split.length > 1) {
            suffix = split[split.length - 1];
        }
        String actionU = ACTION_URLS.get(suffix);
        if (actionU == null) {
            throw new CommonException(-1, "该文件格式暂不支持预览");
        }
        // actionU = openOnlineOffice.getServiceUrl() + actionU;
        Map<String, Object> ret = new HashMap<>();
        String callHost = spaceUrl.getCallHost() + "/api";
        String downloadFileUrl = String.format("%s/wopi/files/%s-%s/", callHost, spaceId, target.getId());
        ret.put("actionUrl", String.format(actionU, downloadFileUrl));
        return ret;
    }

    @Override
    public Map<String, Object> getFileInfo(String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl) {
        if (!fileid.contains("-")) {
            return new HashMap<>();
        }
        String spaceId = fileid.split("-")[0];
        String fileId = fileid.split("-")[1];
        Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        boolean isPublic = (spaceOptional.isPresent()) && (StringUtils.equals(spaceOptional.get().getState(), "1")) && (spaceOptional.get().getIsPublic() == 1);
        Token usersToken = null;
        if (!isPublic) {
            usersToken = getToken(access_token);
        }
        Query query = new Query().addCriteria(Criteria.where("_id").is(fileId));
        FileMapping fileMapping = mongoTemplate.findOne(query, FileMapping.class, spaceId);
        if (fileMapping == null) {
            throw new CommonException(-1, "文件未找到");
        }
        // Verify if you have file download permissions
        if (!isPublic) {
            // Non public space permission verification returns user token at the same time
            control.validateFileOtherPermissions(usersToken.getEmailAccounts(), spaceId, fileMapping.getPath(), "down");
        }
        File file = new File(fileMapping.getPath());
        Map<String, Object> info = new HashMap<>();
        info.put("BaseFileName", fileMapping.getName());
        info.put("OwnerId", fileMapping.getAuthor().getPersonId());
        info.put("UserFriendlyName", isPublic ? "public" : usersToken.getName());
        info.put("Size", "" + file.length());
        info.put("UserId", isPublic ? "public" : usersToken.getUserId());
        String version = CommonUtils.generateUUID();
        FILE_VERSION.put(fileId, version);
        info.put("Version", version);
        // info.put("CopyPasteRestrictions", "BlockAll");
        info.put("DisablePrint", true);
        info.put("EditModePostMessage", false);
        info.put("EditNotificationPostMessage", false);
        info.put("ClosePostMessage", false);
        info.put("ReadOnly", true);
        info.put("SupportsLocks", false);
        info.put("SupportsUpdate", false);
        info.put("SupportsRename", false);
        info.put("UserCanWrite", false);
        info.put("UserCanNotWriteRelative", true);
        info.put("HidePrintOption", true);
        return info;
    }

    @Override
    public void getContent(String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl) {
        if (!fileid.contains("-")) {
            return;
        }
        String spaceId = fileid.split("-")[0];
        String fileId = fileid.split("-")[1];
        Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        boolean isPublic = (spaceOptional.isPresent()) && (StringUtils.equals(spaceOptional.get().getState(), "1")) && (spaceOptional.get().getIsPublic() == 1);
        Token usersToken = null;
        if (!isPublic) {
            // Non public space permission verification returns user token at the same time
            usersToken = getToken(access_token);
        }
        Query query = new Query().addCriteria(Criteria.where("_id").is(fileId));
        FileMapping fileMapping = mongoTemplate.findOne(query, FileMapping.class, spaceId);
        if (fileMapping == null) {
            throw new CommonException(-1, "文件未找到");
        }
        if (!isPublic) {
            // Verify if you have file download permissions
            control.validateFileOtherPermissions(usersToken.getEmailAccounts(), spaceId, fileMapping.getPath(), "down");
        }
        // localPath = localPath + fileName;
        File localFile = new File(fileMapping.getPath());
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.addHeader("X-WOPI-ItemVersion", FILE_VERSION.get(fileId));
        response.addHeader("Content-Disposition", "attachment;filename=" + new String(localFile.getName().getBytes(StandardCharsets.UTF_8)));
        response.setContentType("application/octet-stream");
        try {
            IOUtils.copyBytes(new FileInputStream(localFile), response.getOutputStream(), 2048, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Token getToken(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtTokenUtils.getToken(request);
        // String token = CommonUtils.getUser(request, Constants.TOKEN);
        if (null == token) {
            throw new CommonException(401, "请登录");
        }
        if (!jwtTokenUtils.validateToken(token)) {
            Cookie cookie = CommonUtils.getCookie(Constants.TOKEN, "", 0, 1);
            Cookie cookie2 = CommonUtils.getCookie(Constants.WAY, "", 0, 1);
            response.addCookie(cookie);
            response.addCookie(cookie2);
            throw new CommonException(401, "请登录");
        } else {
            String ifPresent = tokenCache.getIfPresent(token);
            if (ifPresent == null) {
                throw new CommonException(401, "请登录");
            }
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        return usersToken;
    }

    private Token getToken(String token) {
        if (null == token) {
            throw new CommonException(401, "请登录");
        }
        if (!jwtTokenUtils.validateToken(token)) {
            throw new CommonException(401, "请登录");
        } else {
            String ifPresent = tokenCache.getIfPresent(token);
            if (ifPresent == null) {
                throw new CommonException(401, "请登录");
            }
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        return usersToken;
    }
}
