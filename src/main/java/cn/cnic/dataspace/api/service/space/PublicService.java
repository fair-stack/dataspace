package cn.cnic.dataspace.api.service.space;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.release.ResourceShow;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.service.file.FileHandService;
import cn.cnic.dataspace.api.service.impl.ReleaseServiceImpl;
import cn.cnic.dataspace.api.util.*;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.*;

/**
 * PublicService
 *
 * @author wangCc
 * @date 2021-11-17 20:32
 */
@Service
public class PublicService {

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    @Autowired
    private FileHandService fileHandService;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private SpaceService spaceService;

    /**
     * internal space for public return
     */
    @Data
    private static class InternalSpace {

        private String spaceName;

        private String spaceLogo;

        private String markdown;

        private List<String> tag;

        private List<Map<String, Object>> authorization;

        public InternalSpace(Space space) {
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

    /**
     * public space check
     */
    private boolean spaceUpAudit(Optional<Space> spaceOptional) {
        return (spaceOptional.isPresent()) && (StringUtils.equals(spaceOptional.get().getState(), "1")) && (spaceOptional.get().getIsPublic() == 1);
    }

    /**
     * public space detail
     */
    public ResponseResult<Object> detail(String url) {
        ResponseResult<Object> objectResponseResult = paramJudge(url);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        final Optional<Space> spaceOptional = spaceRepository.findByHomeUrl(url);
        if (!spaceUpAudit(spaceOptional)) {
            return ResultUtil.publicError("SPACE_OFFLINE");
        }
        if (spaceOptional.isPresent()) {
            spaceControlConfig.spaceStat(spaceOptional.get().getSpaceId(), "viewCount", 1L);
        }
        return spaceOptional.<ResponseResult<Object>>map(space -> StringUtils.equals(space.getState(), "1") ? ResultUtil.success(new InternalSpace(space)) : ResultUtil.publicError("SPACE_OFFLINE")).orElseGet(() -> ResultUtil.errorInternational("URL_NOT_EXIST"));
    }

    /**
     * recently dynamic for public space
     */
    public ResponseResult<Object> recent(String url) {
        ResponseResult<Object> objectResponseResult = paramJudge(url);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        final Optional<Space> spaceOptional = spaceRepository.findByHomeUrl(url);
        if (!spaceUpAudit(spaceOptional)) {
            return ResultUtil.publicError("SPACE_OFFLINE");
        }
        List<Space> spaceList = mongoTemplate.find(new Query().addCriteria(Criteria.where("homeUrl").is(url)), Space.class);
        List<InternalSpaceLog> list = new ArrayList<>();
        for (SpaceSvnLog spaceSvnLog : mongoTemplate.find(new Query().addCriteria(Criteria.where("spaceId").is(spaceList.get(0).getSpaceId())).addCriteria(new Criteria().orOperator(Criteria.where("action").in(ACTION_FILE, ACTION_MEMBER, ACTION_PUBLISH))).with(Sort.by("createTime").descending()).with(PageRequest.of(0, 10)), SpaceSvnLog.class)) {
            list.add(new InternalSpaceLog(spaceSvnLog));
        }
        return ResultUtil.success(list);
    }

    /**
     * recently publish
     */
    public ResponseResult<Object> publish(String url) {
        ResponseResult<Object> objectResponseResult = paramJudge(url);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        final Optional<Space> spaceOptional = spaceRepository.findByHomeUrl(url);
        if (!spaceUpAudit(spaceOptional)) {
            return ResultUtil.publicError("SPACE_OFFLINE");
        }
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<ResourceShow> resources = mongoTemplate.find(new Query().addCriteria(Criteria.where("spaceId").is(spaceOptional.get().getSpaceId()).and("type").is(Constants.PUBLISHED)).with(Sort.by(Sort.Order.desc("publicTime"))).with(PageRequest.of(0, 10)), ResourceShow.class, ReleaseServiceImpl.COLLECTION_NAME);
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
    public void cmd(String url, String cmd, HttpServletRequest request, final HttpServletResponse response) {
        ResponseResult<Object> objectResponseResult = paramJudge(url);
        if (objectResponseResult.getCode() != 0) {
            throw new CommonException(500, CommonUtils.messageInternational("GENERAL_PARAMETER_ERROR"));
        }
        Optional<Space> spaceOptional = spaceRepository.findByHomeUrl(url);
        if (!spaceUpAudit(spaceOptional)) {
            throw new CommonException(500, CommonUtils.messageInternational("SPACE_OFFLINE"));
        }
        String target = request.getParameter("target");
        String spaceId = spaceOptional.get().getSpaceId();
        if (cmd.equals("file")) {
            fileHandService.entranceDown(target, spaceId, "down", response);
            return;
        }
    }

    public ResponseResult<Object> open(String url, Integer page, Integer size, String direction, String sort, HttpServletRequest request) {
        ResponseResult<Object> objectResponseResult = paramJudge(url);
        if (objectResponseResult.getCode() != 0) {
            throw new CommonException(500, CommonUtils.messageInternational("GENERAL_PARAMETER_ERROR"));
        }
        Optional<Space> spaceOptional = spaceRepository.findByHomeUrl(url);
        if (!spaceUpAudit(spaceOptional)) {
            throw new CommonException(500, CommonUtils.messageInternational("SPACE_OFFLINE"));
        }
        String target = request.getParameter("target");
        String spaceId = spaceOptional.get().getSpaceId();
        return fileHandService.cmd("ds.publicFile", page, size, direction, sort, target, spaceId);
    }

    public ResponseResult<Object> judge(String url, HttpServletRequest request) {
        ResponseResult<Object> objectResponseResult = paramJudge(url);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        String token = jwtTokenUtils.getToken(request);
        Criteria criteria = new Criteria();
        criteria.orOperator(Criteria.where("_id").is(url), Criteria.where("homeUrl").is(url));
        Query query = new Query().addCriteria(criteria);
        Space one = mongoTemplate.findOne(query, Space.class);
        if (null == one) {
            return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
        }
        String type = "";
        if (one.getIsPublic() == 1) {
            type = "public";
        } else {
            if (one.getApplyIs() == 1) {
                type = "limited";
            } else {
                type = "private";
            }
        }
        Map<String, Object> resultMap = new HashMap<>(3);
        Map<String, Object> spaceMap = new HashMap<>(6);
        if (StringUtils.isNotEmpty(token)) {
            if (StringUtils.isNotEmpty(token) && jwtTokenUtils.validateToken(token)) {
                String userId = jwtTokenUtils.getUserIdFromToken(token);
                List<String> spaceIds = null;
                if (StringUtils.isNotEmpty(userId)) {
                    CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
                    spaceIds = cacheLoading.getUserApplySpaces(userId);
                }
                if (judgeMember(one.getAuthorizationList(), userId)) {
                    resultMap.put("role", 3);
                } else {
                    resultMap.put("role", spaceIds.contains(one.getSpaceId()) ? 2 : 1);
                }
                spaceMap.put("spaceLogo", one.getSpaceLogo());
                Set<AuthorizationPerson> authorizationList = one.getAuthorizationList();
                if (authorizationList != null && authorizationList.size() > 0) {
                    ArrayList<AuthorizationPerson> authorizationPeople = new ArrayList<>(authorizationList);
                    spaceMap.put("authorizationList", authorizationPeople.subList(0, 1));
                } else {
                    spaceMap.put("authorizationList", null);
                }
                spaceMap.put("tags", one.getTags());
                spaceMap.put("memberCount", one.getAuthorizationList().size());
                spaceMap.put("viewCount", one.getViewCount());
            }
        } else {
            resultMap.put("role", 1);
        }
        spaceMap.put("spaceName", one.getSpaceName());
        resultMap.put("type", type);
        resultMap.put("space", spaceMap);
        return ResultUtil.success(resultMap);
    }

    /**
     * Obtain component interfaces
     */
    public ResponseResult<Map<String, Object>> getComponent(String url, String hash) {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(url.trim())) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        final Optional<Space> spaceOptional = spaceRepository.findByHomeUrl(url);
        if (!spaceUpAudit(spaceOptional)) {
            return ResultUtil.publicError("SPACE_OFFLINE");
        }
        return spaceService.getComponent("public", spaceOptional.get().getSpaceId(), hash);
    }

    /**
     * Component Preview
     */
    public ResponseResult<Object> previewData(String url, String hash, String componentId, HttpServletRequest request) {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(url.trim())) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        final Optional<Space> spaceOptional = spaceRepository.findByHomeUrl(url);
        if (!spaceUpAudit(spaceOptional)) {
            return ResultUtil.publicError("SPACE_OFFLINE");
        }
        return spaceService.previewData("public", spaceOptional.get().getSpaceId(), hash, componentId, request);
    }

    public ResponseResult<Object> getFileData(String url, String hash, HttpServletRequest request) {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(url.trim())) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        final Optional<Space> spaceOptional = spaceRepository.findByHomeUrl(url);
        if (!spaceUpAudit(spaceOptional)) {
            return ResultUtil.publicError("SPACE_OFFLINE");
        }
        return spaceService.getFileData("public", spaceOptional.get().getSpaceId(), hash, request);
    }

    private boolean judgeMember(Set<AuthorizationPerson> authorizationPersonList, String userId) {
        for (AuthorizationPerson authorizationPerson : authorizationPersonList) {
            if (authorizationPerson.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    private ResponseResult<Object> paramJudge(String url) {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(url.trim())) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        return ResultUtil.success();
    }
}
