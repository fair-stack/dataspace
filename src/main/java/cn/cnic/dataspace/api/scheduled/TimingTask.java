package cn.cnic.dataspace.api.scheduled;

import cn.cnic.dataspace.api.bigdataprocessing.SpatialLogRule;
import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.datax.admin.entity.DataMapping;
import cn.cnic.dataspace.api.datax.admin.mapper.DataMappingMapper;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingService;
import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.model.CacheData;
import cn.cnic.dataspace.api.model.center.Account;
import cn.cnic.dataspace.api.model.harvest.*;
import cn.cnic.dataspace.api.model.manage.BasicSetting;
import cn.cnic.dataspace.api.model.manage.SystemConf;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.file.UploadFile;
import cn.cnic.dataspace.api.model.open.ApiAuth;
import cn.cnic.dataspace.api.model.open.OpenApi;
import cn.cnic.dataspace.api.model.release.ResourceV2;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.model.user.UserLoginLog;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.util.*;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import static cn.cnic.dataspace.api.util.Constants.SYSTEM_STARTUP;

@EnableScheduling
@Slf4j
@Component
public class TimingTask {

    @Resource
    private SpaceUrl spaceUrl;

    @Resource
    private SpaceControlConfig spaceControlConfig;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private DataMappingMapper dataMappingMapper;

    @Resource
    private SpatialLogRule spatialLogRule;

    @Autowired
    private DataMappingService dataMappingService;

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    private final Cache<String, Object> spaceStatistic = CaffeineUtil.getSpaceStatistic();

    // Obtain system loading status
    private boolean getSystemStatus() {
        SystemConf systemConf = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("type").is(SYSTEM_STARTUP)), SystemConf.class);
        return (boolean) systemConf.getConf();
    }

    /**
     * Clear online users
     */
    // Execute once every half hour
    @Scheduled(cron = "0 */30 * * * ?")
    public void clearOnlineUser() {
        List<ConsumerDO> consumerDOS = mongoTemplate.find(new Query().addCriteria(Criteria.where("online").is(true)), ConsumerDO.class);
        if (consumerDOS.isEmpty()) {
            return;
        }
        Set<String> hashSet = new HashSet<>(consumerDOS.size());
        ConcurrentMap<@NonNull String, @NonNull String> stringStringConcurrentMap = tokenCache.asMap();
        for (String token : stringStringConcurrentMap.keySet()) {
            hashSet.add(stringStringConcurrentMap.get(token));
        }
        for (ConsumerDO consumerDO : consumerDOS) {
            String emailAccounts = consumerDO.getEmailAccounts();
            if (!hashSet.contains(emailAccounts)) {
                // Login expired Modify login status
                Query query = new Query().addCriteria(Criteria.where("_id").is(consumerDO.getId()));
                Update update = new Update();
                update.set("online", false);
                update.set("lastLogoutTime", new Date());
                // Modify online status
                mongoTemplate.upsert(query, update, ConsumerDO.class);
                // Record login logs
                UserLoginLog userLoginLog = new UserLoginLog(consumerDO.getId(), emailAccounts, "登录信息过期", null, Constants.LoginWay.SYS, "logout");
                mongoTemplate.insert(userLoginLog);
            }
        }
    }

    /**
     * @ MethodName: clearTheLog
     */
    // Execute once an hour
    @Scheduled(cron = "0 */60 * * * ?")
    public void clearTheLog() {
        if (getSystemStatus()) {
            return;
        }
        log.info(">>>>>>>>>>>>>>> 定时统计首页统计数据    <<<<<<<<<<<<<<<<<<");
        CacheData cacheData = mongoTemplate.findOne(new Query(), CacheData.class);
        List<Space> all = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").is("1")), Space.class);
        long total = 0L;
        for (Space space : all) {
            Query query = new Query().addCriteria(Criteria.where("spaceId").is(space.getSpaceId()));
            SpaceDataStatistic spaceDataStatistic = mongoTemplate.findOne(query, SpaceDataStatistic.class);
            if (null == spaceDataStatistic) {
                continue;
            }
            total += spaceDataStatistic.getDataSize();
        }
        // Total system capacity storage
        cacheData.setFileSize(FileUtils.formFileSize(total));
        cacheData.setFileSizeLong(total);
        cacheData.setSpaceCount(mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is("1")), Space.class));
        cacheData.setUserCount(mongoTemplate.count(new Query(), ConsumerDO.class));
        cacheData.setPublicCount(mongoTemplate.count(new Query().addCriteria(Criteria.where("type").is(Constants.PUBLISHED)), ResourceV2.class));
        mongoTemplate.save(cacheData);
        log.info(">>>>>>>>>>>>>>>    任务结束  <<<<<<<<<<<<<<<<<<");
    }

    /**
     * @ MethodName: computeHotSpace
     */
    // At midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void computeHotSpace() {
        if (getSystemStatus()) {
            return;
        }
        log.info(">>>>>>>>>>>>>>> 定时计算首页最热空间信息  <<<<<<<<<<<<<<<<<<");
        CacheData cacheData = mongoTemplate.findOne(new Query(), CacheData.class);
        String spaceId = spaceControlConfig.hotSpace();
        if (StringUtils.isNotEmpty(spaceId)) {
            cacheData.setHotSpaceId(spaceId);
            mongoTemplate.save(cacheData);
        }
        log.info(">>>>>>>>>>>>>>>   任务计算结束   <<<<<<<<<<<<<<<<<<");
        log.info(">>>>>>>>>>>>>>> 定时失效分享链接  <<<<<<<<<<<<<<<<<<");
        Query query = new Query().addCriteria(Criteria.where("share").is(0).and("failureTime").lte(new Date()));
        long count = mongoTemplate.count(query, ShareLink.class);
        if (count > 0) {
            List<ShareLink> shareLinks = mongoTemplate.find(query, ShareLink.class);
            for (ShareLink shareLink : shareLinks) {
                // Delete ftp
                if (StringUtils.isNotEmpty(shareLink.getFtpUserId())) {
                    mongoTemplate.remove(new Query().addCriteria(Criteria.where("_id").is(shareLink.getFtpUserId())), FtpUser.class);
                    mongoTemplate.remove(new Query().addCriteria(Criteria.where("userId").is(shareLink.getFtpUserId())), FTPShort.class);
                }
            }
            // Set Expiration
            Update update = new Update();
            update.set("share", 1);
            mongoTemplate.updateMulti(query, update, ShareLink.class);
            // Delete FTP user cache
            CaffeineUtil.clearFtpUserId();
            CaffeineUtil.clearFtpShor();
        }
        log.info(">>>>>>>>>>>>>>> 共有 " + count + " 个分享链接过期  <<<<<<<<<<<<<<<<<<");
    }

    /**
     * computeMysqlDataCount
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public // @Scheduled(fixedDelay = 199991)
    void computeMysqlDataCount() {
        if (getSystemStatus()) {
            return;
        }
        log.info(">>>>>>>>>>>>>>> 定时计算首页DataMappingCount  <<<<<<<<<<<<<<<<<<");
        List<Space> all = spaceRepository.findAll();
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory().getConnection();
            for (Space space : all) {
                QueryWrapper<DataMapping> dataMappingQueryWrapper = new QueryWrapper<>();
                dataMappingQueryWrapper.eq("space_id", space.getSpaceId());
                List<DataMapping> dataMappingList = dataMappingMapper.selectList(dataMappingQueryWrapper);
                for (DataMapping dataMapping : dataMappingList) {
                    String sql = String.format("ANALYZE TABLE `%s`.`%s`", space.getDbName(), dataMapping.getName());
                    CommonDBUtils.executeSql(connection, sql);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        log.info(">>>>>>>>>>>>>>>   DataMappingCount  任务计算结束   <<<<<<<<<<<<<<<<<<");
    }

    /**
     * @ MethodName: deleteUploadFile
     */
    // 00:30 am
    @Scheduled(cron = "0 30 0 * * ?")
    public void deleteUploadFile() throws ParseException {
        if (getSystemStatus()) {
            return;
        }
        log.info(">>>>>>>>>>>>>>> 删除文件分片的过期文件开始执行  <<<<<<<<<<<<<<<<<<");
        String path = spaceUrl.getReleaseStored() + "/" + Constants.Release.FILE_SHARD;
        File file = new File(path);
        Date failureTime = CommonUtils.getHour(new Date(), -6);
        long checkTime = CommonUtils.getStringToDate(CommonUtils.getDateString(failureTime)).getTime();
        if (file.exists()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    String name = f.getName();
                    long fileTime = CommonUtils.getStringToDate(name).getTime();
                    if (fileTime <= checkTime) {
                        try {
                            Files.walk(f.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
            }
        }
        log.info(">>>>>>>>>>>>>>> 文件分片的过期文件删除结束  <<<<<<<<<<<<<<<<<<");
        log.info(">>>>>>>>>>>>>>> 开始删除数据库记录  <<<<<<<<<<<<<<<<<<");
        Query query = new Query().addCriteria(Criteria.where("createTime").lte(failureTime));
        long count = mongoTemplate.count(query, UploadFile.class);
        log.info("数据库分片过期数据共: {}  " + count + " 条");
        if (count > 0) {
            mongoTemplate.remove(query, UploadFile.class);
        }
        log.info(">>>>>>>>>>>>>>> 任务结束  <<<<<<<<<<<<<<<<<<");
    }

    /**
     * @ MethodName: updateOpenApiAuth
     */
    // 1:00 am
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateOpenApiAuth() {
        if (getSystemStatus()) {
            return;
        }
        log.info(">>>>>>>>>>>>>>> 定时更新开放接口授权信息  <<<<<<<<<<<<<<<<<<");
        Query query = new Query().addCriteria(Criteria.where("state").is(Constants.OpenApiState.online));
        List<OpenApi> openApiList = mongoTemplate.find(query, OpenApi.class);
        for (OpenApi openApi : openApiList) {
            List<ApiAuth> authApp = openApi.getAuthApp();
            if (null != authApp) {
                boolean update = false;
                for (ApiAuth apiAuth : authApp) {
                    if (!apiAuth.isExpire() && apiAuth.getAuthType().equals(Constants.OpenApiState.Short_Time)) {
                        // "2022-09-20"
                        String authTime = apiAuth.getAuthTime();
                        String dateString = CommonUtils.getDateString(new Date());
                        try {
                            if (CommonUtils.getStringToDate(authTime).getTime() < CommonUtils.getStringToDate(dateString).getTime()) {
                                apiAuth.setExpire(true);
                                CacheLoading cacheLoading = new CacheLoading();
                                cacheLoading.clearAppAuthPath(apiAuth.getAppKey());
                                update = true;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (update) {
                    openApi.setAuthApp(authApp);
                    mongoTemplate.save(openApi);
                }
            }
        }
        log.info(">>>>>>>>>>>>>>>   任务计算结束   <<<<<<<<<<<<<<<<<<");
    }

    /**
     * @ MethodName: updateSpaceFile
     */
    // 2 am
    @Scheduled(cron = "0 0 2 * * ?")
    public void updateSpaceFile() {
        if (getSystemStatus()) {
            return;
        }
        log.info(">>>>>>>>>>>>>>> 凌晨两点定时更新 上一天文件发生变动的空间（空间下文件的数量和大小及空间文件映射） <<<<<<<<<<<<<<<<<<");
        Query query1 = new Query().addCriteria(Criteria.where("state").is("1"));
        List<Space> all = mongoTemplate.find(query1, Space.class);
        for (Space space : all) {
            String spaceId = space.getSpaceId();
            Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
            SpaceDataStatistic spaceDataStatistic = mongoTemplate.findOne(query, SpaceDataStatistic.class);
            // Verify if the space file has changed
            Criteria criteria = Criteria.where("action").is("file").and("spaceId").is(spaceId);
            Date date = new Date();
            Date end = CommonUtils.conversionDate(CommonUtils.getDateString(date), "end");
            Date start = CommonUtils.conversionDate(CommonUtils.getDateString(date), "start");
            criteria.and("createTime").gte(start).lte(end);
            long logCount = mongoTemplate.count(new Query().addCriteria(criteria), SpaceSvnLog.class);
            spatialLogRule.spaceFileUpload(logCount, space, spaceDataStatistic);
        }
        log.info(">>>>>>>>>>>>>>>   任务计算结束   <<<<<<<<<<<<<<<<<<");
    }

    /**
     * Regularly clean up completed space import task records
     */
    // 3am
    @Scheduled(cron = "0 0 3 * * ?")
    public void deleteTimingTask() {
        log.info(">>>>>>>>>>>>>>> task:7 定时清理 已完成的空间导入任务记录 任务开始 <<<<<<<<<<<<<<<<<<");
        // Clean up completed data records
        // Completed Tasks
        Query query = new Query().addCriteria(Criteria.where("state").is(2));
        List<MiningTask> miningTasks = mongoTemplate.find(query, MiningTask.class);
        for (MiningTask miningTask : miningTasks) {
            mongoTemplate.remove(new Query().addCriteria(Criteria.where("rootId").is(miningTask.getTaskId())), TaskFileImp.class);
        }
        mongoTemplate.remove(query, MiningTask.class);
        log.info(">>>>>>>>>>>>>>> 任务处理结束 <<<<<<<<<<<<<<<<<<");
    }

    /**
     * Space log processing (to prevent excessive log data volume)
     */
    @Scheduled(cron = "0 0 1 1 * ? ")
    public void spaceLogProcessing() {
        if (getSystemStatus()) {
            return;
        }
        log.info(">>>>>>>>>>>>>>> 月初 空间日志处理 （一个月为限 超出的 导出成文件）任务开始 <<<<<<<<<<<<<<<<<<");
        spatialLogRule.execute();
        log.info(">>>>>>>>>>>>>>> 任务处理结束 <<<<<<<<<<<<<<<<<<");
    }

    /**
     * Data push fairman()
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void fairManDataSend() {
        if (getSystemStatus()) {
            return;
        }
        log.info(">>>>>>>>>>>>>>> fairMan 数据统计推送任务开始 <<<<<<<<<<<<<<<<<<");
        Account one = mongoTemplate.findOne(new Query(), Account.class);
        if (!one.getIsOpen()) {
            return;
        }
        BasicSetting basicSetting = mongoTemplate.findOne(new Query(), BasicSetting.class);
        Map<String, Object> dataMap = new HashMap<>(5);
        dataMap.put("userName", one.getAccount());
        dataMap.put("softwareId", "619a4823be34efa5543d8bf8");
        dataMap.put("softwareName", "DataSpace");
        dataMap.put("softwareVersion", basicSetting.getVersion());
        Object spaceDownSize = spaceStatistic.getIfPresent("spaceDownSize");
        Object accTotal = spaceStatistic.getIfPresent("accTotal");
        CacheData cacheData = mongoTemplate.findOne(new Query(), CacheData.class);
        // data statistics
        Map<String, Object> data = new HashMap<>(10);
        data.put("spacesCount", cacheData.getSpaceCount());
        data.put("totalStorage", cacheData.getFileSizeLong());
        data.put("publishDataset", cacheData.getPublicCount());
        data.put("usersCount", cacheData.getUserCount());
        data.put("totalDownload", spaceDownSize);
        data.put("visitCount", accTotal);
        long downCount = 0L;
        int totalTables = 0;
        long totalTableRows = 0L;
        Query query = new Query().addCriteria(Criteria.where("state").is("1"));
        List<Space> spaces = mongoTemplate.find(query, Space.class);
        for (Space space : spaces) {
            downCount += space.getDownload();
            try {
                R<Map<String, Object>> staInfo = dataMappingService.getStaInfo(space.getSpaceId());
                Map<String, Object> data1 = staInfo.getData();
                totalTableRows += (long) data1.get("dataCount");
                totalTables += (int) data1.get("tableCount");
            } catch (Exception e) {
                log.info("空间：" + space.getSpaceName() + " _ spaceId: " + space.getSpaceId() + "   统计表格数据出错!");
            }
        }
        long totalFiles = 0L;
        List<SpaceDataStatistic> spaceDataStatistics = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").is(1)), SpaceDataStatistic.class);
        for (SpaceDataStatistic spaceDataStatistic : spaceDataStatistics) {
            totalFiles += spaceDataStatistic.getFileNum();
        }
        data.put("downloadCount", downCount);
        data.put("totalFiles", totalFiles);
        data.put("totalTables", totalTables);
        data.put("totalTableRows", totalTableRows);
        dataMap.put("softwareData", data);
        String json = JSONObject.toJSONString(dataMap);
        HttpClient httpClient = new HttpClient();
        String fairManMarketUrl = spaceUrl.getFairManDataSendUrl();
        String res = httpClient.doPostJsonWayTwo(json, fairManMarketUrl);
        log.info(">>>>>>>>>>>>>>> 任务处理结束 <<<<<<<<<<<<<<<<<<");
    }
}
