package cn.cnic.dataspace.api.service.space;

import cn.cnic.dataspace.api.config.space.FileMappingManage;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.harvest.FTPShort;
import cn.cnic.dataspace.api.model.harvest.ShareLink;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.harvest.FtpUser;
import cn.cnic.dataspace.api.model.release.ResourceShow;
import cn.cnic.dataspace.api.service.file.FileHandService;
import cn.cnic.dataspace.api.service.impl.ReleaseServiceImpl;
import cn.cnic.dataspace.api.util.*;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.*;
import static cn.cnic.dataspace.api.service.space.SpaceService.SPACE_SENIOR;

@Service
@Slf4j
public class ShareService {

    /**
     * Generate Link Password Method
     */
    // No password
    public static final String NO = "no";

    // Random password
    public static final String RAND = "rand";

    // Custom Password
    public static final String CUSTOM = "custom";

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ElfinderStorageService elfinderStorageService;

    @Lazy
    @Autowired
    private SpaceUrl spaceUrl;

    @Lazy
    @Autowired
    private SpaceControlConfig spaceControlConfig;

    @Lazy
    @Autowired
    private FileHandService fileHandService;

    @Lazy
    @Autowired
    private FileMappingManage fileMappingManage;

    @Autowired
    private SpaceService spaceService;

    private final BASE64Encoder encoder = new BASE64Encoder();

    private final BASE64Decoder decoder = new BASE64Decoder();

    /**
     * internal space for public return
     */
    @Data
    private static class InternalSpace {

        private String spaceId;

        private String spaceName;

        private String spaceLogo;

        private String markdown;

        private List<String> tag;

        private List<Map<String, Object>> authorization;

        public InternalSpace(Space space) {
            this.spaceId = space.getSpaceId();
            this.spaceName = space.getSpaceName();
            this.spaceLogo = space.getSpaceLogo();
            this.markdown = space.getDescription();
            this.tag = space.getTags();
            List<Map<String, Object>> list = new ArrayList<>();
            for (AuthorizationPerson authorizationPerson : space.getAuthorizationList()) {
                Map<String, Object> map = new HashMap<>();
                map.put("personName", authorizationPerson.getUserName());
                map.put("avatar", authorizationPerson.getAvatar());
                map.put("email", authorizationPerson.getEmail());
                list.add(map);
            }
            this.authorization = list;
        }
    }

    /**
     * internal space log for public
     */
    @Data
    private static class InternalSpaceLog {

        private String dateTime;

        private String content;

        public InternalSpaceLog(SpaceSvnLog spaceSvnLog) {
            if (null != spaceSvnLog.getCreateTime()) {
                this.dateTime = CommonUtils.getDateString(spaceSvnLog.getCreateTime());
            } else {
                this.dateTime = spaceSvnLog.getUpdateDateTime();
            }
            this.content = spaceSvnLog.getDescription();
        }
    }

    public ResponseResult<Object> createLink(String token, String spaceId, String fileHash, String time, String way, String type, String password, HttpServletRequest request) {
        if (StringUtils.isEmpty(spaceId) || StringUtils.isEmpty(time) || StringUtils.isEmpty(way)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        Token user = jwtTokenUtils.getToken(token);
        spaceControlConfig.spatialVerification(spaceId, user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        if (!way.equals(NO) && !way.equals(RAND) && !way.equals(CUSTOM)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId).and("state").is("1")), Space.class);
        if (null == space) {
            return ResultUtil.errorInternational("SPACE_REVIEW");
        }
        if (!way.equals(NO)) {
            if (StringUtils.isEmpty(password) || StringUtils.isEmpty(password.trim())) {
                return ResultUtil.errorInternational("SHARE_LINK_PASS");
            }
            password = password.trim();
        }
        String content = "";
        Map<String, Object> map = new HashMap<>(3);
        if (type.equals("space")) {
            spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), spaceId, SpaceRoleEnum.F_SHAR_SPACE.getRole());
            content = "创建空间分享：创建了空间分享链接";
            map.put("id", space.getSpaceId());
            map.put("logo", space.getSpaceLogo());
            map.put("name", space.getSpaceName());
            map.put("updateTime", space.getCreateDateTime());
        } else {
            ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
            Target target = elfinderStorage.fromHash(fileHash);
            String path = target.toString();
            FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(path)), FileMapping.class, spaceId);
            if (null == fileMapping) {
                return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
            }
            String role = "";
            if (fileMapping.getAuthor().getEmail().equals(user.getEmailAccounts())) {
                role = SpaceRoleEnum.F_SHAR_FILE_AM.getRole();
            } else {
                role = SpaceRoleEnum.F_SHAR_FILE_OT.getRole();
            }
            spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), spaceId, role);
            File file = new File(path);
            if (!file.exists()) {
                return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
            }
            content = "创建文件分享：创建了 " + file.getName() + (file.isFile() ? "（文件）" : "（文件夹）") + "的分享链接";
            map.put("id", space.getSpaceId());
            map.put("hash", fileHash);
            map.put("name", file.getName());
            map.put("size", (file.isFile() ? fileMapping.getSize() : 0));
            map.put("isFile", file.isFile());
        }
        Date date = new Date();
        String url = type.equals("space") ? spaceUrl.getShareSpaceUrl() : spaceUrl.getShareFileUrl();
        String link = CommonUtils.generateUUID();
        ShareLink shareLink = new ShareLink();
        shareLink.setLink(link);
        String encodedText = encoder.encode(link.getBytes(StandardCharsets.UTF_8));
        shareLink.setUrl(url + encodedText);
        shareLink.setCode(encodedText);
        shareLink.setContent(map);
        shareLink.setFounder(new AuthorizationPerson(user));
        shareLink.setType(type);
        shareLink.setPasWay(way);
        shareLink.setTime(time);
        shareLink.setPassword(password);
        shareLink.setSpaceId(space.getSpaceId());
        shareLink.setCreateTime(date);
        Date failureTime = CommonUtils.getHour(date, Integer.valueOf(time));
        shareLink.setFailureTime(failureTime);
        mongoTemplate.insert(shareLink);
        // log
        spaceControlConfig.spaceLogSave(spaceId, content, user.getUserId(), new Operator(user), SpaceSvnLog.ACTION_OTHER);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("link", url + encodedText);
        resultMap.put("way", way);
        resultMap.put("failureTime", failureTime);
        resultMap.put("password", password);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> list(String token, Integer page, Integer size, String content, String type) {
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("founder.userId").is(userId));
        if (!type.equals("all") && !type.equals("0") && !type.equals("1")) {
            return ResultUtil.success();
        }
        if (!type.equals("all")) {
            query.addCriteria(Criteria.where("share").is(Integer.parseInt(type)));
        }
        if (StringUtils.isNotEmpty(content)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(content.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("content.name").regex(pattern));
        }
        long count = mongoTemplate.count(query, ShareLink.class);
        List<ShareLink> shareLinks = null;
        if (count > 0) {
            query.with(PageRequest.of(page - 1, size));
            query.with(Sort.by(Sort.Order.desc("createTime")));
            shareLinks = mongoTemplate.find(query, ShareLink.class);
            for (ShareLink shareLink : shareLinks) {
                shareLink.setFtpUserId(null);
                // shareLink.setPassword(null);
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("count", count);
        resultMap.put("data", shareLinks);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> set(String token, String shareId) {
        if (StringUtils.isEmpty(shareId)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("_id").is(shareId));
        ShareLink one = mongoTemplate.findOne(query, ShareLink.class);
        if (null == one) {
            return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
        }
        if (!one.getFounder().getUserId().equals(userId)) {
            return ResultUtil.errorInternational("PERMISSION_FORBIDDEN");
        }
        one.setShare(1);
        mongoTemplate.save(one);
        // Delete ftp
        if (StringUtils.isNotEmpty(one.getFtpUserId())) {
            mongoTemplate.remove(new Query().addCriteria(Criteria.where("_id").is(one.getFtpUserId())), FtpUser.class);
            mongoTemplate.remove(new Query().addCriteria(Criteria.where("userId").is(one.getFtpUserId())), FTPShort.class);
            // Delete FTP user cache
            CaffeineUtil.clearFtpUserId();
            CaffeineUtil.clearFtpShor();
        }
        return ResultUtil.success();
    }

    public boolean roleJudge(Space space, String userId) {
        boolean judge = false;
        for (AuthorizationPerson authorizationPerson : space.getAuthorizationList()) {
            if (authorizationPerson.getRole().equals(SPACE_SENIOR)) {
                judge = true;
                break;
            }
        }
        return !judge && !space.getUserId().equals(userId);
    }

    public ShareLink getShareLink(String linkId) {
        if (StringUtils.isEmpty(linkId) || StringUtils.isEmpty(linkId.trim())) {
            throw new CommonException(CommonUtils.messageInternational("GENERAL_PARAMETER_ERROR"));
        }
        ShareLink shareLink = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("code").is(linkId)), ShareLink.class);
        if (shareLink == null) {
            String link = null;
            try {
                link = new String(decoder.decodeBuffer(linkId), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                throw new CommonException(CommonUtils.messageInternational("SYSTEM_ERROR"));
            }
            if (null == link) {
                throw new CommonException(CommonUtils.messageInternational("GENERAL_NOT_EXIST"));
            }
            Query query = new Query().addCriteria(Criteria.where("link").is(link));
            shareLink = mongoTemplate.findOne(query, ShareLink.class);
        }
        if (shareLink == null) {
            throw new CommonException(CommonUtils.messageInternational("GENERAL_NOT_EXIST"));
        }
        if (shareLink.getShare() == 1) {
            throw new CommonException(CommonUtils.messageInternational("SHARE_LINK_TIME"));
        }
        long failure = shareLink.getFailureTime().getTime();
        long time = new Date().getTime();
        if (time > failure) {
            throw new CommonException(CommonUtils.messageInternational("SHARE_LINK_TIME"));
        }
        return shareLink;
    }

    public void passwordIf(String pasWay, String password, String cofPas) {
        if (!pasWay.equals(NO)) {
            if (StringUtils.isEmpty(password) || StringUtils.isEmpty(password.trim())) {
                throw new CommonException(CommonUtils.messageInternational("SHARE_LINK_PASS"));
            }
            if (!cofPas.equals(password)) {
                throw new CommonException(CommonUtils.messageInternational("SHARE_LINK_ERROR_PWD"));
            }
        }
    }

    public ResponseResult<Object> isPwd(String linkId) {
        ShareLink shareLink = getShareLink(linkId);
        if (!shareLink.getType().equals("file") && !shareLink.getType().equals("space")) {
            return ResultUtil.success();
        }
        HashMap<String, Object> resultMap = new HashMap<>();
        String pasWay = shareLink.getPasWay();
        if (pasWay.equals(NO)) {
            resultMap.put("isPass", false);
        } else {
            resultMap.put("isPass", true);
        }
        resultMap.put("founder", shareLink.getFounder());
        resultMap.put("failureTime", shareLink.getFailureTime());
        resultMap.put("type", shareLink.getType());
        resultMap.put("spaceId", shareLink.getSpaceId());
        if (shareLink.getType().equals("file")) {
            resultMap.put("fileName", ((Map) shareLink.getContent()).get("name"));
            resultMap.put("isFile", ((Map) shareLink.getContent()).get("isFile"));
        } else {
            resultMap.put("updateTime", ((Map) shareLink.getContent()).get("updateTime"));
            resultMap.put("spaceName", ((Map) shareLink.getContent()).get("name"));
        }
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> fileList(String linkId, String password, HttpServletRequest request) {
        ShareLink shareLink = getShareLink(linkId);
        if (!shareLink.getType().equals("file")) {
            return ResultUtil.success();
        }
        countTotal("viewNum", shareLink.getId(), request.getSession());
        String pasWay = shareLink.getPasWay();
        passwordIf(pasWay, password, shareLink.getPassword());
        Object content = shareLink.getContent();
        return ResultUtil.success(content);
    }

    // public ResponseResult<Object> spaceInfo(String linkId) {
    // ShareLink shareLink = getShareLink(linkId);
    // if(!shareLink.getType().equals("space")){
    // return ResultUtil.success();
    // }
    // HashMap<String, Object> resultMap = new HashMap<>();
    // String pasWay = shareLink.getPasWay();
    // if(pasWay.equals(NO)){
    // resultMap.put("isPass",false);
    // }else {
    // resultMap.put("isPass",true);
    // }
    // resultMap.put("founder",shareLink.getFounder());
    // resultMap.put("failureTime",shareLink.getFailureTime());
    // resultMap.put("updateTime",((Map)shareLink.getContent()).get("updateTime"));
    // resultMap.put("spaceName",((Map)shareLink.getContent()).get("name"));
    // return ResultUtil.success(resultMap);
    // }
    public ResponseResult<Object> detail(String linkId, String password, HttpServletRequest request) {
        ShareLink shareLink = getShareLink(linkId);
        if (!shareLink.getType().equals("space")) {
            return ResultUtil.success();
        }
        String pasWay = shareLink.getPasWay();
        passwordIf(pasWay, password, shareLink.getPassword());
        String id = ((Map) shareLink.getContent()).get("id").toString();
        Space one = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id).and("state").is("1")), Space.class);
        if (null == one) {
            return ResultUtil.errorInternational("SPACE_OFFLINE_FORBIDDEN");
        }
        countTotal("viewNum", shareLink.getId(), request.getSession());
        spaceControlConfig.spaceStat(one.getSpaceId(), "viewCount", 1L);
        // Obtain spatial information‘
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("linkId", linkId);
        resultMap.put("failureTime", shareLink.getFailureTime());
        resultMap.put("spaceInfo", new InternalSpace(one));
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> recent(String linkId, String password) {
        ShareLink shareLink = getShareLink(linkId);
        passwordIf(shareLink.getPasWay(), password, shareLink.getPassword());
        if (!shareLink.getType().equals("space")) {
            return ResultUtil.success();
        }
        String id = ((Map) shareLink.getContent()).get("id").toString();
        // Space one = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id).and("state").is("1")), Space.class);
        // if(null == one){
        // return ResultUtil.errorInternational("SPACE_OFFLINE_FORBIDDEN");
        // }
        List<InternalSpaceLog> list = new ArrayList<>();
        for (SpaceSvnLog spaceSvnLog : mongoTemplate.find(new Query().addCriteria(Criteria.where("spaceId").is(id)).addCriteria(new Criteria().orOperator(Criteria.where("action").in(ACTION_FILE, ACTION_MEMBER, ACTION_PUBLISH))).with(Sort.by("createTime").descending()).with(PageRequest.of(0, 10)), SpaceSvnLog.class)) {
            list.add(new InternalSpaceLog(spaceSvnLog));
        }
        return ResultUtil.success(list);
    }

    public ResponseResult<Object> publish(String linkId, String password) {
        ShareLink shareLink = getShareLink(linkId);
        if (!shareLink.getType().equals("space")) {
            return ResultUtil.success();
        }
        passwordIf(shareLink.getPasWay(), password, shareLink.getPassword());
        String id = ((Map) shareLink.getContent()).get("id").toString();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<ResourceShow> resources = mongoTemplate.find(new Query().addCriteria(Criteria.where("spaceId").is(id).and("type").is(Constants.PUBLISHED)).with(Sort.by(Sort.Order.desc("publicTime"))).with(PageRequest.of(0, 10)), ResourceShow.class, ReleaseServiceImpl.COLLECTION_NAME);
        for (ResourceShow resource : resources) {
            Map<String, Object> map = new HashMap<>(16);
            map.put("image", resource.getImage());
            map.put("title", resource.getTitleCH());
            map.put("url", resource.getDetailsUrl());
            map.put("org", resource.getOrgName());
            map.put("dateTime", resource.getPublicTime());
            nodes.add(map);
        }
        return ResultUtil.success(nodes);
    }

    /**
     * get cmd from public space
     */
    @SneakyThrows
    public void cmd(String linkId, String password, String cmd, HttpServletRequest request, final HttpServletResponse response) {
        ShareLink shareLink = getShareLink(linkId);
        passwordIf(shareLink.getPasWay(), password, shareLink.getPassword());
        if (cmd.equals("file")) {
            String target = request.getParameter("target");
            fileHandService.entranceDown(target, shareLink.getSpaceId(), "down", response);
            downTotal(shareLink.getId());
            return;
        }
    }

    public ResponseResult<Object> open(String linkId, String password, Integer page, Integer size, String direction, String sort, HttpServletRequest request) {
        ShareLink shareLink = getShareLink(linkId);
        passwordIf(shareLink.getPasWay(), password, shareLink.getPassword());
        String target = request.getParameter("target");
        String spaceId = shareLink.getSpaceId();
        return fileHandService.cmd("ds.publicFile", page, size, direction, sort, target, spaceId);
    }

    private void countTotal(String key, String shareId, HttpSession session) {
        boolean judge = true;
        synchronized (this) {
            Object share = session.getAttribute("share" + shareId);
            if (share == null) {
                session.setAttribute("share" + shareId, shareId);
                // 20 minutes
                session.setMaxInactiveInterval(20 * 60);
            } else {
                judge = false;
            }
        }
        if (judge) {
            Update update = new Update();
            update.inc(key, 1);
            Query query = new Query().addCriteria(Criteria.where("_id").is(shareId));
            mongoTemplate.upsert(query, update, ShareLink.class);
        }
    }

    private void downTotal(String shareId) {
        synchronized (this) {
            Update update = new Update();
            update.inc("dowNum", 1);
            Query query = new Query().addCriteria(Criteria.where("_id").is(shareId));
            mongoTemplate.upsert(query, update, ShareLink.class);
        }
    }

    public List<String> conversion(List<String> hashList, String spaceShort, String spaceId) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        ElfinderStorage elfinderStorage = null;
        try {
            elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
        } catch (Exception e) {
            throw new CommonException("error!");
        }
        Set<String> set = new HashSet<>();
        long total = 0L;
        for (String hash : hashList) {
            Target target = elfinderStorage.fromHash(hash);
            if (null != target) {
                String path = target.toString();
                total += fileMappingManage.getSizeBytes(path, spaceId);
                String s = path.replaceAll(spaceShort, "~");
                set.add(s.substring(s.indexOf("~") + 1));
            }
        }
        spaceControlConfig.dataOut("fairLink", total, spaceId);
        return new ArrayList<>(set);
    }

    /**
     * Obtain component interfaces
     */
    public ResponseResult<Map<String, Object>> getComponent(String linkId, String password, String hash) {
        ShareLink shareLink = getShareLink(linkId);
        passwordIf(shareLink.getPasWay(), password, shareLink.getPassword());
        return spaceService.getComponent("public", shareLink.getSpaceId(), hash);
    }

    /**
     * Component Preview
     */
    public ResponseResult<Object> previewData(String linkId, String password, String hash, String componentId, HttpServletRequest request) {
        ShareLink shareLink = getShareLink(linkId);
        passwordIf(shareLink.getPasWay(), password, shareLink.getPassword());
        return spaceService.previewData("public", shareLink.getSpaceId(), hash, componentId, request);
    }

    public ResponseResult<Object> getFileData(String linkId, String password, String hash, HttpServletRequest request) {
        ShareLink shareLink = getShareLink(linkId);
        passwordIf(shareLink.getPasWay(), password, shareLink.getPassword());
        return spaceService.getFileData("public", shareLink.getSpaceId(), hash, request);
    }
}
