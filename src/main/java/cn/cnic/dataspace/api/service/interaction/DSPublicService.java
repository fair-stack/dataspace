package cn.cnic.dataspace.api.service.interaction;

import cn.cnic.dataspace.api.asynchronous.AsyncDeal;
import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.space.FileMappingManage;
import cn.cnic.dataspace.api.config.space.MsgUtil;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.core.Volume;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.manage.ReleaseAccount;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.space.child.Person;
import cn.cnic.dataspace.api.model.email.EmailRole;
import cn.cnic.dataspace.api.model.release.*;
import cn.cnic.dataspace.api.model.release.instdb.SendInsertDB;
import cn.cnic.dataspace.api.model.release.stemcells.*;
import cn.cnic.dataspace.api.model.release.template.Template;
import cn.cnic.dataspace.api.model.release.template.XmlTemplateUtil;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.queue.SpaceQuery;
import cn.cnic.dataspace.api.repository.ResourceRepository;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.repository.SvnSpaceLogRepository;
import cn.cnic.dataspace.api.repository.UserRepository;
import cn.cnic.dataspace.api.service.ExternalInterService;
import cn.cnic.dataspace.api.service.impl.ReleaseServiceImpl;
import cn.cnic.dataspace.api.service.space.MessageService;
import cn.cnic.dataspace.api.util.*;
import cn.hutool.extra.cglib.CglibUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;
import static cn.cnic.dataspace.api.model.user.Message.TITLE_PUBLISH_NOT_PASSED;
import static cn.cnic.dataspace.api.model.user.Message.TITLE_PUBLISH_PASSED;
import static cn.cnic.dataspace.api.service.space.SpaceService.SPACE_SENIOR;
import static cn.cnic.dataspace.api.util.CommonUtils.*;

@Service
@EnableAsync
@Slf4j
public class DSPublicService {

    private final Cache<String, String> publicModel = CaffeineUtil.getPublicModel();

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    private final Cache<String, String> insertDBData = CaffeineUtil.getScienceData();

    private final Cache<String, Map<String, String>> publicOrgUrl = CaffeineUtil.getPublicOrgUrl();

    private final Cache<String, Boolean> publicFileStop = CaffeineUtil.getPublicFileStop();

    private final Cache<String, String> publicData = CaffeineUtil.getPublicData();

    @Autowired
    private ReleaseServiceImpl releaseService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private ExternalInterService externalInterService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ElfinderStorageService elfinderStorageService;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private MsgUtil msgUtil;

    @Autowired
    private SvnSpaceLogRepository svnSpaceLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Lazy
    @Autowired
    private AsyncDeal asyncDeal;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    @Autowired
    private MessageService messageService;

    @Lazy
    @Autowired
    private FileMappingManage fileMappingManage;

    public static final String COLLECTION_NAME = "resource_v2";

    private final String[] tags = { "author", "paper", "project", "org" };

    /**
     * Interactive Dataset Publishing - json ld
     */
    public ResponseResult<Object> dsSubmit(String token, ResourceRequest req, HttpServletRequest request) {
        if (StringUtils.isNotEmpty(req.getTemplateId())) {
            loadingModelCheck(req.getTemplateId(), req.getOrgId());
        }
        // analysis
        Token user = jwtTokenUtils.getToken(token);
        // Parameter verification
        ParameterValidation.DSValidation(req, publicModel, mongoTemplate);
        spaceControlConfig.spatialVerification(req.getSpaceId(), user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), req.getSpaceId(), SpaceRoleEnum.P_ADD.getRole());
        String resourceId = req.getResourceId();
        // version
        String version = req.getVersion();
        if (StringUtils.isNotEmpty(version)) {
            // Version verification
            releaseService.judgeVersion(req.getResourceId(), req.getVersion());
        }
        // Parsing front-end data into entities
        ResourceV2 resource = getResource(user, req);
        // Data duplicate submission verification
        String check = resource.getTitleCH() + resource.getTitleEN() + resource.getSpaceId() + resource.getFounderId() + resource.getVersion() + resource.getOrgId() + resource.getResourceType() + resource.getDataType();
        String res = publicData.getIfPresent(check);
        if (StringUtils.isNotEmpty(res)) {
            return ResultUtil.success();
        } else {
            publicData.put(check, user.getUserId());
        }
        List<RequestFile> fileList = req.getFileList();
        Map<String, Object> pubFile = null;
        if (null != fileList && !fileList.isEmpty()) {
            pubFile = getFileList(fileList, request, req.getSpaceId());
            resource.setViewFileList(fileList);
        }
        List<String> tableList = req.getTableList();
        if (null != tableList && !tableList.isEmpty()) {
            resource.setTableList(tableList);
        }
        // Verify personal information
        ConsumerDO consumerDO = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(user.getUserId())), ConsumerDO.class);
        String orgChineseName = consumerDO.getOrgChineseName();
        resource.setFounderOrg(orgChineseName);
        if (req.getType() == Constants.DRAFT) {
            if (StringUtils.isNotEmpty(resourceId)) {
                resource.setResourceId(resourceId);
            }
            ResourceV2 save = mongoTemplate.save(resource, COLLECTION_NAME);
            return ResultUtil.success(save.getId());
        } else {
            // if (StringUtils.isEmpty(orgChineseName)) {
            // return ResultUtil.error(1007, messageInternational("DS_INFO"));
            // }
            ResponseResult<Object> result = this.check(token, resource.getResourceId(), resource.getResourceType(), resource.getSpaceId(), resource.getTitleCH());
            if (result.getCode() != 0) {
                return result;
            }
            if ((boolean) result.getData()) {
                return ResultUtil.errorInternational("RELEASE_AUDIT_double_name");
            }
            String id = resource.getResourceId() == null ? resourceId : resource.getResourceId();
            // Generate file processing parameters
            Map fileParam = getFileParam(resource.getFounderId(), id, version);
            paramMapAdd(fileParam, pubFile, resource.getDataType(), resource.getSpaceId(), tableList);
            // Publish to insertDB
            return submitInsertDb(resource, resourceId, fileParam);
        }
    }

    /**
     * Obtain data content from the previous version
     */
    public ResponseResult<Object> levelVersionDetails(String token, String id) {
        if (StringUtils.isEmpty(id)) {
            return ResultUtil.errorInternational("DS_RESOURCE_ID");
        }
        String email = tokenCache.getIfPresent(token);
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        ResourceV2 one = mongoTemplate.findOne(query, ResourceV2.class, COLLECTION_NAME);
        if (one == null) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        spaceControlConfig.spatialVerification(one.getSpaceId(), email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, one.getSpaceId(), SpaceRoleEnum.P_ADD.getRole());
        if (one.getType() != Constants.PUBLISHED) {
            return ResultUtil.errorInternational("DS_RESOURCE_MODIFY");
        }
        if (!email.equals(one.getFounder())) {
            return ResultUtil.errorInternational("DS_RESOURCE_OPERATION");
        }
        List<ResourceDo> metadata = one.getMetadata();
        Map<String, Object> dataMap = new HashMap<>(16);
        if (metadata != null) {
            for (ResourceDo meta : metadata) {
                dataMap.put(meta.getIri() + meta.getLanguage(), meta.getValue());
            }
        }
        List<Map<String, Object>> modelList = new ArrayList<>(8);
        if (StringUtils.isNotEmpty(one.getTemplateId()) && StringUtils.isNotEmpty(one.getOrgId())) {
            List<Map> modelData = getModelData(one.getTemplateId(), one.getOrgId(), one.getId());
            for (Map<String, Object> datum : modelData) {
                List<Object> resourceList = (List) datum.get("resources");
                for (Object obj : resourceList) {
                    Map<String, Object> resMap = (Map) obj;
                    String key = resMap.get("iri").toString() + resMap.get("language");
                    if (dataMap.containsKey(key)) {
                        resMap.put("value", dataMap.get(key));
                    } else {
                        resMap.put("value", null);
                    }
                }
                Map<String, Object> modelMap = new HashMap<>(1);
                modelMap.put("name", datum.get("name").toString());
                modelMap.put("desc", datum.get("desc").toString());
                modelMap.put("group", resourceList);
                modelList.add(modelMap);
            }
            // Encapsulate basic data
            modelData.clear();
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("templateData", modelList);
        resultMap.put("id", null);
        resultMap.put("resourceId", one.getResourceId());
        resultMap.put("version", null);
        resultMap.put("orgId", one.getOrgId());
        resultMap.put("orgName", one.getOrgName());
        resultMap.put("spaceId", one.getSpaceId());
        resultMap.put("sendFileList", one.getViewFileList());
        resultMap.put("templateId", one.getTemplateId());
        resultMap.put("template", one.getTemplate());
        resultMap.put("orgUrl", one.getOrgUrl());
        resultMap.put("orgCode", one.getOrgCode());
        resultMap.put("plan", one.getPlan());
        resultMap.put("resourceType", one.getResourceType());
        resultMap.put("dataType", one.getDataType());
        resultMap.put("tableList", one.getTableList());
        dataMap.clear();
        return ResultUtil.success(resultMap);
    }

    /**
     * Edit Query
     */
    public ResponseResult<Object> draftsDetails(String token, String resourceId) {
        if (StringUtils.isEmpty(resourceId)) {
            return ResultUtil.errorInternational("DS_RESOURCE_ID");
        }
        String email = tokenCache.getIfPresent(token);
        Query query = new Query().addCriteria(Criteria.where("_id").is(resourceId));
        ResourceV2 one = mongoTemplate.findOne(query, ResourceV2.class, COLLECTION_NAME);
        if (one == null) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        spaceControlConfig.spatialVerification(one.getSpaceId(), email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, one.getSpaceId(), SpaceRoleEnum.P_ADD.getRole());
        if (one.getType() == Constants.PUBLISHED) {
            return ResultUtil.errorInternational("DS_RESOURCE_MODIFY");
        }
        if (!email.equals(one.getFounder())) {
            return ResultUtil.errorInternational("DS_RESOURCE_OPERATION");
        }
        List<ResourceDo> metadata = one.getMetadata();
        Map<String, Object> dataMap = new HashMap<>(16);
        if (metadata != null) {
            for (ResourceDo meta : metadata) {
                dataMap.put(meta.getIri() + meta.getLanguage(), meta.getValue());
            }
        }
        List<Map<String, Object>> modelList = new ArrayList<>(8);
        if (StringUtils.isNotEmpty(one.getTemplateId()) && StringUtils.isNotEmpty(one.getOrgId())) {
            List<Map> modelData = getModelData(one.getTemplateId(), one.getOrgId(), one.getId());
            for (Map<String, Object> datum : modelData) {
                List<Object> resourceList = (List) datum.get("resources");
                for (Object obj : resourceList) {
                    Map<String, Object> resMap = (Map) obj;
                    String key = resMap.get("iri").toString() + resMap.get("language");
                    if (dataMap.containsKey(key)) {
                        resMap.put("value", dataMap.get(key));
                    } else {
                        resMap.put("value", null);
                    }
                }
                Map<String, Object> modelMap = new HashMap<>(1);
                modelMap.put("name", datum.get("name").toString());
                modelMap.put("desc", datum.get("desc").toString());
                modelMap.put("group", resourceList);
                modelList.add(modelMap);
            }
            // Encapsulate basic data
            modelData.clear();
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("templateData", modelList);
        resultMap.put("id", one.getId());
        resultMap.put("resourceId", one.getResourceId());
        resultMap.put("version", one.getVersion());
        resultMap.put("orgId", one.getOrgId());
        resultMap.put("orgName", one.getOrgName());
        resultMap.put("spaceId", one.getSpaceId());
        resultMap.put("sendFileList", one.getViewFileList());
        resultMap.put("templateId", one.getTemplateId());
        resultMap.put("template", one.getTemplate());
        resultMap.put("orgUrl", one.getOrgUrl());
        resultMap.put("orgCode", one.getOrgCode());
        resultMap.put("plan", one.getPlan());
        resultMap.put("resourceType", one.getResourceType());
        resultMap.put("dataType", one.getDataType());
        resultMap.put("tableList", one.getTableList());
        dataMap.clear();
        return ResultUtil.success(resultMap);
    }

    /**
     * Resource modification
     */
    public ResponseResult<Object> updateDrafts(String token, ResourceRequest resourceRequest, HttpServletRequest request) {
        // Parameter verification
        if (StringUtils.isNotEmpty(resourceRequest.getTemplateId())) {
            loadingModelCheck(resourceRequest.getTemplateId(), resourceRequest.getOrgId());
        }
        Token user = jwtTokenUtils.getToken(token);
        ParameterValidation.DSValidation(resourceRequest, publicModel, mongoTemplate);
        if (StringUtils.isEmpty(resourceRequest.getResourceId())) {
            return ResultUtil.errorInternational("DS_RESOURCE_ID");
        }
        spaceControlConfig.spatialVerification(resourceRequest.getSpaceId(), user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), resourceRequest.getSpaceId(), SpaceRoleEnum.P_ADD.getRole());
        // Query database data
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(resourceRequest.getResourceId()));
        ResourceV2 resource = mongoTemplate.findOne(query, ResourceV2.class, COLLECTION_NAME);
        if (resource == null) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        if (resource.getType() == Constants.AUDIT || resource.getType() == Constants.PUBLISHED) {
            return ResultUtil.errorInternational("DS_RESOURCE_MODIFY");
        }
        String imageOld = resource.getImage();
        String version = resourceRequest.getVersion();
        if (StringUtils.isNotEmpty(version)) {
            // Version verification
            releaseService.judgeVersion(resource.getResourceId(), version);
        } else {
            ResponseResult<Object> result = releaseService.getVersion(resource.getResourceId());
            resourceRequest.setVersion((String) result.getData());
        }
        // Let's talk about it when we make changes to the file
        List<RequestFile> fileList = resourceRequest.getFileList();
        Map<String, Object> pubFile = null;
        if (null != fileList && !fileList.isEmpty()) {
            pubFile = getFileList(fileList, request, resourceRequest.getSpaceId());
            resource.setViewFileList(fileList);
        } else {
            resource.setViewFileList(null);
        }
        List<String> tableList = resourceRequest.getTableList();
        if (null != tableList && !tableList.isEmpty()) {
            resource.setTableList(tableList);
        } else {
            resource.setTableList(null);
        }
        // Complete and modify information
        resource.setUpdateTime(new Date());
        editResource(resource, resourceRequest);
        String imageNew = resource.getImage();
        if (StringUtils.isNotEmpty(imageOld)) {
            try {
                if (!imageOld.equals(imageNew)) {
                    Files.delete(new File(spaceUrl.getRootDir(), imageOld).toPath());
                }
            } catch (IOException ioException) {
            }
        }
        // Data duplicate submission verification
        String check = resource.getTitleCH() + resource.getTitleEN() + resource.getSpaceId() + resource.getFounderId() + resource.getVersion() + resource.getOrgId() + resource.getResourceType() + resource.getDataType();
        String res = publicData.getIfPresent(check);
        if (StringUtils.isNotEmpty(res)) {
            return ResultUtil.success();
        } else {
            publicData.put(check, user.getUserId());
        }
        if (StringUtils.isEmpty(resource.getFounderOrg())) {
            ConsumerDO consumerDO = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(user.getUserId())), ConsumerDO.class);
            String orgChineseName = consumerDO.getOrgChineseName();
            resource.setFounderOrg(orgChineseName);
        }
        if (resourceRequest.getType() == 1) {
            // Draft box, release judgment
            ResponseResult<Object> result = this.check(token, resource.getResourceId(), resource.getResourceType(), resource.getSpaceId(), resource.getTitleCH());
            if (result.getCode() != 0) {
                return result;
            }
            if ((boolean) result.getData()) {
                return ResultUtil.errorInternational("RELEASE_AUDIT_double_name");
            }
            // Generate file processing parameters
            Map fileParam = getFileParam(resource.getFounderId(), resource.getResourceId(), version);
            paramMapAdd(fileParam, pubFile, resource.getDataType(), resource.getSpaceId(), tableList);
            return submitInsertDb(resource, resource.getResourceId(), fileParam);
        } else {
            resource.setType(Constants.DRAFT);
            resourceRepository.save(resource);
            return ResultUtil.success();
        }
    }

    private void paramMapAdd(Map<String, Object> fileParam, Map<String, Object> pubFile, int dataType, String spaceId, List<String> tableList) {
        fileParam.put("rootPath", spaceUrl.getReleaseStored());
        fileParam.put("pathList", (pubFile == null ? null : pubFile.get("pathList")));
        fileParam.put("manPath", (pubFile == null ? null : pubFile.get("manPath")));
        if (dataType == 1 || dataType == 2) {
            Space space = spaceRepository.findById(spaceId).get();
            fileParam.put("spaceDbName", space.getDbName());
            List<String> csvList = new ArrayList<>(tableList.size());
            tableList.forEach(var -> {
                String releasePath = fileParam.get("csvRoot").toString();
                csvList.add(releasePath + File.separator + var + ".xlsx");
            });
            fileParam.put("csvList", csvList);
        }
    }

    /**
     * Revoke Interface
     */
    public ResponseResult<Object> releaseRepeal(String token, String resourceId, String version) {
        String email = jwtTokenUtils.getEmail(token);
        ResourceV2 byResourceId = findByResourceId(resourceId, version, Constants.AUDIT);
        if (byResourceId == null) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        spaceControlConfig.spatialVerification(byResourceId.getSpaceId(), email, Constants.SpaceRole.LEVEL_OTHER);
        // Request Revocation Interface
        HttpClient httpClient = new HttpClient();
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("resourceId", byResourceId.getTraceId()));
        String code = getToken(httpClient, spaceUrl.getGetTokenUrl(), byResourceId.getOrgId(), Constants.GAIN);
        String result = requestRepeal(httpClient, byResourceId.getOrgId(), params, code);
        Map resultData = null;
        try {
            resultData = JSONObject.parseObject(result, Map.class);
        } catch (Exception e) {
            // Retrieve token
            code = getToken(httpClient, spaceUrl.getGetTokenUrl(), byResourceId.getOrgId(), Constants.REFRESH);
            result = requestRepeal(httpClient, byResourceId.getOrgId(), params, code);
            resultData = JSONObject.parseObject(result, Map.class);
        }
        if (!resultData.containsKey("result")) {
            return ResultUtil.errorInternational("DS_RESOURCE_REVOKE");
        }
        if (!(boolean) resultData.get("result")) {
            return ResultUtil.errorInternational("DS_RESOURCE_REVOKE");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(byResourceId.getId()));
        Update update = new Update();
        // Fallback to Draft
        update.set("type", Constants.DRAFT);
        publicFileStop.put(byResourceId.getResourceId() + byResourceId.getVersion(), true);
        mongoTemplate.upsert(query, update, COLLECTION_NAME);
        return ResultUtil.success();
    }

    /**
     * Requesting InstDB to revoke the interface
     */
    private String requestRepeal(HttpClient httpClient, String orgId, List<NameValuePair> params, String code) {
        String ifPresent = insertDBData.getIfPresent(Constants.DATASET_CANCEL + orgId);
        Map urlMap = JSONObject.parseObject(ifPresent, Map.class);
        Map<String, String> header = new HashMap<>(8);
        header.put("token", code);
        header.put("version", urlMap.get("version").toString());
        try {
            return httpClient.doGetWayTwo(params, urlMap.get("url").toString(), header);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CommonException(messageInternational("DS_RESOURCE_INSERT"));
        }
    }

    /**
     * Submit instDB
     */
    private ResponseResult submitInsertDb(ResourceV2 resource, String resourceId, Map fileMap) {
        List<String> pathList = (List) fileMap.get("pathList");
        String manPath = (String) fileMap.get("manPath");
        List<String> newPath = new ArrayList<>(pathList.size());
        for (String pp : pathList) {
            newPath.add(pp.replace(manPath, ""));
        }
        // InsertDb data conversion
        SendInsertDB conversion = conversion(resource, newPath);
        if (StringUtils.isNotEmpty(resource.getTraceId())) {
            // Reject adding last ID//Version update judgment
            conversion.setRootId(resource.getTraceId());
            resource.setResourceId(resourceId);
        } else if (StringUtils.isNotEmpty(resourceId)) {
            String version = resource.getVersion();
            if (!version.equals(Constants.VERSION)) {
                Integer v = Integer.parseInt(version.substring(1, 2));
                ResourceV2 byResourceId = findByResourceId(resourceId, "V" + (v - 1), Constants.PUBLISHED);
                conversion.setRootId(byResourceId.getTraceId());
            }
            resource.setResourceId(resourceId);
        }
        // New version of structured data encapsulation
        Map<String, Object> typeMap = new HashMap<>(2);
        if (resource.getDataType() == 0) {
            typeMap.put("type", "0");
        } else if (resource.getDataType() == 1) {
            typeMap.put("type", "1");
            typeMap.put("fileList", resource.getTableList());
        } else if (resource.getDataType() == 2) {
            typeMap.put("type", "2");
            typeMap.put("fileList", resource.getTableList());
        }
        conversion.setDataType(typeMap);
        conversion.setFileIsZip("no");
        String param = JSON.toJSONString(conversion);
        HttpClient httpClient = new HttpClient();
        // Get token
        String token = getToken(httpClient, spaceUrl.getGetTokenUrl(), resource.getOrgId(), Constants.GAIN);
        String result = requestInstDBPublic(httpClient, token, param, resource.getOrgId());
        Map resultData = null;
        try {
            // Parse return value
            resultData = JSONObject.parseObject(result, Map.class);
        } catch (Exception e) {
            token = getToken(httpClient, spaceUrl.getGetTokenUrl(), resource.getOrgId(), Constants.REFRESH);
            result = requestInstDBPublic(httpClient, token, param, resource.getOrgId());
            // Parse return value
            resultData = JSONObject.parseObject(result, Map.class);
        }
        if (!resultData.containsKey("resourceId")) {
            log.info("互操作接口接口调用错误 {} " + result);
            return ResultUtil.errorInternational("DS_RESOURCE_PUBLISH");
        }
        resource.setTraceId(resultData.get("resourceId").toString());
        resource.setPublishTarget(Constants.INSERT);
        resource.setFileSuccessOf(0);
        resource.setFileSend(false);
        resource.setAuditSend(false);
        resource.setAuditError(null);
        resource.setErrorMessage(null);
        String success = success(resource);
        fileMap.put("id", success);
        fileMap.put("type", Constants.INSERT);
        fileMap.put("username", resultData.get("username"));
        fileMap.put("password", resultData.get("password"));
        fileMap.put("resourceId", resource.getResourceId());
        fileMap.put("version", resource.getVersion());
        fileMap.put("traceId", resource.getTraceId());
        fileMap.put("orgId", resource.getOrgId());
        fileMap.put("tokenUrl", spaceUrl.getGetTokenUrl());
        fileMap.put("ftpUrl", resultData.get("ftpUrl"));
        fileMap.put("dataType", resource.getDataType());
        fileMap.put("spaceId", resource.getSpaceId());
        log.info("任务：发布文件推送 traceId" + resource.getTraceId() + " path:" + resource.getRealPath());
        // Join the task queue to start processing
        publicFileStop.put(resource.getResourceId() + resource.getVersion(), false);
        SpaceQuery instance = SpaceQuery.getInstance();
        instance.addFileSendCache(resource.getFounderId(), fileMap, "sendFile", mongoTemplate);
        return ResultUtil.success(success);
    }

    /**
     * Request instdb to publish interface
     */
    private String requestInstDBPublic(HttpClient httpClient, String token, String param, String orgId) {
        String publicUrl = insertDBData.getIfPresent(Constants.DATASET_PUBLISH + orgId);
        Map publicMap = JSONObject.parseObject(publicUrl, Map.class);
        Map<String, String> header = new HashMap<>(8);
        header.put("token", token);
        header.put("version", publicMap.get("version").toString());
        try {
            return httpClient.doPostHeader(param, publicMap.get("url").toString(), header);
        } catch (Exception e) {
            log.error("互操作接口连接 发布接口 失败 {} " + e.getMessage());
            throw new CommonException(messageInternational("DS_RESOURCE_PUBLISH"));
        }
    }

    /**
     * Obtain and publish insertDB objects
     */
    private SendInsertDB conversion(ResourceV2 resource, List<String> pathList) {
        SendInsertDB sendResource = new SendInsertDB();
        sendResource.setVersion(resource.getVersion());
        sendResource.setResourceType(resource.getResourceType());
        sendResource.setTemplateName(resource.getTemplate());
        Map<String, Object> publish = new HashMap<>();
        publish.put("name", resource.getFounderName());
        publish.put("email", resource.getFounder());
        publish.put("org", resource.getFounderOrg());
        Map<String, Object> organization = new HashMap<>();
        organization.put("id", resource.getOrgId());
        organization.put("name", resource.getOrgName());
        Map<String, Object> callbackUrl = new HashMap<>();
        callbackUrl.put("onSuccess", spaceUrl.getCallbackUrl());
        callbackUrl.put(" onUpdate", spaceUrl.getResourceUpdateUrl());
        sendResource.setPublish(publish);
        sendResource.setOrganization(organization);
        sendResource.setCallbackUrl(callbackUrl);
        Map<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("@context", "https://schema.org");
        dataMap.put("@type", "Dataset");
        // Differentiation between Chinese and English data
        Map<String, List<ResourceDo>> judgeMap = new HashMap<>();
        for (ResourceDo resourceDo : resource.getMetadata()) {
            String iri = resourceDo.getIri();
            String key = iri.substring(iri.lastIndexOf("/") + 1);
            if (!judgeMap.containsKey(key)) {
                judgeMap.put(key, new ArrayList<ResourceDo>() {

                    {
                        add(resourceDo);
                    }
                });
            } else {
                judgeMap.get(key).add(resourceDo);
            }
        }
        // Assembly format
        for (String key : judgeMap.keySet()) {
            List<ResourceDo> resourceDos = judgeMap.get(key);
            if (resourceDos.size() > 1) {
                // Chinese and English
                for (ResourceDo resourceDo : resourceDos) {
                    List<Map<String, Object>> languageDate = getLanguageDate((List) dataMap.get(key), resourceDo.getValue(), resourceDo.getLanguage());
                    dataMap.put(key, languageDate);
                }
            } else {
                ResourceDo resourceDo = resourceDos.get(0);
                String type = resourceDo.getType();
                switch(type) {
                    case "author":
                        dataMap.put(key, getAuthorList(resourceDo.getValue()));
                        break;
                    case "org":
                        dataMap.put(key, getSubjectOf(resourceDo.getValue(), "Organization"));
                        break;
                    case "paper":
                        dataMap.put(key, getSubjectOf(resourceDo.getValue(), "Paper"));
                        break;
                    case "project":
                        dataMap.put(key, getSubjectOf(resourceDo.getValue(), "Project"));
                        break;
                    case "DOI":
                        if (StringUtils.isEmpty((String) resourceDo.getValue()) && "apply".equals(resourceDo.getFormate())) {
                            dataMap.put(key, "apply");
                        } else {
                            dataMap.put(key, resourceDo.getValue());
                        }
                        break;
                    case "CSTR":
                        if (StringUtils.isEmpty((String) resourceDo.getValue()) && "apply".equals(resourceDo.getFormate())) {
                            dataMap.put(key, "apply");
                        } else {
                            dataMap.put(key, resourceDo.getValue());
                        }
                        break;
                    case "table":
                        dataMap.put(key, getTableList(resourceDo.getValue(), pathList));
                        break;
                    case "image":
                        Object value = resourceDo.getValue();
                        if (value != null) {
                            List list = (List) value;
                            if (list.size() > 0) {
                                String image = (String) list.get(0);
                                if (image.contains("/")) {
                                    try {
                                        String s = fileToBase64(new File(spaceUrl.getRootDir() + image));
                                        dataMap.put(key, new ArrayList<String>(1) {

                                            {
                                                add(s);
                                            }
                                        });
                                    } catch (Exception e) {
                                        dataMap.put(key, resourceDo.getValue());
                                    }
                                } else {
                                    dataMap.put(key, resourceDo.getValue());
                                }
                            }
                        } else {
                            dataMap.put(key, resourceDo.getValue());
                        }
                        break;
                    default:
                        dataMap.put(key, resourceDo.getValue());
                        break;
                }
            }
        }
        // dataMap.put("url","https://www.doi.org/10.11922/sciencedb.j00001.00145");
        sendResource.setMetadata(dataMap);
        return sendResource;
    }

    private List<Map<String, Object>> getLanguageDate(List metadata, Object value, String language) {
        List<Map<String, Object>> name = metadata;
        Map<String, Object> nameMap = new HashMap<>();
        nameMap.put("@value", value);
        nameMap.put("@language", language);
        if (name == null) {
            name = new ArrayList<Map<String, Object>>() {

                {
                    add(nameMap);
                }
            };
        } else {
            name.add(nameMap);
        }
        return name;
    }

    private List<Map<String, Object>> getSubjectOf(Object value, String type) {
        List<Map<String, Object>> subjectOf = new ArrayList<>();
        if (value == null) {
            return null;
        }
        List<Map<String, Object>> data = (List<Map<String, Object>>) value;
        if (type.equals("Paper")) {
            for (Map<String, Object> datum : data) {
                Map<String, Object> valueMap = new HashMap<>();
                ResponseResult<Object> responseResult = externalInterService.objectDel(datum.get("id").toString(), "Paper");
                if (responseResult.getCode() == 0) {
                    List<Map> dataList = (List<Map>) responseResult.getData();
                    if (dataList.size() <= 0) {
                        throw new CommonException(-1, messageInternational("DS_RESOURCE_PAPER"));
                    }
                    Map dataMap = dataList.get(0);
                    valueMap.put("@type", type);
                    valueMap.put("@id", dataMap.get("id"));
                    valueMap.put("name", dataMap.get("zh_Name"));
                    valueMap.put("en_name", null == dataMap.get("en_Name") ? "" : dataMap.get("en_Name"));
                    valueMap.put("publishStatus", null == dataMap.get("publishStatus") ? "" : dataMap.get("publishStatus"));
                    valueMap.put("periodical", null == dataMap.get("periodical") ? "" : dataMap.get("periodical"));
                    valueMap.put("doi", null == dataMap.get("doi") ? "" : dataMap.get("doi"));
                    valueMap.put("url", null == dataMap.get("url") ? "" : dataMap.get("url"));
                    valueMap.put("referenceInfo", null == dataMap.get("referenceInfo") ? "" : dataMap.get("referenceInfo"));
                    subjectOf.add(valueMap);
                } else {
                    throw new CommonException(-1, (String) responseResult.getMessage());
                }
            }
        } else {
            for (Map<String, Object> map : data) {
                Map<String, Object> nameMap = new HashMap<>();
                nameMap.put("@type", type);
                nameMap.put("@id", map.get("id"));
                nameMap.put("name", map.get("name"));
                subjectOf.add(nameMap);
            }
        }
        return subjectOf;
    }

    /**
     * Obtain Author
     */
    private List<Map<String, Object>> getAuthorList(Object value) {
        List<Map<String, Object>> authList = new ArrayList<>();
        if (value != null) {
            List<Map<String, Object>> valueList = (List<Map<String, Object>>) value;
            for (Map<String, Object> map : valueList) {
                Object type = map.get("type");
                if (type != null) {
                    if (type.toString().equals("Organization")) {
                        Map<String, Object> nameMap = new HashMap<>();
                        nameMap.put("@type", type);
                        nameMap.put("@id", map.get("id"));
                        nameMap.put("name", map.get("name"));
                        authList.add(nameMap);
                        continue;
                    }
                }
                Map<String, Object> valueMap = new HashMap<>();
                ResponseResult<Object> responseResult = externalInterService.objectDel(map.get("id").toString(), "Person");
                if (responseResult.getCode() == 0) {
                    List<Map> dataList = (List<Map>) responseResult.getData();
                    if (dataList.size() <= 0) {
                        throw new CommonException(-1, messageInternational("DS_RESOURCE_AUTHOR"));
                    }
                    Map dataMap = dataList.get(0);
                    // Add basic information of the author
                    valueMap.put("@type", "Person");
                    valueMap.put("@id", map.get("id"));
                    valueMap.put("name", dataMap.get("zh_Name"));
                    valueMap.put("email", dataMap.get("email"));
                    valueMap.put("en_name", null == dataMap.get("en_Name") ? "" : dataMap.get("en_Name"));
                    // Institutional Information
                    Map<String, Object> org = new HashMap<>();
                    try {
                        List<Map<String, Object>> orgList = (List<Map<String, Object>>) dataMap.get("employment");
                        if (orgList != null && !orgList.isEmpty()) {
                            Map<String, Object> orgMap = orgList.get(0);
                            org.put("@type", "Organization");
                            org.put("@id", orgMap.get("id"));
                            org.put("name", orgMap.get("zh_Name"));
                            org.put("en_name", orgMap.get("en_Name"));
                            valueMap.put("Organization", org);
                        } else {
                            org.put("@type", "Organization");
                            org.put("@id", "");
                            org.put("name", "");
                            valueMap.put("Organization", org);
                        }
                    } catch (Exception e) {
                        org.put("@type", "Organization");
                        org.put("@id", "");
                        org.put("name", "");
                        valueMap.put("Organization", org);
                    }
                } else {
                    throw new CommonException(-1, (String) responseResult.getMessage());
                }
                authList.add(valueMap);
            }
        }
        return authList;
    }

    private List<Map<String, Object>> getTableList(Object value, List<String> pathList) {
        if (null == value) {
            return null;
        }
        String stringValue;
        try {
            stringValue = (String) value;
        } catch (Exception e) {
            throw new CommonException(-1, messageInternational("DS_TEMPLATE_TYPE"));
        }
        Query query = new Query().addCriteria(Criteria.where("sampleId").is(stringValue));
        long count = mongoTemplate.count(query, SampleDo.class);
        if (count <= 0) {
            return null;
        }
        List<SampleDo> sampleDoList = mongoTemplate.find(query, SampleDo.class);
        List<Map<String, Object>> objects = new ArrayList<>(sampleDoList.size());
        for (SampleDo sampleDo : sampleDoList) {
            List<SampleCore> sampleCoreList = sampleDo.getSampleCoreList();
            Map<String, Object> dataMap = new HashMap<>(sampleCoreList.size());
            for (SampleCore sampleCore : sampleCoreList) {
                if (sampleCore.getName().equals("originalData")) {
                    List<Map<String, Object>> originalData = (List<Map<String, Object>>) sampleCore.getValue();
                    for (Map<String, Object> originalDatum : originalData) {
                        String path = originalDatum.get("path").toString();
                        String fileName = originalDatum.get("fileName").toString();
                        // The first scenario is to directly transfer the file
                        boolean one = false;
                        for (String s : pathList) {
                            if (path.equals(s)) {
                                one = true;
                                originalDatum.put("path", "/" + fileName);
                                break;
                            }
                        }
                        if (!one) {
                            // The second scenario is to transfer the file parent path
                            for (String s : pathList) {
                                if (path.contains(s)) {
                                    String partPath = FileUtils.getFileName(s);
                                    String replace = path.replace(s, "");
                                    if (replace.indexOf("/") != 0) {
                                        replace = "/" + replace;
                                    }
                                    originalDatum.put("path", "/" + partPath + replace);
                                    break;
                                }
                            }
                        }
                    }
                }
                try {
                    String value1 = (String) sampleCore.getValue();
                    dataMap.put(sampleCore.getName(), value1);
                } catch (Exception e) {
                    try {
                        Map mapValue = (Map) sampleCore.getValue();
                        dataMap.put(sampleCore.getName(), mapValue.get("id"));
                    } catch (Exception e3) {
                        dataMap.put(sampleCore.getName(), sampleCore.getValue());
                    }
                }
            }
            objects.add(dataMap);
        }
        return objects;
    }

    /**
     * File parsing
     */
    private Map<String, Object> getFileList(List<RequestFile> fileList, HttpServletRequest request, String spaceId) {
        Map<String, Object> fileMap = new HashMap<>();
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
        List<String> pathList = new ArrayList<>();
        getWebFile(fileList, pathList, elfinderStorage, spaceId);
        fileMap.put("pathList", pathList);
        String rootPath = elfinderStorage.getVolumes().get(0).getRoot().toString();
        fileMap.put("manPath", rootPath);
        // fileMap.put("sendFile",sendFileList);
        return fileMap;
    }

    /**
     * Resolve incoming folders from the front-end
     */
    private void getWebFile(List<RequestFile> files, List<String> pathList, ElfinderStorage elfinderStorage, String spaceId) {
        try {
            for (RequestFile requestFile : files) {
                Target target = elfinderStorage.fromHash(requestFile.getHash());
                String path = target.toString();
                File file = new File(path);
                if (file.exists()) {
                    if (file.isDirectory()) {
                        if (file.listFiles().length == 0) {
                            throw new CommonException(-1, "文件夹 " + file.getName() + " 内无文件,暂不支持发布,请重新选择文件发布！");
                        } else {
                            long invoke = fileMappingManage.getSizeBytes(path, spaceId);
                            if (invoke == 0) {
                                throw new CommonException(-1, "文件夹 " + file.getName() + " 内无文件,暂不支持发布,请重新选择文件发布！");
                            }
                        }
                    } else {
                        if (file.length() == 0) {
                            throw new CommonException(-1, "文件 " + file.getName() + " 大小为0,暂不支持发布,请重新选择文件发布！");
                        }
                    }
                    pathList.add(path);
                } else {
                    int i = path.lastIndexOf(FILE_SPLIT);
                    String fileName = path.substring(i + 1);
                    throw new CommonException(-1, "文件或文件夹 " + fileName + " 不存在或者文件名称文件路径已被更改,请重新选择文件发布！");
                }
            }
        } catch (Exception e) {
            throw new CommonException(-1, messageInternational("DS_PUBLISH"));
        }
    }

    private List<Map> getModelData(String templateId, String orgId, String id) {
        // Encapsulation Template Data
        String ifPresent = publicModel.getIfPresent(templateId);
        if (ifPresent == null) {
            templateList(orgId);
            ifPresent = publicModel.getIfPresent(templateId);
        }
        List<Map> result;
        if (ifPresent != null) {
            result = JSONObject.parseObject(ifPresent, ArrayList.class);
        } else {
            if (StringUtils.isNotEmpty(id)) {
                Query query = new Query().addCriteria(Criteria.where("_id").is(id));
                mongoTemplate.remove(query, ResourceV2.class);
            }
            throw new CommonException(-3, messageInternational("DS_RESOURCE_META"));
        }
        return result;
    }

    private void loadingModelCheck(String templateId, String orgId) {
        String ifPresent = publicModel.getIfPresent(templateId);
        if (ifPresent == null) {
            templateList(orgId);
        }
    }

    /**
     * Encapsulate file processing path//Generate compressed package path
     */
    private Map getFileParam(String founderId, String resourceId, String version) {
        String releasePath = spaceUrl.getReleaseStored() + "/" + founderId;
        String realPath = releasePath + "/" + resourceId + version + ".zip";
        String csvPath = releasePath + "/" + resourceId + version + "-csv";
        File file = new File(csvPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        Map<String, String> fileParam = new HashMap<>();
        fileParam.put("releasePath", releasePath);
        fileParam.put("targetPath", realPath);
        fileParam.put("csvRoot", csvPath);
        return fileParam;
    }

    /**
     * Obtain resource
     */
    public ResourceV2 getResource(Token user, ResourceRequest resourceRequest) {
        ResourceV2 resource = new ResourceV2();
        if (StringUtils.isEmpty(resourceRequest.getResourceId())) {
            resource.setResourceId(CommonUtils.generateUUID());
        }
        String spaceId = resourceRequest.getSpaceId();
        resource.setDataType(resourceRequest.getDataType());
        resource.setCreateTime(new Date());
        resource.setUpdateTime(new Date());
        resource.setOrgId(resourceRequest.getOrgId());
        resource.setOrgName(resourceRequest.getOrgName());
        resource.setVersion(resourceRequest.getVersion());
        resource.setSpaceId(spaceId);
        resource.setTemplateId(resourceRequest.getTemplateId());
        resource.setTemplate(resourceRequest.getTemplate());
        // resource.setOrgUrl(resourceRequest.getTemplateUrl());
        // resource.setOrgCode(resourceRequest.getReleaseUrl());
        resource.setFounderId(user.getUserId());
        resource.setFounderName(user.getName());
        resource.setFounder(user.getEmailAccounts());
        resource.setResourceType(resourceRequest.getResourceType());
        if (StringUtils.isNotEmpty(resourceRequest.getOrgId())) {
            String ifPresent = publicModel.getIfPresent(resourceRequest.getOrgId());
            Map map = JSONObject.parseObject(ifPresent);
            if (map != null) {
                resource.setResourceTypeShow((String) map.get(resourceRequest.getResourceType()));
            }
        }
        resource.setType(resourceRequest.getType());
        resource.setPlan(resourceRequest.getPlan());
        List<ResourceDo> resourceDoList = resourceRequest.getResourceDoList();
        if (resourceDoList != null && resourceDoList.size() > 0) {
            parsingRelease(resource, resourceRequest.getResourceDoList());
            resource.setMetadata(resourceDoList);
        }
        return resource;
    }

    /**
     * Edit resource
     */
    private void editResource(ResourceV2 resource, ResourceRequest resourceRequest) {
        String spaceId = resourceRequest.getSpaceId();
        resource.setOrgId(resourceRequest.getOrgId());
        resource.setOrgName(resourceRequest.getOrgName());
        resource.setSpaceId(spaceId);
        resource.setTemplateId(resourceRequest.getTemplateId());
        resource.setTemplate(resourceRequest.getTemplate());
        resource.setVersion(resourceRequest.getVersion());
        // resource.setOrgUrl(resourceRequest.getTemplateUrl());
        // resource.setOrgCode(resourceRequest.getReleaseUrl());
        resource.setResourceType(resourceRequest.getResourceType());
        resource.setType(resourceRequest.getType());
        if (StringUtils.isNotEmpty(resourceRequest.getOrgId())) {
            String ifPresent = publicModel.getIfPresent(resourceRequest.getOrgId());
            Map map = JSONObject.parseObject(ifPresent);
            resource.setResourceTypeShow((String) map.get(resourceRequest.getResourceType()));
        }
        resource.setPlan(resourceRequest.getPlan());
        resource.setDataType(resourceRequest.getDataType());
        List<ResourceDo> resourceDoList = resourceRequest.getResourceDoList();
        if (resourceDoList != null && resourceDoList.size() > 0) {
            parsingRelease(resource, resourceDoList);
            resource.setMetadata(resourceDoList);
        }
    }

    /**
     * Process template data
     */
    private void parsingRelease(ResourceV2 resource, List<ResourceDo> resourceRequestList) {
        resourceRequestList.stream().forEachOrdered(resourceRequest -> {
            // *****************
            String iri = resourceRequest.getIri();
            String key = iri.substring(iri.lastIndexOf("/") + 1);
            String language = resourceRequest.getLanguage();
            Object value = resourceRequest.getValue();
            switch(key) {
                case "keywords":
                    if (language.equals("zh")) {
                        resource.setKeywordCH((List) value);
                    } else {
                        resource.setKeywordEN((List) value);
                    }
                    break;
                case "name":
                    if (language.equals("zh")) {
                        resource.setTitleCH((String) value);
                    } else {
                        resource.setTitleEN((String) value);
                    }
                    break;
                case "image":
                    if (value != null) {
                        List list = (List) value;
                        if (list.size() > 0) {
                            String base64 = (String) list.get(0);
                            if (CommonUtils.isPicBase(base64)) {
                                String head = String.valueOf(new Date().getTime());
                                String imagePath = "/" + Constants.Image.image + "/" + Constants.Image.RESOURCE + "/" + resource.getResourceId() + resource.getVersion() + "_" + head + ".jpg";
                                try {
                                    CommonUtils.generateImage(base64, spaceUrl.getRootDir() + imagePath);
                                    resource.setImage(imagePath);
                                    resourceRequest.setValue(new ArrayList<String>(1) {

                                        {
                                            add(imagePath);
                                        }
                                    });
                                } catch (Exception e) {
                                    resource.setImage(base64);
                                }
                            } else {
                                resource.setImage(base64);
                            }
                        }
                    }
                default:
                    break;
            }
        });
    }

    public ResponseResult<Object> retry(String id, HttpServletRequest request) {
        if (StringUtils.isEmpty(id)) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        String token = jwtTokenUtils.getToken(request);
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        ResourceV2 resource = mongoTemplate.findOne(query, ResourceV2.class, COLLECTION_NAME);
        if (resource == null) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        spaceControlConfig.spatialVerification(resource.getSpaceId(), jwtTokenUtils.getEmail(token), Constants.SpaceRole.LEVEL_OTHER);
        if (resource.getType() == Constants.AUDIT) {
            if (StringUtils.isNotEmpty(resource.getFtpHost())) {
                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("id", resource.getId());
                fileMap.put("username", resource.getFtpUsername());
                fileMap.put("password", resource.getFtpPassword());
                fileMap.put("resourceId", resource.getResourceId());
                fileMap.put("version", resource.getVersion());
                fileMap.put("traceId", resource.getTraceId());
                fileMap.put("orgId", resource.getOrgId());
                fileMap.put("tokenUrl", spaceUrl.getGetTokenUrl());
                fileMap.put("ftpUrl", resource.getFtpHost());
                fileMap.put("spaceId", resource.getSpaceId());
                fileMap.put("dataType", resource.getDataType());
                fileMap.put("type", Constants.INSERT);
                List<RequestFile> fileList = resource.getViewFileList();
                Map<String, Object> pubFile = null;
                if (null != fileList && !fileList.isEmpty()) {
                    pubFile = getFileList(fileList, request, resource.getSpaceId());
                }
                List<String> tableList = resource.getTableList();
                // Generate file processing parameters
                Map fileParam = getFileParam(resource.getFounderId(), resource.getResourceId(), resource.getVersion());
                paramMapAdd(fileParam, pubFile, resource.getDataType(), resource.getSpaceId(), tableList);
                fileMap.putAll(fileParam);
                log.info("任务：数据重新传输开始 traceId" + resource.getTraceId() + " path:" + resource.getRealPath());
                Boolean fileSend = resource.getFileSend();
                publicFileStop.put(resource.getResourceId() + resource.getVersion(), false);
                Update update = new Update();
                update.set("fileSuccessOf", 0);
                update.set("fileSend", resource.getFileSend());
                mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(resource.getId())), update, ResourceV2.class);
                if (null == fileSend || !fileSend) {
                    // Join the task queue to start processing
                    SpaceQuery instance = SpaceQuery.getInstance();
                    instance.addFileSendCache(resource.getFounderId(), fileMap, "sendFile", mongoTemplate);
                } else if (fileSend) {
                    // Join the task queue to start processing
                    SpaceQuery instance = SpaceQuery.getInstance();
                    instance.addFileSendCache(resource.getFounderId(), fileMap, "sendApi", mongoTemplate);
                }
            }
        }
        return ResultUtil.success();
    }

    /**
     * Email resend
     */
    public ResponseResult<Object> emailReissue(String token, String id) {
        if (StringUtils.isEmpty(id)) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        ResourceV2 resource = mongoTemplate.findOne(query, ResourceV2.class, COLLECTION_NAME);
        if (resource == null) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        if (resource.getType() != Constants.PUBLISHED) {
            ResultUtil.error("只有审核通过的才支持邮件补发!");
        }
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("name", resource.getFounderName());
        attachment.put("email", resource.getFounder());
        attachment.put("resourceName", resource.getTitleCH());
        EmailModel emailAudit = EmailModel.EMAIL_AUDIT();
        emailAudit.setMessage(emailAudit.getMessage().replaceAll("resourceName", resource.getTitleCH()));
        attachment.put("url", resource.getDetailsUrl());
        BufferedImage image = QRCodeUtil.createImage("utf-8", resource.getDetailsUrl(), 300, 300);
        QRCodeUtil.addUpFont(image, "扫码查看数据资源");
        attachment.put("base", QRCodeUtil.GetBase64FromImage(image));
        asyncDeal.send(attachment, emailAudit, EmailRole.DATA_PUBLIC_AUDIT);
        return ResultUtil.success();
    }

    public ResponseResult<Object> releaseCallback(String approvalStatus, String reason, String rejectApproval, String resourceId, String doi, String cstr, String detailsUrl, MultipartFile file) {
        if (StringUtils.isEmpty(approvalStatus) || StringUtils.isEmpty(resourceId)) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        ResourceV2 byCode = findByCode(resourceId);
        if (byCode == null) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        if (byCode.getType() != Constants.AUDIT) {
            return ResultUtil.errorInternational("DS_RESOURCE_STATE");
        }
        String title = StringUtils.isNotEmpty(byCode.getTitleCH()) ? byCode.getTitleCH() : byCode.getTitleEN();
        Query query = new Query().addCriteria(Criteria.where("_id").is(byCode.getId()));
        Optional<ConsumerDO> userOptional = userRepository.findById(byCode.getFounderId());
        boolean present = userOptional.isPresent();
        // Change Audit Status
        Update update = new Update();
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("name", byCode.getFounderName());
        attachment.put("email", byCode.getFounder());
        attachment.put("resourceName", title);
        EmailModel emailAudit = EmailModel.EMAIL_AUDIT();
        Map<String, Object> msgMap = new HashMap<>(16);
        if (approvalStatus.equals("adopt")) {
            // Determine the status type: adopt/pass no/fail
            update.set("type", Constants.PUBLISHED);
            update.set("latest", true);
            update.set("DOI", doi);
            update.set("CSTR", cstr);
            update.set("detailsUrl", detailsUrl);
            update.set("publicTime", new Date());
            if (!byCode.getVersion().equals(Constants.VERSION)) {
                // Modify the status of the previous version
                Query query1 = new Query().addCriteria(Criteria.where("resourceId").is(byCode.getResourceId()).and("latest").is(true));
                Update update1 = new Update();
                update1.set("latest", false);
                mongoTemplate.upsert(query1, update1, COLLECTION_NAME);
            }
            spaceControlConfig.dataOut("released", byCode.getDataSize(), byCode.getSpaceId());
            // log record
            svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(byCode.getSpaceId()).version(SpaceSvnLog.ACTION_VALUE).description(byCode.getFounderName() + " 发布数据资源《" + title + "》").action(SpaceSvnLog.ACTION_PUBLISH).operatorId(byCode.getFounderId()).operator(new Operator(userRepository.findById(byCode.getFounderId()).get())).createTime(new Date()).build());
            if (present) {
                String content = messageInternational("DS_RESOURCE_CONTENT");
                // send message
                msgMap.put("title", messageInternational("DS_RESOURCE_AUDIT"));
                String msgTitle = content.replaceAll("title", title);
                msgMap.put("content", msgTitle);
                msgUtil.sendMsg(byCode.getFounder(), msgUtil.mapToString(msgMap));
                // msg record
                String linkUrl = spaceUrl.getPublicUrl();
                messageService.sendToApplicant(TITLE_PUBLISH_PASSED, msgTitle, new Person(userOptional.get()), 1, linkUrl);
                attachment.put("url", detailsUrl);
                BufferedImage image = QRCodeUtil.createImage("utf-8", detailsUrl, 300, 300);
                QRCodeUtil.addUpFont(image, "扫码查看数据资源");
                attachment.put("base", QRCodeUtil.GetBase64FromImage(image));
                emailAudit.setMessage(emailAudit.getMessage().replaceAll("resourceName", title));
            }
        } else if (approvalStatus.equals("no")) {
            update.set("type", Constants.REJECT);
            update.set("dismissTime", new Date());
            update.set("dismissReason", reason);
            update.set("rejectApproval", rejectApproval);
            // File processing
            String path = spaceUrl.getReleaseStored() + "/" + Constants.Release.PUBLIC_REJECTED + "/" + byCode.getResourceId() + "/" + byCode.getVersion();
            try {
                Map<String, Object> upload = upload(file, path);
                int code = (int) upload.get("code");
                if (code == 200) {
                    update.set("rejectFile", upload.get("rejectFile"));
                } else {
                    return ResultUtil.error(upload.get("message").toString());
                }
            } catch (Exception e) {
                return ResultUtil.errorInternational("DS_RESOURCE_TURN_DOWN");
            }
            if (present) {
                String rr = reason.length() > 10 ? reason.substring(0, 9) : reason;
                String content = "您提交的数据资源《" + title + "》未通过审核，未通过原因：[" + rr + "...]";
                msgMap.put("title", messageInternational("DS_RESOURCE_NOT_PASS"));
                msgMap.put("content", content);
                msgUtil.sendMsg(byCode.getFounder(), msgUtil.mapToString(msgMap));
                // msg record
                String linkUrl = spaceUrl.getPublicUrl();
                messageService.sendToApplicant(TITLE_PUBLISH_NOT_PASSED, content, new Person(userRepository.findById(byCode.getFounderId()).get()), 0, linkUrl);
                // email
                attachment.put("url", spaceUrl.getResourceUrl());
                emailAudit.setTitle(messageInternational("DS_RESOURCE_NOT_PASS"));
                emailAudit.setMessage("您提交的数据资源《" + title + "》未通过审核，未通过原因：[" + rr + "...]，可在数据发布里查看详情。");
            }
        } else {
            return ResultUtil.success(messageInternational("DS_RESOURCE_SUPPORT"));
        }
        if (present) {
            asyncDeal.send(attachment, emailAudit, EmailRole.DATA_PUBLIC_AUDIT);
        }
        mongoTemplate.upsert(query, update, COLLECTION_NAME);
        return ResultUtil.success();
    }

    private Map<String, Object> upload(MultipartFile file, String path) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        List<SendFile> sendFileList = new ArrayList<>();
        if (file != null) {
            String originalFilename = file.getOriginalFilename();
            long size = file.getSize();
            if (size > 104857600) {
                map.put("code", 500);
                map.put("message", messageInternational("DS_RESOURCE_LIMIT"));
                return map;
            }
            SendFile sendFile = new SendFile();
            sendFile.setFileName(originalFilename);
            sendFile.setSize(size);
            File file1 = new File(path);
            if (!file1.exists()) {
                file1.mkdirs();
            }
            String filePath = path + "/" + originalFilename;
            try {
                // create a file
                File saveFile = new File(filePath);
                // file save
                file.transferTo(saveFile);
                sendFile.setFileId(SMS4.Encryption(filePath));
                sendFileList.add(sendFile);
            } catch (Exception e) {
                e.printStackTrace();
                map.put("code", 500);
                map.put("message", messageInternational("DS_RESOURCE_SAVE"));
                return map;
            }
        }
        map.put("rejectFile", sendFileList);
        map.put("message", "success");
        return map;
    }

    /**
     * Query Resources
     */
    private ResourceV2 findByCode(String code) {
        Query query = new Query();
        query.addCriteria(Criteria.where("traceId").is(code));
        ResourceV2 one = mongoTemplate.findOne(query, ResourceV2.class, COLLECTION_NAME);
        return one;
    }

    /**
     * Obtain insertDB token (login)
     */
    private String getToken(HttpClient httpClient, String url, String orgId, String type) {
        if (type.equals("refresh")) {
            // Refresh
            insertDBData.invalidate(Constants.INS_TOKEN + orgId);
        }
        // token
        String token = insertDBData.getIfPresent(Constants.INS_TOKEN + orgId);
        if (StringUtils.isEmpty(token)) {
            // Login user to obtain token
            Query queryOne = new Query().addCriteria(Criteria.where("orgId").is(orgId));
            ReleaseAccount one = mongoTemplate.findOne(queryOne, ReleaseAccount.class);
            if (one == null) {
                throw new CommonException(-1, messageInternational("DS_RESOURCE_ORG"));
            }
            String result;
            try {
                Map<String, String> ifPresent = publicOrgUrl.getIfPresent(orgId);
                if (ifPresent == null) {
                    externalInterService.accessOrgList(null, Constants.GENERAL);
                    ifPresent = publicOrgUrl.getIfPresent(orgId);
                }
                if (ifPresent == null) {
                    throw new CommonException(-1, messageInternational("DS_ORG_NOT_FOUND"));
                }
                String host = ifPresent.get("host");
                result = httpClient.doGetHeader(host + url, one.getAuthCode());
            } catch (Exception e) {
                e.printStackTrace();
                throw new CommonException(-1, messageInternational("DS_AUTHORIZE"));
            }
            HashMap hashMap;
            try {
                hashMap = JSONObject.parseObject(result, HashMap.class);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CommonException(-1, messageInternational("DS_AUTHORIZE_TIMEOUT"));
            }
            if (CollectionUtils.isEmpty(hashMap)) {
                throw new CommonException(-1, messageInternational("DS_AUTHORIZE_TIMEOUT"));
            }
            if (!hashMap.containsKey("ticket")) {
                throw new CommonException(-1, messageInternational("DS_AUTHORIZE_TIMEOUT"));
            }
            if (hashMap.containsKey("serviceList")) {
                List<Map<String, Object>> urlList = (List) hashMap.get("serviceList");
                for (Map<String, Object> objectMap : urlList) {
                    String name = objectMap.get("name").toString();
                    insertDBData.put(name + orgId, JSONObject.toJSONString(objectMap));
                }
            }
            Map data = (Map) hashMap.get("ticket");
            insertDBData.put(Constants.INS_TOKEN + orgId, data.get("token").toString());
            token = data.get("token").toString();
        }
        return token;
    }

    /**
     * Successfully submitted
     */
    private String success(ResourceV2 resource) {
        // Pending review
        resource.setType(Constants.AUDIT);
        resource.setSuccessOf(true);
        // preserve
        ResourceV2 save = resourceRepository.save(resource);
        return save.getId();
    }

    /**
     * Query Resources
     */
    public ResourceV2 findByResourceId(String resourceId, String version, int type) {
        Query query = new Query();
        query.addCriteria(Criteria.where("resourceId").is(resourceId).and("version").is(version).and("type").is(type));
        ResourceV2 one = mongoTemplate.findOne(query, ResourceV2.class, COLLECTION_NAME);
        return one;
    }

    /**
     * Template parsing
     */
    public ResponseResult<Object> templateList(String orgId) {
        if (null == orgId || StringUtils.isEmpty(orgId.trim())) {
            return ResultUtil.errorInternational("DS_PUBLISH_ORG");
        }
        HttpClient httpClient = new HttpClient();
        String code = getToken(httpClient, spaceUrl.getGetTokenUrl(), orgId, Constants.GAIN);
        String xml = requestInstDBTemplate(httpClient, code, orgId);
        List<Map<String, Object>> xmlList;
        try {
            xmlList = JSONObject.parseObject(xml, List.class);
        } catch (Exception e) {
            code = getToken(httpClient, spaceUrl.getGetTokenUrl(), orgId, Constants.REFRESH);
            xml = requestInstDBTemplate(httpClient, code, orgId);
            xmlList = JSONObject.parseObject(xml, List.class);
        }
        if (null == xmlList) {
            return ResultUtil.success();
        }
        // Record Template
        Map<String, Object> typeMap = new HashMap<>(16);
        List<Map> templateList = new ArrayList<>();
        Map<String, List<Map<String, Object>>> tempMap = new HashMap<>(8);
        for (Map<String, Object> template : xmlList) {
            String type = (String) template.get("type");
            if (StringUtils.isEmpty(type)) {
                type = "11-数据集";
            }
            String tempId = DigestUtils.md5DigestAsHex(template.get("name").toString().getBytes());
            Map temp = templateDetails(tempId, template.get("url").toString(), type.substring(0, type.indexOf("-")));
            if (tempMap.containsKey(type)) {
                tempMap.get(type).add(temp);
            } else {
                tempMap.put(type, new ArrayList<Map<String, Object>>() {

                    {
                        add(temp);
                    }
                });
            }
        }
        for (String key : tempMap.keySet()) {
            Map<String, Object> resultMap = new HashMap<>(8);
            List<Map<String, Object>> valueList = new ArrayList<>(8);
            Map<String, Object> valueMap = new HashMap<>(8);
            valueMap.put("label", key.substring(key.indexOf("-") + 1));
            valueMap.put("value", key.substring(0, key.indexOf("-")));
            typeMap.put(key.substring(0, key.indexOf("-")), key.substring(key.indexOf("-") + 1));
            valueList.add(valueMap);
            resultMap.put("data", tempMap.get(key));
            resultMap.put("type", valueList);
            templateList.add(resultMap);
        }
        if (typeMap.size() > 0) {
            publicModel.put(orgId, JSONObject.toJSONString(typeMap));
        }
        return ResultUtil.success(templateList);
    }

    /**
     * Please request instdb to obtain the template interface
     */
    private String requestInstDBTemplate(HttpClient httpClient, String token, String orgId) {
        String publicUrl = insertDBData.getIfPresent(Constants.GET_TEMPLATES + orgId);
        Map publicMap = JSONObject.parseObject(publicUrl, Map.class);
        Map<String, String> header = new HashMap<>(8);
        header.put("token", token);
        header.put("version", publicMap.get("version").toString());
        try {
            return httpClient.doGet(publicMap.get("url").toString(), header);
        } catch (Exception e) {
            log.error("互操作接口连接 获取模板接口 失败 {} " + e.getMessage());
            throw new CommonException(messageInternational("DS_RESOURCE_INSERT"));
        }
    }

    private Map<String, Object> templateDetails(String tempId, String url, String type) {
        Map<String, Object> tempMap = new HashMap<>();
        // Central account
        boolean judge = false;
        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        Object acc = cacheLoading.loadingCenterOpen();
        if (acc != null) {
            judge = true;
        }
        Template template;
        try {
            template = XmlTemplateUtil.getTemplate(url);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(-1, messageInternational("DS_TEMPLATE"));
        }
        List<Template.Group> groups = template.getGroup();
        // Required Field Verification
        Map<String, String> param = new HashMap<>();
        List<String> ju = new ArrayList<>();
        for (Template.Group group : groups) {
            List<Template.Resource> resources = group.getResources();
            for (Template.Resource resource : resources) {
                String multiply = resource.getMultiply();
                if (StringUtils.isNotEmpty(multiply) && StringUtils.isNotEmpty(multiply.trim())) {
                    String iri = resource.getIri();
                    String key = iri.substring(iri.lastIndexOf("/") + 1);
                    String language = resource.getLanguage();
                    param.put(key + language, resource.getTitle() + "~" + multiply);
                }
                if (!judge) {
                    ju.add(resource.getType());
                }
            }
        }
        publicModel.put(tempId, JSON.toJSONString(groups));
        publicModel.put(tempId + Constants.CHECK, JSON.toJSONString(param));
        if (!judge) {
            boolean pa = true;
            for (String tag : tags) {
                if (ju.contains(tag)) {
                    pa = false;
                    break;
                }
            }
            judge = pa;
        }
        tempMap.put("id", tempId);
        tempMap.put("disable", judge);
        tempMap.put("name", template.getTemplateName());
        tempMap.put("type", type);
        tempMap.put("templateInfo", template);
        return tempMap;
    }

    /**
     * data space senior admin including space owner
     * senior admins have many authorities excluding delete space
     */
    private boolean isSpaceSeniorAdmin(String spaceId, String userId) {
        boolean flag = false;
        Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        if (spaceOptional.isPresent()) {
            Space space = spaceOptional.get();
            for (AuthorizationPerson authorizationPerson : space.getAuthorizationList()) {
                if (StringUtils.equals(authorizationPerson.getUserId(), userId) && StringUtils.equals(authorizationPerson.getRole(), SPACE_SENIOR)) {
                    flag = true;
                    break;
                }
            }
        }
        return flag || StringUtils.equals(userId, spaceOptional.get().getUserId());
    }

    /**
     * Stem cell template adaptation download
     */
    public void tempDown(String orgId, String templateId, String iri, HttpServletResponse response) throws IOException {
        if (orgId.trim().equals("") || iri.trim().equals("")) {
            return;
        }
        List<Map> modelData = getModelData(templateId, orgId, null);
        String iriTo = iri.substring(iri.lastIndexOf("/") + 1);
        StringBuffer fileTitle = new StringBuffer();
        List<Template.Children> gxbModel = getGXBModel(modelData, iriTo, fileTitle);
        if (gxbModel == null) {
            return;
        }
        List<String> tempList = new ArrayList<>(gxbModel.size());
        List<String> dataList = new ArrayList<>(gxbModel.size());
        for (Template.Children children : gxbModel) {
            tempList.add(children.getTitle());
            dataList.add(getSampleData(children.getName()));
        }
        // Create Workbook
        HSSFWorkbook workBook = null;
        // Obtain output flow based on the response ancestor
        OutputStream outputStream = null;
        try {
            // Declare the corresponding text type
            response.setContentType("application/application/vnd.ms-excel");
            // set name
            String filename = fileTitle.toString() + "导入模板.xls";
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
            workBook = new HSSFWorkbook();
            // Create a table in the name of a workbook
            HSSFSheet hssfSheetnew = workBook.createSheet();
            HSSFRow hssfRow = hssfSheetnew.createRow(0);
            HSSFRow hssfRow2 = hssfSheetnew.createRow(1);
            for (int y = 0; y < tempList.size(); y++) {
                HSSFCell hssfCell = hssfRow.createCell(y);
                HSSFCell hssfCell2 = hssfRow2.createCell(y);
                try {
                    hssfCell.setCellValue(tempList.get(y));
                    hssfCell2.setCellValue(dataList.get(y));
                } catch (Exception e) {
                    hssfCell.setCellValue("");
                    hssfCell2.setCellValue("");
                }
            }
            outputStream = response.getOutputStream();
            // Call the Excel table download function of the browser function through the output stream
            workBook.write(outputStream);
        } catch (IOException e) {
            log.debug("-------------  File export failed!  -------------------");
            response.sendError(500, "模板解析错误!");
        } catch (Exception e) {
            log.debug("-------------  File export failed!  -------------------");
            e.printStackTrace();
            response.sendError(500, "模板解析错误!");
        } finally {
            workBook.close();
            outputStream.flush();
            outputStream.close();
        }
        return;
    }

    /**
     * Template Rule Analysis
     */
    private List<Template.Children> getGXBModel(List<Map> modelData, String iri, StringBuffer title) {
        if (null == modelData) {
            return null;
        }
        for (Map modelDatum : modelData) {
            List resources = (List) modelDatum.get("resources");
            for (Object object : resources) {
                Template.Resource resource = JSONObject.parseObject(JSONObject.toJSONString(object), Template.Resource.class);
                String getIri = resource.getIri();
                String key = getIri.substring(getIri.lastIndexOf("/") + 1);
                if (key.equals(iri)) {
                    title.append(resource.getTitle());
                    List<Template.Options> operation = resource.getOperation();
                    for (Template.Options options : operation) {
                        if (options.getType().equals("save") && null != options.getChildren() && !options.getChildren().isEmpty()) {
                            return options.getChildren();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Template Sample Data Addition
     */
    private String getSampleData(String name) {
        String value;
        switch(name) {
            case "name":
                value = "样例数据";
                break;
            case "species":
                value = "人";
                break;
            case "organ":
                value = "肺";
                break;
            case "omics":
                value = "基因组";
                break;
            case "cells":
                value = "造血干细胞";
                break;
            case "sex":
                value = "男性/雄性";
                break;
            case "strain":
                value = "DBA";
                break;
            case "age":
                value = "old";
                break;
            case "interpose":
                value = "Plasmid transfection";
                break;
            case "processDescription":
                value = "for single cells - direct lysis...";
                break;
            case "platform":
                value = "Illumina HiSeq 2500";
                break;
            case "type":
                value = "scRNA-Seq";
                break;
            case "originalData":
                value = "N0555-T_1.fastq.gz （多个文件用英文分号(;)隔开）";
                break;
            case "imagingInstrument":
                value = "film scanner";
                break;
            case "imagingParameters":
                value = "optimum";
                break;
            default:
                value = "";
                break;
        }
        return value;
    }

    /**
     * Adding Single Sample Data
     */
    public ResponseResult<Object> sampleAdd(RequestSample requestData, HttpServletRequest request) {
        // Parameter verification
        List<String> validation = CommonUtils.validation(requestData);
        if (validation.size() > 0) {
            return ResultUtil.error(-1, validation.toString());
        }
        String token = jwtTokenUtils.getToken(request);
        spaceControlConfig.spatialVerification(requestData.getSpaceId(), jwtTokenUtils.getEmail(token), Constants.SpaceRole.LEVEL_OTHER);
        String sampleId;
        if (StringUtils.isNotEmpty(requestData.getSampleId()) && !requestData.getSampleId().trim().equals("")) {
            sampleId = requestData.getSampleId();
        } else {
            sampleId = CommonUtils.generateUUID();
        }
        String templateId = requestData.getTemplateId();
        String orgId = requestData.getOrgId();
        String iri = requestData.getIri();
        String iriTo = iri.substring(iri.lastIndexOf("/") + 1);
        List<Map> modelData = getModelData(templateId, orgId, null);
        List<Template.Children> gxbModel = getGXBModel(modelData, iriTo, new StringBuffer());
        Map<String, String> paramMap = new HashMap<>(gxbModel.size());
        Map<String, String> typeMap = new HashMap<>(gxbModel.size());
        for (Template.Children children : gxbModel) {
            String name = children.getName();
            String multiply = children.getMultiply();
            paramMap.put(name, children.getTitle() + "~" + multiply);
            typeMap.put(name, children.getType());
        }
        Map<String, Object> sampleData = requestData.getSampleData();
        String name = "";
        for (String key : sampleData.keySet()) {
            if (key.equals("name")) {
                name = (String) sampleData.get(key);
            }
            ParameterValidation.iriCheck(paramMap, key, sampleData.get(key));
        }
        ParameterValidation.paramIsNot(paramMap);
        // Verification end data conversion
        List<SampleCore> sampleCoreList = new ArrayList<>(sampleData.size());
        for (String key : sampleData.keySet()) {
            SampleCore sampleCore = new SampleCore();
            sampleCore.setName(key);
            String type = typeMap.get(key);
            if (type.equals("dataspace")) {
                Object value = sampleData.get(key);
                if (null != value) {
                    List<String> hashList = (List) value;
                    StringBuffer buff = new StringBuffer("未匹配到文件:");
                    boolean judge = true;
                    List<Map<String, Object>> dataList = new ArrayList<>(hashList.size());
                    ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, requestData.getSpaceId());
                    for (String hash : hashList) {
                        String path = elfinderStorage.fromHash(hash).toString();
                        String rootPath = elfinderStorage.getVolumes().get(0).getRoot().toString();
                        File file = new File(path);
                        if (file.exists()) {
                            Map<String, Object> fileMap = new HashMap<>(3);
                            fileMap.put("fileName", file.getName());
                            fileMap.put("hash", hash);
                            fileMap.put("path", path.replace(rootPath, ""));
                            fileMap.put("size", file.length());
                            dataList.add(fileMap);
                        } else {
                            judge = false;
                            buff.append(file.getName() + ",");
                        }
                    }
                    sampleCore.setPerfect(judge);
                    sampleCore.setValue(dataList);
                    if (!judge) {
                        sampleCore.setAnnotation(((buff.toString().substring(0, buff.toString().length() - 1)) + "；"));
                    }
                }
            } else {
                sampleCore.setValue(sampleData.get(key));
                sampleCore.setPerfect(true);
            }
            sampleCoreList.add(sampleCore);
        }
        String id = requestData.getId();
        if (StringUtils.isNotEmpty(id) && !id.trim().equals("")) {
            Query query = new Query().addCriteria(Criteria.where("_id").is(id));
            SampleDo one = mongoTemplate.findOne(query, SampleDo.class);
            one.setSampleCoreList(sampleCoreList);
            one.setType(0);
            one.setAnnotation(null);
            mongoTemplate.save(one);
            return ResultUtil.success(sampleId);
        } else {
            SampleDo sampleDo = new SampleDo();
            sampleDo.setSampleId(sampleId);
            sampleDo.setName(name);
            sampleDo.setIri(iriTo);
            sampleDo.setTemplateId(templateId);
            sampleDo.setOrgId(orgId);
            sampleDo.setSampleCoreList(sampleCoreList);
            mongoTemplate.insert(sampleDo);
        }
        return ResultUtil.success(sampleId);
    }

    /**
     * Sample Query
     */
    public ResponseResult<Object> sampleQuery(Integer page, Integer size, Integer type, String iri, String sampleId) {
        Map<String, Object> resultMap = new HashMap<>();
        Query query = new Query();
        if (StringUtils.isEmpty(sampleId)) {
            resultMap.put("count", 0);
            resultMap.put("data", null);
            return ResultUtil.success(resultMap);
        }
        Criteria criteria = Criteria.where("sampleId").is(sampleId);
        String iriTo = iri.substring(iri.lastIndexOf("/") + 1);
        criteria.and("iri").is(iriTo);
        if (type == 1) {
            criteria.and("type").is(type);
        }
        query.addCriteria(criteria);
        long count = mongoTemplate.count(query, SampleDo.class);
        List<SampleDo> sampleDos = null;
        if (count > 0) {
            query.with(Sort.by(Sort.Order.desc("createTime")));
            query.with(PageRequest.of(page - 1, size));
            sampleDos = mongoTemplate.find(query, SampleDo.class);
        }
        long perfect = 0l;
        long all = 0l;
        if (type == 0) {
            all = count;
            if (count > 0) {
                criteria.and("type").is(1);
                perfect = mongoTemplate.count(new Query().addCriteria(criteria), SampleDo.class);
            }
        } else if (type == 1) {
            perfect = count;
            all = mongoTemplate.count(new Query().addCriteria(Criteria.where("sampleId").is(sampleId).and("iri").is(iriTo)), SampleDo.class);
        }
        resultMap.put("perfect", perfect);
        resultMap.put("all", all);
        resultMap.put("data", sampleDos);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> sampleDelete(String id) {
        if (StringUtils.isNotEmpty(id) && !id.trim().equals("")) {
            Query query = new Query().addCriteria(Criteria.where("_id").is(id));
            SampleDo one = mongoTemplate.findOne(query, SampleDo.class);
            if (null != one) {
                String ifPresent = publicModel.getIfPresent(one.getSampleId());
                if (ifPresent != null) {
                    publicModel.put(one.getSampleId(), String.valueOf(Integer.valueOf(ifPresent) - 1));
                }
                mongoTemplate.remove(query, SampleDo.class);
            }
        }
        return ResultUtil.success();
    }

    /**
     * Batch addition of samples (analyzed by template)
     */
    public ResponseResult<Object> sampleBatchAdd(String token, String sampleId, String spaceId, String orgId, String templateId, String iri, String[] fileList, String fileHash, MultipartFile mulFile, HttpServletRequest request) throws IOException {
        if (StringUtils.isEmpty(sampleId) || sampleId.trim().equals("")) {
            sampleId = CommonUtils.generateUUID();
        }
        if ((null == mulFile || mulFile.isEmpty()) && StringUtils.isEmpty(fileHash)) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        ElfinderStorage elfinderStorage = null;
        InputStream inputStream = null;
        if (StringUtils.isEmpty(fileHash)) {
            String originalFilename = mulFile.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.indexOf(".") + 1);
            if (!suffix.equals("xls")) {
                return ResultUtil.errorInternational("DS_SAMPLE_FILE");
            }
            long size = mulFile.getSize();
            if (size > 104857600) {
                return ResultUtil.errorInternational("DS_RESOURCE_LIMIT");
            }
            inputStream = mulFile.getInputStream();
        } else {
            elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
            Target target = elfinderStorage.fromHash(fileHash);
            String filePath = target.toString();
            File file = new File(filePath);
            if (!file.isFile()) {
                return ResultUtil.errorInternational("DS_SAMPLE_TYPE");
            }
            String suffix = file.getName().substring(file.getName().indexOf(".") + 1);
            if (!suffix.equals("xls")) {
                return ResultUtil.errorInternational("DS_SAMPLE_FILE");
            }
            long size = file.length();
            if (size > 104857600) {
                return ResultUtil.errorInternational("DS_RESOURCE_LIMIT");
            }
            inputStream = new FileInputStream(file);
        }
        HSSFWorkbook work = null;
        try {
            // Obtain this Excel table object
            work = new HSSFWorkbook(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.errorInternational("DS_SAMPLE_PARSING");
        }
        HSSFSheet sheetAt = work.getSheetAt(0);
        if (null == sheetAt) {
            return ResultUtil.errorInternational("DS_SAMPLE_PARSING");
        }
        HSSFRow logo = sheetAt.getRow(0);
        if (null == logo) {
            return ResultUtil.errorInternational("DS_SAMPLE_PARSING");
        }
        // Template processing parsing
        List<Map> modelData = getModelData(templateId, orgId, null);
        String iriTo = iri.substring(iri.lastIndexOf("/") + 1);
        List<Template.Children> gxbModel = getGXBModel(modelData, iriTo, new StringBuffer());
        if (gxbModel == null) {
            return ResultUtil.errorInternational("DS_TEMPLATE");
        }
        List<String> templeList = new ArrayList<>(16);
        Map<String, Template.Children> modelMap = new HashMap<>(16);
        Map<String, Map<String, Map<String, Object>>> urlDataMap = new HashMap<>(8);
        for (Template.Children children : gxbModel) {
            String name = children.getTitle();
            String type = children.getType();
            if (type.equals("selectGetUrl")) {
                String isAll = children.getIsAll();
                if (isAll.equals("false")) {
                    children.setUrl(children.getUrl() + "-dic.all");
                }
                Map<String, Map<String, Object>> urlData = getUrlData(children.getUrl());
                urlDataMap.put(name, urlData);
            }
            templeList.add(children.getTitle());
            modelMap.put(children.getTitle(), children);
        }
        modelData.clear();
        gxbModel.clear();
        // File parsing processing
        if (null == elfinderStorage) {
            elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
        }
        Volume volume = elfinderStorage.getVolumes().get(0);
        Map<String, String> fileMap = getWebFileName(elfinderStorage, Arrays.asList(fileList));
        List<String> nuptialList = new ArrayList<>(16);
        short lastCellNum = logo.getLastCellNum();
        List<String> logoList = new ArrayList<>(16);
        for (int x = 0; x < lastCellNum; x++) {
            // Process header
            String s = logo.getCell(x).toString();
            logoList.add(s);
            templeList.remove(s);
        }
        List<TemplateData> sampleDoList = new ArrayList<>(16);
        int lastRowNum = sheetAt.getLastRowNum();
        for (int i = 1; i <= lastRowNum; i++) {
            // Processing Line Data
            HSSFRow row = sheetAt.getRow(i);
            short lastNum = row.getLastCellNum();
            if (lastNum <= 0) {
                continue;
            }
            TemplateData sampleDo = new TemplateData(sampleId, templateId, orgId, iriTo, new Date());
            List<SampleCore> sampleCoreList = new ArrayList<>(16);
            String name = "";
            boolean type = true;
            StringBuffer annotation = new StringBuffer();
            for (int y = 0; y < lastCellNum; y++) {
                HSSFCell cell = row.getCell(y);
                String value = "";
                if (null != cell) {
                    value = cell.toString();
                }
                SampleCore sampleCore = new SampleCore();
                String log = logoList.get(y);
                if (modelMap.containsKey(log)) {
                    Template.Children children = modelMap.get(log);
                    sampleCore.setName(children.getName());
                    if (children.getName().equals("name")) {
                        name = value;
                    }
                    String multiply = children.getMultiply();
                    StringBuffer buffer = new StringBuffer();
                    boolean judge = ParameterValidation.paramCheck(multiply, value, buffer);
                    if (!judge) {
                        type = false;
                        sampleCore.setPerfect(false);
                        sampleCore.setAnnotation(buffer.toString());
                        annotation.append(log + " " + buffer.toString() + "；");
                    } else {
                        if (children.getType().equals("selectGetUrl")) {
                            Map<String, Map<String, Object>> stringMapMap = urlDataMap.get(log);
                            String trim = value.toLowerCase().trim();
                            if (null != stringMapMap && stringMapMap.containsKey(trim)) {
                                sampleCore.setValue(stringMapMap.get(trim));
                                sampleCore.setPerfect(true);
                            } else {
                                type = false;
                                sampleCore.setPerfect(false);
                                sampleCore.setAnnotation("未匹配到" + children.getTitle());
                                annotation.append("未匹配到" + children.getTitle() + "\t");
                            }
                        } else if (children.getType().equals("dataspace")) {
                            String[] split = value.split(";");
                            List<Map<String, Object>> dataList = new ArrayList<>(split.length);
                            StringBuffer buff1 = new StringBuffer();
                            buff1.append("未匹配到文件:");
                            StringBuffer buff2 = new StringBuffer();
                            buff2.append("文件冲突:");
                            boolean dataspace = true;
                            for (String fileName : split) {
                                if (fileMap.containsKey(fileName)) {
                                    Map<String, Object> map = new HashMap<>(3);
                                    if (nuptialList.contains(fileName)) {
                                        dataspace = false;
                                        buff2.append(fileName + ",");
                                    } else {
                                        String path = fileMap.get(fileName);
                                        String[] split1 = path.split("~");
                                        Target target = volume.fromPath(split1[0]);
                                        String hash = elfinderStorage.getHash(target);
                                        map.put("hash", hash);
                                        map.put("fileName", fileName);
                                        map.put("path", split1[0]);
                                        map.put("size", Long.valueOf(split1[1]));
                                        dataList.add(map);
                                        nuptialList.add(fileName);
                                    }
                                } else {
                                    dataspace = false;
                                    buff1.append(fileName + ",");
                                }
                            }
                            if (dataspace) {
                                sampleCore.setValue(dataList);
                                sampleCore.setPerfect(true);
                            } else {
                                type = false;
                                String annotations = "";
                                String string = buff1.toString();
                                if (!string.equals("未匹配到文件:")) {
                                    annotations += ((string.substring(0, string.length() - 1)) + "；");
                                }
                                String stringTo = buff2.toString();
                                if (!stringTo.equals("文件冲突:")) {
                                    annotations += ((stringTo.substring(0, stringTo.length() - 1)) + "；");
                                }
                                sampleCore.setAnnotation(annotations);
                                sampleCore.setPerfect(false);
                                annotation.append(annotations + "\t");
                            }
                        } else {
                            sampleCore.setPerfect(true);
                            sampleCore.setValue(value);
                        }
                    }
                    sampleCoreList.add(sampleCore);
                }
            }
            for (String temple : templeList) {
                SampleCore sampleCore = new SampleCore();
                Template.Children children = modelMap.get(temple);
                sampleCore.setName(children.getName());
                type = false;
                sampleCore.setPerfect(false);
                sampleCore.setAnnotation(temple + " 请完善该项信息");
                annotation.append(temple + " " + "请完善该项信息" + "；");
                sampleCoreList.add(sampleCore);
            }
            // Add sequencing sample data to
            if (!type) {
                sampleDo.setType(1);
            }
            sampleDo.setAnnotation(annotation.toString());
            sampleDo.setName(name);
            sampleDo.setSampleCoreList(sampleCoreList);
            sampleDoList.add(sampleDo);
        }
        urlDataMap.clear();
        if (!sampleDoList.isEmpty()) {
            mongoTemplate.insertAll(sampleDoList);
        }
        return ResultUtil.success(sampleId);
    }

    /**
     * URL data acquisition (temporarily supports get requests)
     */
    private Map<String, Map<String, Object>> getUrlData(String url) {
        if (StringUtils.isEmpty(url) || url.trim().equals("")) {
            return new HashMap<>(0);
        }
        HttpClient httpClient = new HttpClient();
        String result = null;
        try {
            result = httpClient.doGetWayTwo(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (StringUtils.isEmpty(result)) {
            return new HashMap<>(0);
        }
        Map dataMap = null;
        try {
            dataMap = JSONObject.parseObject(result, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>(0);
        }
        List data = (List) dataMap.get("data");
        Map<String, Map<String, Object>> resultMap = new HashMap<>(data.size());
        for (Object datum : data) {
            Map map = (Map) datum;
            String name = map.get("name").toString().trim();
            String nameEn = map.get("nameEn").toString().toLowerCase().trim();
            resultMap.put(name, map);
            resultMap.put(nameEn, map);
        }
        return resultMap;
    }

    public ResponseResult<Object> templateQuery(Integer page, Integer size, Integer type, String iri, String sampleId) {
        Map<String, Object> resultMap = new HashMap<>();
        Query query = new Query();
        if (StringUtils.isEmpty(sampleId)) {
            resultMap.put("count", 0);
            resultMap.put("data", null);
            return ResultUtil.success(resultMap);
        }
        Criteria criteria = Criteria.where("sampleId").is(sampleId);
        String iriTo = iri.substring(iri.lastIndexOf("/") + 1);
        criteria.and("iri").is(iriTo);
        if (type == 1) {
            criteria.and("type").is(type);
        }
        query.addCriteria(criteria);
        long count = mongoTemplate.count(query, TemplateData.class);
        List<TemplateData> sampleDos = null;
        if (count > 0) {
            query.with(Sort.by(Sort.Order.desc("createTime")));
            query.with(PageRequest.of(page - 1, size));
            sampleDos = mongoTemplate.find(query, TemplateData.class);
        }
        long perfect = 0l;
        long all = 0l;
        if (type == 0) {
            all = count;
            if (count > 0) {
                criteria.and("type").is(1);
                perfect = mongoTemplate.count(new Query().addCriteria(criteria), TemplateData.class);
            }
        } else if (type == 1) {
            perfect = count;
            all = mongoTemplate.count(new Query().addCriteria(Criteria.where("sampleId").is(sampleId).and("iri").is(iriTo)), TemplateData.class);
        }
        resultMap.put("perfect", perfect);
        resultMap.put("all", all);
        resultMap.put("data", sampleDos);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> operation(String sampleId, String type) {
        Query query = new Query().addCriteria(Criteria.where("sampleId").is(sampleId));
        long count = mongoTemplate.count(query, TemplateData.class);
        if (count > 0) {
            if (type.equals("confirm")) {
                List<TemplateData> templateData = mongoTemplate.find(query, TemplateData.class);
                Iterator<TemplateData> iterator = templateData.iterator();
                List<SampleDo> sampleDoList = new ArrayList<>(templateData.size());
                while (iterator.hasNext()) {
                    TemplateData next = iterator.next();
                    SampleDo sampleDo = new SampleDo();
                    CglibUtil.copy(next, sampleDo);
                    sampleDoList.add(sampleDo);
                }
                mongoTemplate.insertAll(sampleDoList);
                mongoTemplate.remove(query, TemplateData.class);
            } else {
                mongoTemplate.remove(query, TemplateData.class);
            }
        } else {
            return ResultUtil.errorInternational("");
        }
        return ResultUtil.success();
    }

    private Map<String, String> getWebFileName(ElfinderStorage elfinderStorage, List<String> requestFileList) {
        List<String> pathList = new ArrayList<>(requestFileList.size());
        for (String hash : requestFileList) {
            String path = elfinderStorage.fromHash(hash).toString();
            pathList.add(path);
        }
        String rootPath = elfinderStorage.getVolumes().get(0).getRoot().toString();
        Map<String, String> result = new HashMap<>(16);
        for (String path : pathList) {
            getFilePathMap(path, result, rootPath);
        }
        return result;
    }

    private void getFilePathMap(String path, Map<String, String> fileMap, String rootPath) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            String name = file.getName();
            String filePath = file.getPath();
            String replace = filePath.replace(rootPath, "");
            fileMap.put(name, replace + "~" + file.length());
        } else {
            File[] files = file.listFiles();
            for (File file1 : files) {
                getFilePathMap(file1.getPath(), fileMap, rootPath);
            }
        }
        return;
    }

    public ResponseResult<Object> sampleDel(String[] ids) {
        if (null != ids && ids.length > 0) {
            for (String id : ids) {
                Query query = new Query().addCriteria(Criteria.where("_id").is(id));
                SampleDo one = mongoTemplate.findOne(query, SampleDo.class);
                if (null != one) {
                    String ifPresent = publicModel.getIfPresent(one.getSampleId());
                    if (ifPresent != null) {
                        publicModel.put(one.getSampleId(), String.valueOf(Integer.valueOf(ifPresent) - 1));
                    }
                    mongoTemplate.remove(query, SampleDo.class);
                }
            }
        }
        return ResultUtil.success();
    }

    public ResponseResult<Object> check(String token, String resourceId, String resourceType, String spaceId, String title) {
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(title.trim())) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.P_ADD.getRole());
        Query query = new Query();
        query.addCriteria(Criteria.where("spaceId").is(spaceId));
        query.addCriteria(Criteria.where("titleCH").is(title));
        query.addCriteria(Criteria.where("resourceType").is(resourceType));
        if (StringUtils.isNotEmpty(resourceId)) {
            query.addCriteria(Criteria.where("resourceId").ne(resourceId));
        }
        Criteria criteria = new Criteria();
        criteria.orOperator(Criteria.where("type").is(Constants.PUBLISHED), Criteria.where("type").is(Constants.AUDIT));
        query.addCriteria(criteria);
        long count = mongoTemplate.count(query, ResourceV2.class);
        if (count > 0) {
            return ResultUtil.success(true);
        }
        return ResultUtil.success(false);
    }

    public Object doGet(String url) {
        if (!url.contains("http") && !url.contains(":") && !url.contains("//") && !url.contains(".")) {
            throw new CommonException(CommonUtils.messageInternational("HARVEST_TASK_URL"));
        }
        HttpClient httpClient = new HttpClient();
        try {
            String result = httpClient.doGetWayTwo(url);
            return JSONObject.parseObject(result, Map.class);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return null;
    }

    public Object doPost(DoPostInfo doPostInfo) {
        if (null == doPostInfo) {
            return null;
        }
        String url = doPostInfo.getUrl();
        if (!url.contains("http") && !url.contains(":") && !url.contains("//") && !url.contains(".")) {
            throw new CommonException(CommonUtils.messageInternational("HARVEST_TASK_URL"));
        }
        Object param = doPostInfo.getParam();
        HttpClient httpClient = new HttpClient();
        try {
            String result = httpClient.doPostJsonWayTwo(JSONObject.toJSONString(param), url);
            return JSONObject.parseObject(result, Map.class);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
