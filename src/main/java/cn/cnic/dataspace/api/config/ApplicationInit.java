package cn.cnic.dataspace.api.config;

import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.datax.admin.config.UpdateStatusTask;
import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import cn.cnic.dataspace.api.datax.admin.upgrade.DSManager;
import cn.cnic.dataspace.api.ftp.authen.CustomServer;
import cn.cnic.dataspace.api.ftp.authen.UserbaseAuthenticator;
import cn.cnic.dataspace.api.ftp.listener.SpaceFileListener;
import cn.cnic.dataspace.api.ftp.minimalftp.FTPServer;
import cn.cnic.dataspace.api.model.CacheData;
import cn.cnic.dataspace.api.model.manage.ApproveSetting;
import cn.cnic.dataspace.api.model.manage.BasicSetting;
import cn.cnic.dataspace.api.model.manage.SystemConf;
import cn.cnic.dataspace.api.model.backup.BackupSpaceMain;
import cn.cnic.dataspace.api.model.space.*;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.email.SysEmail;
import cn.cnic.dataspace.api.model.release.Subject;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.statistics.SpaceStatistic;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.model.user.RoleDO;
import cn.cnic.dataspace.api.quartz.MyJob;
import cn.cnic.dataspace.api.quartz.QuartzManager;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.util.*;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.File;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import static cn.cnic.dataspace.api.model.manage.ApproveSetting.NEED_APPROVED;
import static cn.cnic.dataspace.api.util.CommonUtils.FILE_SPLIT;
import static cn.cnic.dataspace.api.util.CommonUtils.generateSnowflake;
import static cn.cnic.dataspace.api.util.Constants.SYSTEM_STARTUP;

/**
 * admin user check
 *
 * @date 2020-02-21 14:54
 */
@Component
@Slf4j
public class ApplicationInit implements ApplicationRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CreateMongoIndex createMongoIndex;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private FileProperties fileProperties;

    @Resource
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    @Resource
    private UpdateStatusTask updateStatusTask;

    @Autowired
    private VersionUpdate versionUpdate;

    private final Cache<String, Object> config = CaffeineUtil.getConfig();

    private final Cache<String, Object> spaceStatistic = CaffeineUtil.getSpaceStatistic();

    private static final String URL = "/data/subject.json";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // System startup in progress
        systemStartUp(true);
        // Create default system administrator
        createAdminUser();
        // mailbox system
        initializeEmail();
        // role
        initializeRole();
        // Initialize public space
        initializePublicSpace();
        // Initialize system configuration
        String lodVersion = initializeBase();
        // Initialize discipline
        initializeSubject();
        // Initialize space configuration
        initializeApproveSetting();
        // Initialize Index
        createMongoIndex.createIndex();
        // Calculate Space Downloads - Calculate Space Visits
        accSpaceStatistic();
        // Old version space processing
        updateSpaceStatistic();
        // Restore ongoing space backup tasks
        startJob();
        // Initialize MySQL
        initMysql();
        // Add db to old space
        toOldSpaceAddDbName();
        updateStatusTask.init();
        // Version upgrade changes
        versionUpdate.init(spaceUrl, lodVersion, spaceControlConfig);
        // Spatial File Mapping Index
        createMongoIndex.createSpaceFileMappingIndex();
        log.info(">>>>>>>>>>>>>>> runner sft <<<<<<<<<<<<<");
        // Start FTP service
        runnerFTP(mongoTemplate, spaceUrl);
        log.info(">>>>>>>>>>>>>>> runner sft success <<<<<<<<<<<<<");
        // System startup ended
        systemStartUp(false);
        log.info("--------  initialize success ----------------");
    }

    private void systemStartUp(boolean status) {
        SystemConf systemConf = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("type").is(SYSTEM_STARTUP)), SystemConf.class);
        if (null == systemConf) {
            mongoTemplate.insert(new SystemConf(SYSTEM_STARTUP, status));
        } else {
            systemConf.setConf(status);
            systemConf.setLastUpdateTime(new Date());
            mongoTemplate.save(systemConf);
        }
    }

    private void createAdminUser() {
        if (mongoTemplate.find(new Query().addCriteria(Criteria.where("state").is(1)), ConsumerDO.class).size() == 0) {
            ConsumerDO consumerDO = new ConsumerDO();
            consumerDO.setName("admin");
            consumerDO.setEmailAccounts("");
            consumerDO.setPassword(RSAEncrypt.encrypt(""));
            consumerDO.setRoles(new ArrayList<String>() {

                {
                    add("role_admin");
                }
            });
            consumerDO.setOrgChineseName("");
            consumerDO.setCreateTime(LocalDateTime.now());
            consumerDO.setState(1);
            mongoTemplate.insert(consumerDO);
        }
    }

    private void initMysql() {
        DSManager dsManager = new DSManager();
        try {
            dsManager.initDS();
            log.info("init ds finished");
            dsManager.upgradeDS();
            log.info("upgrade ds finished");
            log.info("create ds success");
        } catch (Exception e) {
            log.error("create ds failed", e);
            System.exit(1);
            throw new RuntimeException(e);
        }
    }

    /**
     * Create MySQL db for previously created spaces
     */
    private void toOldSpaceAddDbName() {
        List<Space> all = spaceRepository.findAll();
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory().getConnection();
            for (Space space : all) {
                if (StringUtils.isEmpty(space.getDbName())) {
                    // Create a corresponding database for the corresponding db space
                    String dbName = SqlUtils.getUUID32();
                    String createDBSql = SqlUtils.generateCreateDBSql(dbName);
                    CommonDBUtils.executeSql(connection, createDBSql);
                    space.setDbName(dbName);
                    spaceRepository.save(space);
                    log.info(String.format("给空间(%s)创建新的db(%s)", space.getSpaceName(), dbName));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
    }

    private void initializeEmail() {
        Query emailQuery = new Query().addCriteria(Criteria.where("type").is(Constants.CaffeType.SYS_EMAIL));
        SystemConf systemConf = mongoTemplate.findOne(emailQuery, SystemConf.class);
        SysEmail sysEmail = null;
        if (null == systemConf) {
            sysEmail = new SysEmail();
            sysEmail.setHost("xxx");
            sysEmail.setPort(465);
            sysEmail.setFrom("");
            sysEmail.setUsername("");
            sysEmail.setPassword(RSAEncrypt.encrypt(""));
            sysEmail.setProtocol("smtp");
            sysEmail.setUpload(2);
            SystemConf systemConfTo = new SystemConf();
            systemConfTo.setType(Constants.CaffeType.SYS_EMAIL);
            systemConfTo.setConf(sysEmail);
            systemConfTo.setCreateTime(new Date());
            mongoTemplate.insert(systemConfTo);
        } else {
            sysEmail = JSONObject.parseObject(JSONObject.toJSONString(systemConf.getConf()), SysEmail.class);
        }
        config.put(Constants.CaffeType.SYS_EMAIL, sysEmail);
    }

    private void initializePublicSpace() {
        // viewCount  downSize  download
        Query viewQuery = new Query().addCriteria(Criteria.where("viewCount").is(null));
        if (mongoTemplate.count(viewQuery, Space.class) > 0) {
            Update update = new Update();
            update.set("viewCount", 0);
            mongoTemplate.upsert(viewQuery, update, Space.class);
        }
        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(Criteria.where("isPublic").is(1), Criteria.where("applyIs").is(1)));
        query.addCriteria(Criteria.where("state").is("1"));
        if (mongoTemplate.count(query, Space.class) == 0) {
            ConsumerDO emailAccounts = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("emailAccounts").is("admin@dataspace.cn")), ConsumerDO.class);
            Set<AuthorizationPerson> authorizationPeople = new HashSet<>();
            AuthorizationPerson authorizationPerson = new AuthorizationPerson();
            authorizationPerson.setUserId(emailAccounts.getId());
            authorizationPerson.setEmail(emailAccounts.getEmailAccounts());
            authorizationPerson.setUserName(emailAccounts.getName());
            authorizationPerson.setAvatar(emailAccounts.getAvatar());
            authorizationPerson.setRole("拥有者");
            authorizationPeople.add(authorizationPerson);
            // Get the file from resource first
            File resourceFile1 = CommonUtils.getResourceFile("/data/public.txt");
            String logo1 = CommonUtils.readJsonFile(resourceFile1.getPath());
            File resourceFile2 = CommonUtils.getResourceFile("/data/noPublic.txt");
            String logo2 = CommonUtils.readJsonFile(resourceFile2.getPath());
            String desc2 = "DataSpace是科研团队数据协作空间支撑工具，为科研人员、科研团队提供安全、便捷的仓储式科研数据协同管理服务。科研机构可以在DataSpace创建空间，邀请成员进入空间，形成范围可控、多人协作的数据管理工作台，支撑团队内部的项目数据汇交、日常科研数据归档和知识文档管理工作。";
            String top2 = "空间隔离、团队协作、版本化管理、丰富元数据、数据归档、跨空间汇交";
            Space space2 = getSpace("空间示例2-非公开空间", logo2, desc2, authorizationPeople, emailAccounts.getId(), Arrays.asList(top2.split("、")));
            space2.setApplyIs(1);
            space2.setIsPublic(0);
            space2.setViewCount(10);
            mongoTemplate.insert(space2);
            String desc1 = "无需登录即可访问公开空间的数据资源。公开空间是团队的一张名片，研究方向、数据成果都可以在公开空间与搭建分享。";
            String top1 = "所有人可浏览、团队主页、数据共享";
            Space space1 = getSpace("空间示例1-公开空间", logo1, desc1, authorizationPeople, emailAccounts.getId(), Arrays.asList(top1.split("、")));
            space1.setIsPublic(1);
            // space1.setHomeUrl("space-example-1");
            mongoTemplate.insert(space1);
            spaceControlConfig.spaceLogSave(space1.getSpaceId(), "创建空间", emailAccounts.getId(), new Operator(emailAccounts), SpaceSvnLog.ACTION_VERSION);
            spaceControlConfig.spaceLogSave(space2.getSpaceId(), "创建空间", emailAccounts.getId(), new Operator(emailAccounts), SpaceSvnLog.ACTION_VERSION);
        }
        // Fix system defaults
        Query updateQuery = new Query().addCriteria(Criteria.where("homeUrl").is("space-example-1"));
        List<Space> spaces = mongoTemplate.find(updateQuery, Space.class);
        for (Space space : spaces) {
            space.setHomeUrl(space.getSpaceId());
            mongoTemplate.save(space);
        }
    }

    private Space getSpace(String spaceName, String spaceLogo, String desc, Set<AuthorizationPerson> set, String userId, List<String> top) {
        Space space = new Space();
        String spaceId = generateSnowflake();
        space.setSpaceId(spaceId);
        space.setAuthorizationList(set);
        space.setUserId(userId);
        space.setState("1");
        space.setSpaceSize(536870912L);
        String spaceShort = spaceControlConfig.getSpaceShort();
        space.setFilePath(fileProperties.getRootDir() + FILE_SPLIT + spaceShort);
        space.setSpaceShort(spaceShort);
        space.setDescription(desc);
        String imagePath = "/" + Constants.Image.image + "/" + Constants.Image.SPACE + "/" + space.getSpaceId() + ".jpg";
        try {
            CommonUtils.generateImage(spaceLogo, spaceUrl.getRootDir() + imagePath);
            space.setSpaceLogo(imagePath);
        } catch (Exception e) {
            space.setSpaceLogo(spaceLogo);
        }
        space.setSpaceName(spaceName);
        space.setTags(top);
        space.setTopic("light");
        space.setCreateDateTime(CommonUtils.getDateTimeString(LocalDateTime.now()));
        new File(space.getFilePath()).mkdirs();
        return space;
    }

    /**
     * initial approve setting
     */
    private void initializeApproveSetting() {
        // mongoTemplate.remove(new Query(), "approveSetting");
        if (mongoTemplate.count(new Query(), ApproveSetting.class) == 0) {
            mongoTemplate.insert(ApproveSetting.builder().approveId(generateSnowflake()).gb(1024).approved(NEED_APPROVED).build());
            log.info("space approve setting initialize completed ...");
        }
    }

    private void initializeRole() {
        mongoTemplate.remove(new Query(), RoleDO.class);
        List<RoleDO> all = mongoTemplate.findAll(RoleDO.class);
        if (null == all || all.size() == 0) {
            String sup_admin_path = CommonUtils.readJsonFile(CommonUtils.getResourceFile("/data/sup_admin_role.txt").getPath());
            String[] adminPath = (sup_admin_path.replaceAll("\r", "").replaceAll("\n", "")).split(",");
            RoleDO roleAdmin = new RoleDO("管理员", Constants.ADMIN, "页面添加或导入管理员用户", Arrays.asList(adminPath), LocalDateTime.now());
            mongoTemplate.save(roleAdmin);
            String senior_general_path = CommonUtils.readJsonFile(CommonUtils.getResourceFile("/data/senior_general_role.txt").getPath());
            String[] seniorPath = (senior_general_path.replaceAll("\r", "").replaceAll("\n", "")).split(",");
            RoleDO roleSenior = new RoleDO("高级用户", Constants.SENIOR, "页面添加或导入，或注册时通过邮箱识别", Arrays.asList(seniorPath), LocalDateTime.now());
            mongoTemplate.save(roleSenior);
            RoleDO roleGeneral = new RoleDO("一般用户", Constants.GENERAL, "自行注册", Arrays.asList(seniorPath), LocalDateTime.now());
            mongoTemplate.save(roleGeneral);
        }
    }

    private String initializeBase() {
        String lodVersion = "v0";
        BasicSetting setting = mongoTemplate.findOne(new Query(), BasicSetting.class);
        if (Objects.isNull(setting)) {
            setting = new BasicSetting();
            setting.setVersion(spaceUrl.getVersion());
            setting.setCopyright("<p>Copyright @ 中国科学院计算机网络信息中心 备案号：京ICP备011111111号-03</p>");
            setting.setIndexTitle("一个科研团队的数据空间");
            setting.setIndexDescription("DataSpace是一款简洁易操作、够安全、方便团队协作的数据管理平台，你可以创建一个数据空间进行项目数据汇交、数据整编和数据发布，更好地管理和共享你的科研数据。");
            setting.setTopic("qk");
            setting.setLogo("data:image/png;base64,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");
            setting.setBanners(new ArrayList<String>(1) {

                {
                    add("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/4gIoSUNDX1BST0ZJTEUAAQEAAAIYAAAAAAQwAABtbnRyUkdCIFhZWiAAAAAAAAAAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlkZXNjAAAA8AAAAHRyWFlaAAABZAAAABRnWFlaAAABeAAAABRiWFlaAAABjAAAABRyVFJDAAABoAAAAChnVFJDAAABoAAAAChiVFJDAAABoAAAACh3dHB0AAAByAAAABRjcHJ0AAAB3AAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAFgAAAAcAHMAUgBHAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFhZWiAAAAAAAABvogAAOPUAAAOQWFlaIAAAAAAAAGKZAAC3hQAAGNpYWVogAAAAAAAAJKAAAA+EAAC2z3BhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABYWVogAAAAAAAA9tYAAQAAAADTLW1sdWMAAAAAAAAAAQAAAAxlblVTAAAAIAAAABwARwBvAG8AZwBsAGUAIABJAG4AYwAuACAAMgAwADEANv/bAEMABgQFBgUEBgYFBgcHBggKEAoKCQkKFA4PDBAXFBgYFxQWFhodJR8aGyMcFhYgLCAjJicpKikZHy0wLSgwJSgpKP/bAEMBBwcHCggKEwoKEygaFhooKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKP/AABEIA4QHgAMBIgACEQEDEQH/xAAcAAEBAQEBAQEBAQAAAAAAAAAAAQIDBAUGBwj/xABHEAEAAgECBAQDBQUGBQIEBwEAAQIDBBEFEiExQVFhcRMiMgZCgZHRFFJyobEjM0NiweEVU4KS8CRjFjSDogclNTZEc7Lx/8QAGwEBAQEBAQEBAQAAAAAAAAAAAAECAwQFBgf/xAAzEQEAAgIBAwMCBAUEAwEBAAAAAQIDEQQSITEFQVETIhRx0fBhkaGx4SMygcEGUvFCgv/aAAwDAQACEQMRAD8A/wBFAKgAAAAAAAAAAIAoigCAKACAAoAIoAIqAqKAigAAAigAAAAAAAAAAAAAAAAAAAAAAAAAAAigioAKICiKAgoIACiAKigIogKioCooAigCAAAAAAAAAAAAAAAAAAAAAAAAAAAgAKAAACAAAAAAKACAAAAAAAAAAAAAAAAAAAgCgAAACKAIAKigIKACAAAogCiACooIogCgAIoAAAICiKAAAioCoKAIACoCgACAKAACAoAAAAAAAAAAAoAIAAACgAgAKAAAAAAACAAoAAAAAAACgKgIoAigAgKAAIAKigiiAoigCKCCgIKgAKCCgIKgCgAIAAAoAAAAAAAAigCKAAAAAigAICgAIqAogCggCgACAKigAAgqAKAAAAAAgCoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAACKIAoAIKAIoAICoACoAAAogAAAAAAAAAqAAAAACoAAAKgKogIogCgAIqACoAAAAKoAggoAAAgCgCgigACAAAAAAAAAAAAAAAAAAAAAAAAoAAAIACgAgACiCiiACiAoICoKCAAAAKICooAigICgigCCgAgAAAAAAAACiKAAAAAACKigAAiiAAAKiggKAACCoAKgAKCKgAKACKACAAoIogCoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAoAIAAAAACgAgAKACAACKgKigIKAgAAKCAAqAAAAAAAAAKACAKAIoIAAAAAAAAACoqAAAKgKACCoCqIAKIIKgKKgAACoKAIAKAiKIKoAgigIqKAIooCAoAAAgAAAKACAAoAIACgAgAAAAAAAKACAACgoiooAAAACKgKCAqAAogKCAqAAACoAAAAAAAAAAAAAKIoAAAAAAAICiKAIoAAAAAgCiKAAAigAgCoKACAogAqAAAAAAAAAAAAAAAAAAAAAAAACAAoAAAAAAAAAAAIAAAAoAIAAAAAAAAACgAgAKCAgqKAioCoAqoAAAAAAAAAgAKACAAAAoAIAAAAAAAAAAAAAAAAAAAAAAAAACgAgAAqAKIAqAAqAKACACgAAAgqAqiAKCCCoCioogIoAAoAAAAAAAAAAAIAAAAAAAAAAAKKIAogCiAKgAqAAAAAAAAAAAAAAAAAAAAAAAAAAqAAAKAAigIKAiooIqAKIAAAqCgAAIAKIAKgAAAAAAAAAAAAAAAAAAAAigAgAoAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAIoACKAIooACKgIqKgoqAgAKAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAoAIAAAAAAAAAAACgAgAAAKACAAoAIAAAAACgAAAgAKAAAoAIAACiAioACoAogKoAAAAAAAAAAAgAAAAAAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAACgCAAqAACiAogCoAKgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAoAqAAAAAAAAAAAAACAAAAAAKACAAAAAAAAIqAqoKACCKCAqAAqAoAAAAAAAAAAAAAIACgAAAAAAAAAAAgAAAKAAACAAoAIACgAgAAAAAAAAAAAKACAAoAIACgAgAAAAAKAAAAAAAAAAAAAAAAAAqAIKgKoIAACggKCAoAAAAAAAAAgAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACgAAAAAAAAAAAKAIgAAAAAAAAAAAAAAAAIoogAKgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACgAAAAigAgKIoAigAAAAAAACAAoCAoAAAAAgAKAAACAAoAIACgAgAKAAAAAAACAAAAAAoAAAAAAACiAAKCAoIogCooAAAAAAACoAAAAAAAAAAAAAAAAACgAgAigAACoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIACgAgAKoAIAAAAAAAAAIAAAAAAAAAAAAAAAAAgKoACKgKgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgKCAqKAioAogCiAKgAKgCoACoAAAqAAqAAAKACCoACggACgCAoIoAAACKAAAAAAIACgAgAKAAAAAAACAAAAoAIACgAAAAAAAAAAAKgAKgCggCgAIoACoAAAAAAAAAAAAAAAAACgAgAgAAAKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAIAAAAAAAAAAAIAAAAAAAAAAAAAAoCCKgoqKAIACoAgAKAAAAAAAAAAAAAAAAAAAAAAIoAAACAAAogAACoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA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AAAABUAUAAAAAAAAAAAGgAAAAAAAAAAAAAAAAAAAAAAAEVABUAAAAAAAUAFSABUUAAQAFEAFQAAAAAAAAAAAAAAAAAAAAAAAAAEUBBUAAAAAAAAAAAAAAAAAAAkAAAAAEFQAAAAAAAAAAAABFAQAAAAAAAAAEAAAARQEABBUAABBUAAAABBQEAAABBQEFAQAEFNgQ2XY2BNhQVEaAZNl2EE2F2BWRpAQUBBQEFATY2UBNjZdlBBQEF2ARTZVRNhQEUADZQRNhQAFFQNlBBQEFAAADYUEBQAABQEUAFRRAAUAEUAAAUAEAAURQAAAAAAAAFQBoAAAAAAAAAAAAAAAAAAAAEBQAEUARUAAAAAABQAIAAVFAAEEVBVRUAAAAAAAAAAAAAAAAAAAAAAAAAAAAARUABQQAAAAAAAAAAAAAAAAAAAAAAAEFAQUBAAAAAAAAAAEUBBUAAAAAAAABAAAAEUBJFQEFAQUBNgAQUBBUAAANgBBQEFAQUBBQEFAQAU2TZQEFEERQVBQENlATY2UBDZQEFAQUAAAFAQ2UEAABRRFAAFBBQEFAEUAAAAAFAQUAAAAAFQBQAAAUBAAUAAAEAAFAAAAABQBAUEUAUVAAAAAAAAAAAAAAAAAAAAAAAAAAAQAAAAFBFRQIVABUUAAQAFEVAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEUBAAAAAAVAAAAAAAAAAAAAAAAAAAAABBUAFQAAAAAAAAAAAAEFQAAAAAABFQAAAAAAVBQRAARQBBUFABAVAEUBBQEFAQUBAAABQAEFAQUQQABFBQAAAAAAAAAAAAUEQUAAAAAUFAAAUBBQEFAAAAAAAAABQEUAAAAAFAAAAAAAAAQBQRQAAAUAEUAABFRQUAAAAAAAAAAAAAAAAAAAAAAAAABFAQUBAAFRQRUUAUBFAAAQAFAARUUBAAAAAAAAAAAAAAAAAAAAAAAAAABAVAAAAAAAAAAAAAAAAAAAAAAAAAAAAAARQEAAAAAAAAAAAAABBUAAAAAABBUAAAAAAFAAEUEQUBAAAAEUFQVBAAFQUVBQEAAABBQEFRAFAQAUAAAAAAAAAABQQUEQUBFAAUUAAAEABQAAAAAAFARQAAAAAAABQRQAAAAAAAFARQAAAAEFRQABQARFAUQURVAEAAAAAAAEABQAQAAAFRQAABFAEAAAAAAAAAAWO4AAAoAgAAAKgAigAgAoAAACoAgAKAAAAAAAAigAAAAAAAAAAAACAAAAAAeAAAAEgAeAAAAAAAAAAAAAAAAIeIAAAAAHiAAAAAAAHgACAAAAAAAICAoAAAAAAAAAAAAIAAAoAAAAAAAAAgAKAAIgIKACKACAKAAAAqACgCAAoAAAIKAAAACgAAAAAAACgAAAAAAAAAoAAAACAAoAAAAoCAAoAAKAiKAIoAAAACgAj//2Q==");
                }
            });
            setting.setDataSpaceName("数据协同管理工具");
            mongoTemplate.save(setting);
        } else {
            String version = setting.getVersion();
            if (null != version) {
                lodVersion = version;
            }
            String banner = setting.getBanner();
            if (null == setting.getBanners() && StringUtils.isNotEmpty(banner)) {
                setting.setBanners(new ArrayList<String>(1) {

                    {
                        add(banner);
                    }
                });
                setting.setBanner(null);
            }
            setting.setVersion(spaceUrl.getVersion());
            mongoTemplate.save(setting);
        }
        return lodVersion;
    }

    private void initializeSubject() {
        List<Subject> all = mongoTemplate.findAll(Subject.class);
        if (null == all || all.size() == 0) {
            // Get the file from resource first
            File resourceFile = CommonUtils.getResourceFile(URL);
            // Reading file content
            String json = CommonUtils.readJsonFile(resourceFile.getPath());
            List<Object> maps = null;
            try {
                maps = JSONObject.parseObject(json, ArrayList.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mongoTemplate.insert(maps, "subject");
        }
    }

    /**
     * Compute spatial corresponding statistics - Compute access statistics
     */
    private void accSpaceStatistic() {
        long down = spaceControlConfig.getAggregationCount("downSize", "space");
        long acc = spaceControlConfig.getAggregationCount("accCount", "access_record");
        spaceStatistic.put("spaceDownSize", down);
        spaceStatistic.put("accTotal", acc);
        // Initialize cache
        CacheData cacheData = mongoTemplate.findOne(new Query(), CacheData.class);
        if (null == cacheData) {
            cacheData = new CacheData(null, 0, 0, null, 0, 0, null);
            mongoTemplate.save(cacheData);
        }
    }

    /**
     * Update spatial statistics information
     */
    private void updateSpaceStatistic() {
        long count = mongoTemplate.count(new Query(), SpaceStatistic.class);
        if (count == 0) {
            int currentYear = CommonUtils.getCurrentYearTo();
            int currentMonth = CommonUtils.getCurrentMonth();
            int currentDay = CommonUtils.getCurrentDay();
            List<Space> all = mongoTemplate.findAll(Space.class);
            List<SpaceStatistic> spaceStatisticList = new ArrayList<>(all.size());
            for (Space space : all) {
                SpaceStatistic statistic = new SpaceStatistic();
                statistic.setSpaceId(space.getSpaceId());
                statistic.setMemberCount(space.getAuthorizationList().size());
                statistic.setViewCount(space.getViewCount());
                statistic.setYear(currentYear);
                statistic.setMonth(currentMonth);
                statistic.setDay(currentDay);
                statistic.setCreateTime(new Date());
                spaceStatisticList.add(statistic);
            }
            if (!spaceStatisticList.isEmpty()) {
                mongoTemplate.insertAll(spaceStatisticList);
            }
        }
        // Calculation of homepage statistical data
        spaceControlConfig.computeHotSpace();
    }

    /**
     * Resume scheduled tasks
     */
    private void startJob() {
        Query query = new Query().addCriteria(Criteria.where("status").is(Constants.Backup.START));
        List<BackupSpaceMain> backupSpaceMains = mongoTemplate.find(query, BackupSpaceMain.class);
        for (BackupSpaceMain backupSpaceMain : backupSpaceMains) {
            QuartzManager.addJob(backupSpaceMain.getJobId(), backupSpaceMain.getSpaceId(), MyJob.class, backupSpaceMain.getCorn());
            log.info("空间（" + backupSpaceMain.getSpaceName() + "） 备份任务恢复成功：" + backupSpaceMain.getJobId());
        }
    }

    /**
     * Start FTP service
     */
    private void runnerFTP(MongoTemplate mongoTemplate, SpaceUrl spaceUrl) {
        new Thread() {

            @SneakyThrows
            @Override
            public void run() {
                FTPServer server = new FTPServer();
                UserbaseAuthenticator auth = new UserbaseAuthenticator(mongoTemplate, spaceUrl);
                server.setAuthenticator(auth);
                server.setSpaceListener(new SpaceFileListener());
                // Register an instance of this class as a listener
                server.addListener(new CustomServer());
                server.setPASSIVE_MIN_PORT(Integer.valueOf(spaceUrl.getTransmitSt()));
                server.setPASSIVE_MAX_PORT(Integer.valueOf(spaceUrl.getTransmitEnd()));
                server.setIP_HOST(spaceUrl.getFtpHost());
                // Changes the timeout to 10 minutes  100 * 60 * 100
                // 10 minutes
                server.setTimeout(Integer.parseInt(spaceUrl.getTimeOut()));
                // Changes the buffer size  1024 * 5
                // 5 kilobytes
                server.setBufferSize(Integer.parseInt(spaceUrl.getBufferSize()));
                // Start it synchronously in our localhost and in the port 21
                server.listenSync(Integer.parseInt(spaceUrl.getPort()));
            }
        }.start();
    }
}
