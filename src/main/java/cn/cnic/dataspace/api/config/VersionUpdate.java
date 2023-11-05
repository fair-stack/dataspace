package cn.cnic.dataspace.api.config;

import cn.cnic.dataspace.api.bigdataprocessing.SpatialLogRule;
import cn.cnic.dataspace.api.config.space.FileMappingManage;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.model.CacheData;
import cn.cnic.dataspace.api.model.harvest.MiningTask;
import cn.cnic.dataspace.api.model.harvest.TaskFileImp;
import cn.cnic.dataspace.api.model.manage.ApproveSetting;
import cn.cnic.dataspace.api.model.manage.SystemConf;
import cn.cnic.dataspace.api.model.space.*;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.email.SysEmail;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.open.OpenApi;
import cn.cnic.dataspace.api.model.release.ResourceDo;
import cn.cnic.dataspace.api.model.release.ResourceV2;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.statistics.SpaceTypeStatistic;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.queue.SpaceQuery;
import cn.cnic.dataspace.api.queue.space.SpaceTaskUtils;
import cn.cnic.dataspace.api.util.*;
import cn.cnic.dataspace.api.websocket.WebSocketProcess;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.*;

@Repository
@Slf4j
public class VersionUpdate {

    @Resource
    private MongoTemplate mongoTemplate;

    private final Cache<String, Object> config = CaffeineUtil.getConfig();

    private final Cache<String, String> spaceShortCache = CaffeineUtil.getSpaceShort();

    @Autowired
    private FileMappingManage fileMappingManage;

    @Resource
    private SpatialLogRule spatialLogRule;

    @Autowired
    private WebSocketProcess webSocketProcess;

    /**
     * Start
     */
    public void init(SpaceUrl spaceUrl, String lodVersion, SpaceControlConfig spaceControlConfig) {
        log.info("------- 对老版本数据进行优化调整 此过程需要时间 请耐心等待 ------");
        fileStorageDirectoryChanged(spaceUrl, spaceControlConfig);
        // Creation of previous version space without statistical information
        spaceStatisticsCreation();
        // Calculate space files
        updateSpaceFile();
        spaceTypeCreation();
        systemCapacity(spaceUrl);
        // Update email
        emailUpdate();
        // Picture Adjustment
        imageConversion(spaceUrl.getRootDir(), lodVersion);
        // Initialize open_ API List
        initOpenApi();
        spaceLogDeal(lodVersion);
        initializeFileMapping(lodVersion);
        // Initialize Space Permissions Menu
        initSpaceMenuList(lodVersion);
        // Space log processing (large data volume)
        spaceLogProcessing(lodVersion);
        // Restart interrupted space import task
        startInterruptTask();
        // Consolidate file metadata
        integrationFileData();
        log.info("------- 更新结束 ------");
    }

    /**
     * Creation of previous version space without statistical information
     */
    private void spaceStatisticsCreation() {
        List<Space> all = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").ne("0")), Space.class);
        for (Space space : all) {
            Query query = new Query().addCriteria(Criteria.where("spaceId").is(space.getSpaceId()));
            SpaceDataStatistic count = mongoTemplate.findOne(query, SpaceDataStatistic.class);
            if (null == count) {
                SpaceDataStatistic spaceDataStatistic = new SpaceDataStatistic();
                spaceDataStatistic.setSpaceId(space.getSpaceId());
                spaceDataStatistic.setSpaceName(space.getSpaceName());
                spaceDataStatistic.setCapacity(space.getSpaceSize());
                spaceDataStatistic.setState(Integer.valueOf(space.getState()));
                spaceDataStatistic.setPersonNum(space.getAuthorizationList().size());
                Date createTime = null;
                try {
                    createTime = CommonUtils.getStringToDateTime(space.getCreateDateTime());
                } catch (ParseException e) {
                    createTime = new Date();
                }
                spaceDataStatistic.setCreateTime(createTime);
                mongoTemplate.insert(spaceDataStatistic);
            } else {
                if (count.getState() != Integer.valueOf(space.getState())) {
                    count.setState(Integer.valueOf(space.getState()));
                    mongoTemplate.save(count);
                }
            }
            if (StringUtils.isEmpty(space.getHomeUrl()) || StringUtils.isEmpty(space.getHomeUrl().trim())) {
                Update update = new Update();
                update.set("homeUrl", space.getSpaceId());
                mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(space.getSpaceId())), update, Space.class);
            }
        }
    }

    /**
     * Space type statistics, old version data statistics
     */
    private void spaceTypeCreation() {
        long count = mongoTemplate.count(new Query(), SpaceTypeStatistic.class);
        if (count == 0) {
            List<Space> all = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").ne("0")), Space.class);
            for (Space space : all) {
                setSpaceType(space);
            }
        }
    }

    private Calendar getCalendar(String createDateTime) {
        Date stringToDateTime = null;
        try {
            stringToDateTime = CommonUtils.getStringToDateTime(createDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
            stringToDateTime = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(stringToDateTime);
        return calendar;
    }

    public void setSpaceType(Space space) {
        Calendar calendar = getCalendar(space.getCreateDateTime());
        // Obtain the current year
        int year = calendar.get(Calendar.YEAR);
        // Get Current Month
        int month = calendar.get(Calendar.MONTH) + 1;
        Query query = new Query();
        query.addCriteria(Criteria.where("year").is(year).and("month").is(month));
        Update update = new Update();
        Integer isPublic = space.getIsPublic();
        if (null == isPublic || isPublic == 0) {
            int applyIs = space.getApplyIs();
            if (applyIs == 1) {
                update.inc("lim", 1L);
                update.setOnInsert("pri", 0L);
                update.setOnInsert("pub", 0L);
            } else {
                update.inc("pri", 1L);
                update.setOnInsert("pub", 0L);
                update.setOnInsert("lim", 0L);
            }
        } else if (isPublic == 1) {
            update.inc("pub", 1L);
            update.setOnInsert("pri", 0L);
            update.setOnInsert("lim", 0L);
        } else {
            return;
        }
        update.setOnInsert("year", year);
        update.setOnInsert("month", month);
        String s = ("" + month).length() == 1 ? "0" + month : "" + month;
        update.setOnInsert("sort", Integer.valueOf(year + s));
        update.setOnInsert("createTime", calendar.getTime());
        if (null != update) {
            mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceTypeStatistic.class);
        }
    }

    /**
     * Calculate system capacity
     */
    private void systemCapacity(SpaceUrl spaceUrl) {
        List<ApproveSetting> approveSettings = mongoTemplate.find(new Query(), ApproveSetting.class);
        ApproveSetting approveSetting = approveSettings.get(0);
        Long storage = approveSetting.getStorage();
        if (null == storage || storage == 0) {
            FileUtils fileUtils = new FileUtils();
            String rootDir = spaceUrl.getRootDir();
            long spaceSize = fileUtils.getSpaceSize(rootDir);
            log.info("spaceSize: " + spaceSize);
            approveSetting.setStorage(spaceSize);
            mongoTemplate.save(approveSetting);
        }
    }

    // Email update
    private void emailUpdate() {
        Query emailQuery = new Query().addCriteria(Criteria.where("type").is(Constants.CaffeType.SYS_EMAIL));
        SystemConf systemConf = mongoTemplate.findOne(emailQuery, SystemConf.class);
        if (null != systemConf) {
            SysEmail conf = JSONObject.parseObject(JSONObject.toJSONString(systemConf.getConf()), SysEmail.class);
            if (conf.getUsername().equals("") && conf.getUpload() == 1) {
                conf.setPassword(RSAEncrypt.encrypt(""));
                conf.setUpload(2);
                systemConf.setConf(conf);
                mongoTemplate.save(systemConf);
                config.put(Constants.CaffeType.SYS_EMAIL, conf);
            }
        }
    }

    private void updateSpaceFile() {
        log.info("------- 计算空间文件和空间数据量计算 ------");
        List<Space> all = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").ne("0")), Space.class);
        long total = 0L;
        for (Space space : all) {
            Query query = new Query().addCriteria(Criteria.where("spaceId").is(space.getSpaceId()));
            SpaceDataStatistic spaceDataStatistic = mongoTemplate.findOne(query, SpaceDataStatistic.class);
            if (spaceDataStatistic.getDataSize() == 0) {
                String filePath = space.getFilePath();
                String filesCount = FileUtils.getFilesCount(new File(filePath));
                String[] split = filesCount.split(":");
                int count = Integer.valueOf(split[0]);
                long data = Long.valueOf(split[1]);
                boolean judge = false;
                if (spaceDataStatistic.getFileNum() != count) {
                    log.info("系统启动-空间：" + space.getSpaceName() + " 文件数量有变动 由 " + spaceDataStatistic.getFileNum() + " 变为 " + count);
                    spaceDataStatistic.setFileNum(count);
                    judge = true;
                }
                if (data != spaceDataStatistic.getDataSize()) {
                    log.info("系统启动-空间：" + space.getSpaceName() + " 实体文件大小有变动 由 " + spaceDataStatistic.getDataSize() + " 变为 " + data);
                    spaceDataStatistic.setDataSize(data);
                    judge = true;
                }
                if (judge) {
                    mongoTemplate.save(spaceDataStatistic);
                }
            }
            SpaceSizeControl.addSpace(space.getSpaceId(), spaceDataStatistic.getCapacity(), spaceDataStatistic.getDataSize());
            total += spaceDataStatistic.getDataSize();
        }
        CacheData cacheData = mongoTemplate.findOne(new Query(), CacheData.class);
        cacheData.setFileSize(FileUtils.formFileSize(total));
        cacheData.setFileSizeLong(total);
        // Total system capacity storage
        mongoTemplate.save(cacheData);
    }

    /**
     * Image base64 replacement space, user, and publishing impact:<1.2.0
     */
    private void imageConversion(String rootPath, String lodVersion) {
        if (judgeVersion(lodVersion, "120")) {
            return;
        }
        // space
        List<Space> all = mongoTemplate.findAll(Space.class);
        for (Space space : all) {
            String spaceLogo = space.getSpaceLogo();
            if (StringUtils.isNotEmpty(spaceLogo) && StringUtils.isNotEmpty(spaceLogo.trim())) {
                if (CommonUtils.isPicBase(spaceLogo)) {
                    String imagePath = "/" + Constants.Image.image + "/" + Constants.Image.SPACE + "/" + space.getSpaceId() + ".jpg";
                    try {
                        CommonUtils.generateImage(spaceLogo, rootPath + imagePath);
                        Update update = new Update();
                        update.set("spaceLogo", imagePath);
                        mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(space.getSpaceId())), update, Space.class);
                    } catch (Exception e) {
                        log.info("--- 空间图片替换失败: {} spaceId: " + space.getSpaceId());
                    }
                }
            }
        }
        // user
        List<ConsumerDO> consumerDOS = mongoTemplate.findAll(ConsumerDO.class);
        for (ConsumerDO consumerDO : consumerDOS) {
            String avatar = consumerDO.getAvatar();
            if (StringUtils.isNotEmpty(avatar) && StringUtils.isNotEmpty(avatar.trim())) {
                if (CommonUtils.isPicBase(avatar)) {
                    String imagePath = "/" + Constants.Image.image + "/" + Constants.Image.USER + "/" + consumerDO.getId() + ".jpg";
                    try {
                        CommonUtils.generateImage(avatar, rootPath + imagePath);
                        Update update = new Update();
                        update.set("avatar", imagePath);
                        mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(consumerDO.getId())), update, ConsumerDO.class);
                    } catch (Exception e) {
                        log.info("--- 用户图片替换失败: {} userId: " + consumerDO.getId());
                    }
                }
            }
        }
        // release
        List<ResourceV2> resourceV2List = mongoTemplate.findAll(ResourceV2.class);
        for (ResourceV2 resourceV2 : resourceV2List) {
            String image = resourceV2.getImage();
            if (StringUtils.isNotEmpty(image) && StringUtils.isNotEmpty(image.trim())) {
                if (CommonUtils.isPicBase(image)) {
                    String imagePath = "/" + Constants.Image.image + "/" + Constants.Image.RESOURCE + "/" + resourceV2.getResourceId() + resourceV2.getVersion() + ".jpg";
                    try {
                        CommonUtils.generateImage(image, rootPath + imagePath);
                        List<ResourceDo> metadata = resourceV2.getMetadata();
                        metadata.stream().forEachOrdered(resourceDo -> {
                            // *****************
                            String iri = resourceDo.getIri();
                            String key = iri.substring(iri.lastIndexOf("/") + 1);
                            if (key.equals("image")) {
                                resourceDo.setValue(new ArrayList<String>(1) {

                                    {
                                        add(imagePath);
                                    }
                                });
                            }
                        });
                        resourceV2.setImage(imagePath);
                        resourceV2.setMetadata(metadata);
                        mongoTemplate.save(resourceV2);
                        // mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(resourceV2.getId())), update, ResourceV2.class);
                    } catch (Exception e) {
                        log.info("--- 发布图片替换失败: {} resourceId(id): " + resourceV2.getId());
                    }
                }
            }
        }
    }

    /**
     * Initialize open API list
     */
    private void initOpenApi() {
        List<OpenApi> openApiList = mongoTemplate.find(new Query(), OpenApi.class);
        String open_api = CommonUtils.readJsonFile(CommonUtils.getResourceFile("/data/open_api.txt").getPath());
        String[] apis = (open_api.replaceAll("\r", "").replaceAll("\n", "")).split(",");
        Map<String, OpenApi> dataMap = new HashMap<>(apis.length);
        for (String api : apis) {
            String[] split = api.split("&");
            OpenApi openApi = new OpenApi();
            openApi.setName(split[0]);
            openApi.setMethod(split[1]);
            openApi.setPath(split[2]);
            openApi.setVersion(split[3]);
            openApi.setDesc(split[4]);
            openApi.setState(Constants.OpenApiState.online);
            try {
                openApi.setPublicTime(CommonUtils.getStringToDateTime(split[5]));
            } catch (ParseException e) {
                e.printStackTrace();
                openApi.setPublicTime(new Date());
            }
            openApi.setCreateTime(new Date());
            dataMap.put(split[0], openApi);
        }
        for (OpenApi openApi : openApiList) {
            OpenApi openApiNew = dataMap.get(openApi.getName());
            if (null == openApiNew) {
                // Configuration file not found, delete it
                continue;
            }
            boolean judge = false;
            if (!openApi.getMethod().equals(openApiNew.getMethod())) {
                openApi.setMethod(openApiNew.getMethod());
                judge = true;
            }
            if (!openApi.getPath().equals(openApiNew.getPath())) {
                openApi.setPath(openApiNew.getPath());
                judge = true;
            }
            if (!openApi.getVersion().equals(openApiNew.getVersion())) {
                openApi.setVersion(openApiNew.getVersion());
                judge = true;
            }
            if (!openApi.getDesc().equals(openApiNew.getDesc())) {
                openApi.setDesc(openApiNew.getDesc());
                judge = true;
            }
            if (!openApi.getPublicTime().equals(openApiNew.getPublicTime())) {
                openApi.setPublicTime(openApiNew.getPublicTime());
                judge = true;
            }
            if (judge) {
                mongoTemplate.save(openApi);
            }
            dataMap.remove(openApi.getName());
        }
        if (dataMap.size() > 0) {
            for (String key : dataMap.keySet()) {
                mongoTemplate.insert(dataMap.get(key));
            }
        }
    }

    /**
     * Space bottom storage modification
     */
    private void fileStorageDirectoryChanged(SpaceUrl spaceUrl, SpaceControlConfig spaceControlConfig) {
        List<Space> spaceList = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").ne("0")), Space.class);
        log.info("------- 对老版本储存目录更替 ------");
        for (Space space : spaceList) {
            String spaceShort = space.getSpaceShort();
            if (null == spaceShort || spaceShort.equals("")) {
                String shortChain = spaceControlConfig.getSpaceShort();
                space.setSpaceShort(shortChain);
                String filePath = space.getFilePath();
                try {
                    File file = new File(filePath);
                    if (file.exists()) {
                        // Space file movement
                        Files.move(new File(filePath).toPath(), new File(spaceUrl.getRootDir(), shortChain).toPath());
                    }
                    Update update = new Update();
                    update.set("filePath", spaceUrl.getRootDir() + "/" + shortChain);
                    update.set("spaceShort", shortChain);
                    mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(space.getSpaceId())), update, Space.class);
                } catch (IOException ioException) {
                    log.info("空间：" + space.getSpaceName() + " 空间路径：" + filePath + " 移动失败 ————————");
                }
            }
            if (space.getState().equals("1")) {
                spaceShortCache.put(space.getSpaceShort(), space.getSpaceId());
            }
        }
        log.info("------- 对老版本储存目录更替完成 ------");
    }

    /**
     * Space log file processing
     */
    private void spaceLogDeal(String lodVersion) {
        if (judgeVersion(lodVersion, "121")) {
            return;
        }
        log.info("------- 对1.2.1之前版本的空间日志做处理 ------");
        List<Space> spaceList = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").ne("0")), Space.class);
        for (Space space : spaceList) {
            Query query = new Query().addCriteria(Criteria.where("spaceId").is(space.getSpaceId()));
            List<SpaceSvnLog> spaceSvnLogs = mongoTemplate.find(query, SpaceSvnLog.class);
            Iterator<SpaceSvnLog> iterator = spaceSvnLogs.iterator();
            while (iterator.hasNext()) {
                SpaceSvnLog next = iterator.next();
                if (null == next.getCreateTime()) {
                    Update update = new Update();
                    try {
                        update.set("createTime", CommonUtils.getStringToDateTime(next.getUpdateDateTime()));
                    } catch (ParseException e) {
                        update.set("createTime", new Date());
                    }
                    mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(next.getSpaceSvnId())), update, SpaceSvnLog.class);
                }
            }
        }
    }

    /**
     * The impact of adding database mapping to the storage of old version files:<=1.2.0
     */
    private void initializeFileMapping(String lodVersion) {
        if (judgeVersion(lodVersion, "121")) {
            return;
        }
        log.info("------- 对1.2.1之前版本的文件加入数据库映射 ------");
        // File Storage Mapping
        List<Space> spaceList = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").ne("0")), Space.class);
        log.info("开始处理空间 >>>>>>>>>>>>");
        for (Space space : spaceList) {
            String filePath = space.getFilePath();
            File file = new File(filePath);
            File[] files = file.listFiles();
            if (!file.exists() || files.length == 0) {
                log.info("空间无文件 >>>>>>>>>>>>");
                continue;
            }
            long count = mongoTemplate.count(new Query(), FileMapping.class, space.getSpaceId());
            if (count == 0) {
                Query query = new Query().addCriteria(Criteria.where("spaceId").is(space.getSpaceId()));
                SpaceDataStatistic spaceDataStatistic = mongoTemplate.findOne(query, SpaceDataStatistic.class);
                log.info("开始处理空间: " + spaceDataStatistic.getSpaceName() + " ( " + spaceDataStatistic.getSpaceId() + " ) 数据量：" + FileUtils.formFileSize(spaceDataStatistic.getDataSize()) + " 文件数量：" + spaceDataStatistic.getFileNum());
                String spaceId = space.getSpaceId();
                Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
                Operator operator = new Operator();
                for (AuthorizationPerson person : authorizationList) {
                    operator.setPersonId(person.getUserId());
                    operator.setPersonName(person.getUserName());
                    operator.setEmail(person.getEmail());
                    operator.setAvatar(person.getAvatar());
                    break;
                }
                try {
                    List<FileMapping> spaceFileMappingList = fileMappingManage.getSpaceFileMappingList(spaceId, files, operator);
                    log.info("映射处理结束 开始保存数据库 >>>>>>>>>>>>>>>");
                    mongoTemplate.insert(spaceFileMappingList, spaceId);
                    spaceFileMappingList.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("空间：" + space.getSpaceName() + "_" + space.getSpaceId() + " 文件映射处理失败--------------------");
                }
            } else {
                log.info("空间数据库文件映射已存在 >>>>>>>>>>>>");
            }
        }
    }

    /**
     * Initialize Space Permissions Menu
     */
    private void initSpaceMenuList(String lodVersion) {
        long count = mongoTemplate.count(new Query(), SpaceMenu.class);
        if (count == 0) {
            // Level 1 file
            SpaceMenu file_menu = new SpaceMenu();
            file_menu.setCla("文件");
            file_menu.setCreateTime(new Date());
            // Add
            SpaceMenu.Action make_action = new SpaceMenu.Action(SpaceRoleEnum.F_MAKE);
            List<SpaceMenu.Role> make_roleList = new ArrayList<>(1);
            make_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_MAKE));
            make_action.setRoleList(make_roleList);
            // check
            // SpaceMenu.Action cmd_action = new SpaceMenu.Action(SpaceRoleEnum.F_CMD);
            // List<SpaceMenu.Role> cmd_roleList = new ArrayList<>(1);
            // cmd_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_CMD));
            // cmd_action.setRoleList(cmd_roleList);
            // edit
            SpaceMenu.Action edit_action = new SpaceMenu.Action(SpaceRoleEnum.F_EDIT_AM);
            List<SpaceMenu.Role> edit_roleList = new ArrayList<>(2);
            edit_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_EDIT_AM));
            edit_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_EDIT_OT));
            edit_action.setRoleList(edit_roleList);
            // download
            SpaceMenu.Action down_action = new SpaceMenu.Action(SpaceRoleEnum.F_DOWN_AM);
            List<SpaceMenu.Role> down_roleList = new ArrayList<>(2);
            down_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_DOWN_AM));
            down_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_DOWN_OT));
            down_action.setRoleList(down_roleList);
            // delete
            SpaceMenu.Action del_action = new SpaceMenu.Action(SpaceRoleEnum.F_DEL_AM);
            List<SpaceMenu.Role> del_roleList = new ArrayList<>(2);
            del_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_DEL_AM));
            del_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_DEL_OT));
            del_action.setRoleList(del_roleList);
            // share
            SpaceMenu.Action shar_action = new SpaceMenu.Action(SpaceRoleEnum.F_SHAR_SPACE);
            List<SpaceMenu.Role> shar_roleList = new ArrayList<>(3);
            shar_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_SHAR_SPACE));
            shar_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_SHAR_FILE_AM));
            shar_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_SHAR_FILE_OT));
            shar_action.setRoleList(shar_roleList);
            // other
            SpaceMenu.Action other_action = new SpaceMenu.Action(SpaceRoleEnum.F_OTHER_IM);
            List<SpaceMenu.Role> other_roleList = new ArrayList<>(6);
            other_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_OTHER_IM));
            other_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_OTHER_BAIDU));
            other_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_OTHER_FTP));
            other_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_OTHER_WEBDAV));
            // other_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_OTHER_SAFE));
            // other_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.F_OTHER_RS));
            other_action.setRoleList(other_roleList);
            List<SpaceMenu.Action> file_List = new ArrayList<>(6);
            file_List.add(make_action);
            // file_List.add(cmd_action);
            file_List.add(edit_action);
            file_List.add(down_action);
            file_List.add(del_action);
            file_List.add(shar_action);
            file_List.add(other_action);
            file_menu.setActionList(file_List);
            // First level table
            SpaceMenu t_menu = new SpaceMenu();
            t_menu.setCla("表格");
            t_menu.setCreateTime(new Date());
            // Add
            SpaceMenu.Action t_create_action = new SpaceMenu.Action(SpaceRoleEnum.T_CREATE);
            List<SpaceMenu.Role> t_create_roleList = new ArrayList<>(1);
            t_create_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.T_CREATE));
            t_create_action.setRoleList(t_create_roleList);
            // edit
            SpaceMenu.Action t_edit_action = new SpaceMenu.Action(SpaceRoleEnum.T_EDIT);
            List<SpaceMenu.Role> t_edit_roleList = new ArrayList<>(1);
            t_edit_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.T_EDIT));
            t_edit_action.setRoleList(t_edit_roleList);
            // delete
            SpaceMenu.Action t_del_action = new SpaceMenu.Action(SpaceRoleEnum.T_DELETE);
            List<SpaceMenu.Role> t_del_roleList = new ArrayList<>(1);
            t_del_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.T_DELETE));
            t_del_action.setRoleList(t_del_roleList);
            List<SpaceMenu.Action> t_List = new ArrayList<>(3);
            t_List.add(t_create_action);
            t_List.add(t_edit_action);
            t_List.add(t_del_action);
            t_menu.setActionList(t_List);
            // First level member management
            SpaceMenu m_menu = new SpaceMenu();
            m_menu.setCla("成员管理");
            m_menu.setCreateTime(new Date());
            // Add
            SpaceMenu.Action m_add_action = new SpaceMenu.Action(SpaceRoleEnum.M_ADD);
            List<SpaceMenu.Role> m_add_roleList = new ArrayList<>(1);
            m_add_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.M_ADD));
            m_add_action.setRoleList(m_add_roleList);
            // edit
            SpaceMenu.Action m_edit_action = new SpaceMenu.Action(SpaceRoleEnum.M_EDIT);
            List<SpaceMenu.Role> m_edit_roleList = new ArrayList<>(1);
            m_edit_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.M_EDIT));
            m_edit_action.setRoleList(m_edit_roleList);
            // delete
            SpaceMenu.Action m_del_action = new SpaceMenu.Action(SpaceRoleEnum.M_DEL);
            List<SpaceMenu.Role> m_del_roleList = new ArrayList<>(1);
            m_del_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.M_DEL));
            m_del_action.setRoleList(m_del_roleList);
            List<SpaceMenu.Action> m_List = new ArrayList<>(3);
            m_List.add(m_add_action);
            m_List.add(m_edit_action);
            m_List.add(m_del_action);
            m_menu.setActionList(m_List);
            // First level release
            SpaceMenu p_menu = new SpaceMenu();
            p_menu.setCla("发布");
            p_menu.setCreateTime(new Date());
            // Add
            SpaceMenu.Action p_add_action = new SpaceMenu.Action(SpaceRoleEnum.P_ADD);
            List<SpaceMenu.Role> p_add_roleList = new ArrayList<>(1);
            p_add_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.P_ADD));
            p_add_action.setRoleList(p_add_roleList);
            // check
            // SpaceMenu.Action p_list_action = new SpaceMenu.Action(SpaceRoleEnum.P_LIST);
            // List<SpaceMenu.Role> p_list_roleList = new ArrayList<>(1);
            // p_list_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.P_LIST));
            // p_list_action.setRoleList(p_list_roleList);
            List<SpaceMenu.Action> p_List = new ArrayList<>(2);
            p_List.add(p_add_action);
            // p_List.add(p_list_action);
            p_menu.setActionList(p_List);
            // Primary space configuration
            SpaceMenu s_menu = new SpaceMenu();
            s_menu.setCla("空间配置");
            s_menu.setCreateTime(new Date());
            // allocation
            SpaceMenu.Action s_c_action = new SpaceMenu.Action(SpaceRoleEnum.S_CONF_INFO);
            List<SpaceMenu.Role> s_c_roleList = new ArrayList<>(3);
            s_c_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.S_CONF_INFO));
            s_c_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.S_CONF_BAK));
            s_c_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.S_CONF_PER));
            s_c_action.setRoleList(s_c_roleList);
            // Auditing
            SpaceMenu.Action s_a_action = new SpaceMenu.Action(SpaceRoleEnum.S_AUDIT);
            List<SpaceMenu.Role> s_a_roleList = new ArrayList<>(1);
            s_a_roleList.add(new SpaceMenu.Role(SpaceRoleEnum.S_AUDIT));
            s_a_action.setRoleList(s_a_roleList);
            List<SpaceMenu.Action> s_List = new ArrayList<>(2);
            s_List.add(s_c_action);
            s_List.add(s_a_action);
            s_menu.setActionList(s_List);
            mongoTemplate.insert(file_menu);
            mongoTemplate.insert(t_menu);
            mongoTemplate.insert(m_menu);
            mongoTemplate.insert(p_menu);
            mongoTemplate.insert(s_menu);
        }
        // Add default space permissions to default values
        SystemConf systemConf = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("type").is(Constants.CaffeType.SYS_DEF_SPACE_ROLE)), SystemConf.class);
        if (null == systemConf) {
            Map<String, Object> setMap = new HashMap<>(2);
            String[] general = { "F_MAKE", "F_EDIT_AM", "F_DOWN_AM", "F_DEL_AM", "F_OTHER_IM", "F_OTHER_BAIDU", "F_OTHER_FTP", "F_OTHER_WEBDAV", "F_SHAR_FILE_AM", "T_CREATE", "T_EDIT", "T_DELETE" };
            String[] senior = { "F_MAKE", "F_EDIT_AM", "F_EDIT_OT", "F_DOWN_AM", "F_DOWN_OT", "F_DEL_AM", "F_SHAR_SPACE", "F_SHAR_FILE_AM", "F_SHAR_FILE_OT", "F_DEL_OT", "F_OTHER_IM", "F_OTHER_BAIDU", "F_OTHER_FTP", "F_OTHER_WEBDAV", "T_CREATE", "T_EDIT", "T_DELETE", "M_ADD", "M_EDIT", "M_DEL", "P_ADD" };
            setMap.put("普通", general);
            setMap.put("高级", senior);
            systemConf = new SystemConf();
            systemConf.setType(Constants.CaffeType.SYS_DEF_SPACE_ROLE);
            systemConf.setCreateTime(new Date());
            systemConf.setLastUpdateTime(new Date());
            systemConf.setConf(setMap);
            mongoTemplate.insert(systemConf);
        }
        // Add default permissions for spaces before version 1.2.1
        if (judgeVersion(lodVersion, "121")) {
            return;
        }
        Object conf = systemConf.getConf();
        Map<String, List<String>> spaceRole = JSONObject.parseObject(JSONObject.toJSONString(conf), Map.class);
        List<Space> spaceList = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").ne("0")), Space.class);
        List<SpaceRole> spaceRoles = new ArrayList<>(spaceList.size() * 2);
        for (Space space : spaceList) {
            long roleCount = mongoTemplate.count(new Query().addCriteria(Criteria.where("spaceId").is(space.getSpaceId())), SpaceRole.class);
            if (roleCount == 0) {
                for (String key : spaceRole.keySet()) {
                    SpaceRole spaceRole1 = new SpaceRole();
                    spaceRole1.setSpaceId(space.getSpaceId());
                    spaceRole1.setRoleName(key);
                    spaceRole1.setMenus(spaceRole.get(key));
                    spaceRole1.setCreateTime(new Date());
                    spaceRole1.setUpdateTime(new Date());
                    spaceRoles.add(spaceRole1);
                }
            }
        }
        mongoTemplate.insertAll(spaceRoles);
    }

    /**
     * Processing logs from a month ago
     */
    private void spaceLogProcessing(String lodVersion) {
        if (judgeVersion(lodVersion, "121")) {
            return;
        }
        spatialLogRule.execute();
    }

    /**
     * Restart interrupted space import task
     */
    private void startInterruptTask() {
        log.info(">>>>>>>>>>>>>>> 恢复中断的空间导入任务 <<<<<<<<<<<<<<<<<<");
        // Clean up completed data records
        Integer[] states = { 0, 1, 5 };
        // Completed Tasks
        Query query = new Query().addCriteria(Criteria.where("state").in(states));
        List<MiningTask> miningTasks = mongoTemplate.find(query, MiningTask.class);
        if (miningTasks.isEmpty()) {
            log.info(">>>>>>>>>>>>>>> 无中断的空间导入任务 <<<<<<<<<<<<<<<<<<");
            return;
        }
        for (MiningTask miningTask : miningTasks) {
            String taskId = miningTask.getTaskId();
            String userId = miningTask.getUserId();
            Integer[] st = { 0, 1 };
            Query taskQuery = new Query().addCriteria(Criteria.where("rootId").is(taskId).and("state").in(st));
            taskQuery.with(Sort.by(Sort.Direction.ASC, "sort"));
            List<TaskFileImp> taskFileImpList = mongoTemplate.find(taskQuery, TaskFileImp.class);
            if (taskFileImpList.isEmpty()) {
                // Modify the main task status
                Query countQuery = new Query().addCriteria(Criteria.where("rootId").is(taskId).and("state").is(3));
                long count = mongoTemplate.count(countQuery, TaskFileImp.class);
                if (count > 0) {
                    miningTask.setState(4);
                } else {
                    miningTask.setState(2);
                }
                mongoTemplate.save(miningTask);
                continue;
            }
            miningTask.setState(5);
            mongoTemplate.save(miningTask);
            socketMessage(Constants.SocketType.TS_DRAW, miningTask.getTaskId(), miningTask.getSpaceName(), miningTask.getEmail());
            // Join the task queue to start processing
            SpaceQuery instance = SpaceQuery.getInstance();
            SpaceTaskUtils spaceTaskUtils = new SpaceTaskUtils(taskFileImpList, webSocketProcess);
            instance.addCache(userId, miningTask, mongoTemplate, spaceTaskUtils);
            log.info("空间（" + miningTask.getSpaceName() + "） 空间导入任务恢复成功：" + miningTask.getTaskId());
        }
    }

    private void socketMessage(String type, String taskId, String taskName, String email) {
        Map<String, Object> messageMap = new HashMap<>(4);
        messageMap.put("type", type);
        messageMap.put("taskId", taskId);
        messageMap.put("taskName", taskName);
        messageMap.put("mark", "space");
        try {
            webSocketProcess.sendMessage(email, JSONObject.toJSONString(messageMap));
        } catch (Exception e) {
            log.error("空间汇交与收割任务消息通知发送失败: {} " + e.getMessage());
        }
    }

    private boolean judgeVersion(String lodVersion, String version) {
        int ver = Integer.valueOf(version);
        int lodVer = 0;
        if (StringUtils.isNotEmpty(lodVersion)) {
            String substring = lodVersion.substring(1);
            lodVer = Integer.valueOf(substring.replaceAll("\\.", ""));
        }
        return lodVer >= ver;
    }

    /**
     * Consolidate file metadata
     */
    private void integrationFileData() {
        List<FileData> fileDataList = mongoTemplate.findAll(FileData.class, "file_data");
        if (!fileDataList.isEmpty()) {
            Iterator<FileData> iterator = fileDataList.iterator();
            while (iterator.hasNext()) {
                FileData next = iterator.next();
                List<FileMapping.Data> data = next.getData();
                String hash = next.getHash();
                String spaceId = next.getSpaceId();
                Query query = new Query().addCriteria(Criteria.where("hash").is(hash));
                FileMapping fileMapping = mongoTemplate.findOne(query, FileMapping.class, spaceId);
                if (null != fileMapping) {
                    if (fileMapping.getData() == null) {
                        Update update = new Update();
                        update.set("data", data);
                        mongoTemplate.upsert(query, update, spaceId);
                    }
                }
            }
        }
        mongoTemplate.remove(new Query(), "file_data");
    }
}
