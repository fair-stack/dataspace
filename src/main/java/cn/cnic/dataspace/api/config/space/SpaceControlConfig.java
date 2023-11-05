package cn.cnic.dataspace.api.config.space;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.CacheData;
import cn.cnic.dataspace.api.model.space.*;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.release.ResourceV2;
import cn.cnic.dataspace.api.model.statistics.SpaceDataInStatistic;
import cn.cnic.dataspace.api.model.statistics.SpaceDataOutStatistic;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.statistics.SpaceStatistic;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.service.space.SpaceService;
import cn.cnic.dataspace.api.util.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.ACTION_VALUE;
import static cn.cnic.dataspace.api.util.CommonUtils.*;

/**
 * Spatial data statistical control
 */
@Component
@Slf4j
public class SpaceControlConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CacheLoading cacheLoading;

    private final Cache<String, Object> spaceStatistic = CaffeineUtil.getSpaceStatistic();

    /**
     * Verify space refinement permissions
     */
    public void validateSpacePermissions(String email, String spaceId, String spaceRoleEnumRole) {
        String spaceUserRole = cacheLoading.getSpaceUserRole(spaceId, email);
        if (StringUtils.isEmpty(spaceUserRole)) {
            throw new RuntimeException("权限不足！");
        }
        if (spaceUserRole.equals(SpaceService.SPACE_OWNER)) {
            // Direct release with owner's permission
            return;
        }
        List<String> spaceMenRole = cacheLoading.getSpaceMenRole(spaceId, email);
        if (!spaceMenRole.contains(spaceRoleEnumRole)) {
            throw new RuntimeException("权限不足！");
        }
    }

    /**
     * Space Verification - Space Offline - Space Does Not Exist - Space Pending Review
     */
    public void spatialVerification(String spaceId, String email, String level) {
        SpaceSimple spaceSimple = cacheLoading.getSpaceSimple(spaceId);
        if (null == spaceSimple) {
            throw new CommonException(CommonUtils.messageInternational("RELEASE_SPACE_DELETE"));
        }
        if (spaceSimple.getState().equals("0")) {
            throw new CommonException(CommonUtils.messageInternational("SPACE_RE_APPLY"));
        }
        if (spaceSimple.getState().equals("2")) {
            throw new CommonException(CommonUtils.messageInternational("SPACE_OFFLINE_FORBIDDEN"));
        }
        String spaceUserRole = cacheLoading.getSpaceUserRole(spaceId, email);
        if (StringUtils.isEmpty(spaceUserRole)) {
            throw new CommonException(CommonUtils.messageInternational("PERMISSION_DENIED"));
        }
        if (level.equals(Constants.SpaceRole.LEVEL_ADMIN) && !spaceUserRole.equals(Constants.SpaceRole.OWNER)) {
            throw new CommonException(CommonUtils.messageInternational("PERMISSION_FORBIDDEN"));
        }
        if (level.equals(Constants.SpaceRole.LEVEL_SENIOR)) {
            if (!spaceUserRole.equals(Constants.SpaceRole.OWNER) && !spaceUserRole.equals(Constants.SpaceRole.SENIOR)) {
                throw new CommonException(CommonUtils.messageInternational("PERMISSION_FORBIDDEN"));
            }
        }
    }

    /**
     * Obtain File Author
     */
    public boolean fileAuthor(String spaceId, String filePath, String email) {
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(filePath)), FileMapping.class, spaceId);
        return fileMapping.getAuthor().getEmail().equals(email);
    }

    /**
     * Get Space ID
     */
    public String getSpaceId(String code) {
        return cacheLoading.getSpaceId(code);
    }

    /**
     * Obtain space storage identification
     */
    public String getSpaceShort() {
        String shortChain = getShort();
        long spaceShort = mongoTemplate.count(new Query().addCriteria(Criteria.where("spaceShort").is(shortChain)), Space.class);
        if (spaceShort != 0) {
            return this.getSpaceShort();
        }
        return shortChain;
    }

    /**
     * Space operation logging
     */
    public void spaceLogSave(String spaceId, String content, String userId, Operator operator, String action) {
        mongoTemplate.insert(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(spaceId).description(content).operatorId(userId).operator(operator).action(action).createTime(new Date()).version(ACTION_VALUE).build());
    }

    /**
     * Space visits, downloads, total downloads
     */
    public void spaceStat(String spaceId, String key, long value) {
        Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
        updateSpaceCount(query, key, value);
        if (!key.equals("downSize")) {
            updateStatistic(query, key, value);
        }
        // Change the number of members in the statistics
        if (key.equals("memberCount")) {
            Query query1 = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
            Update update = new Update();
            update.inc("personNum", value);
            synchronized (this) {
                mongoTemplate.findAndModify(query1, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceDataStatistic.class);
            }
        }
    }

    /**
     * Data inflow (type - ftp, webDav, fairLink, web)
     */
    public void dataFlow(String type, long date, String spaceId, boolean save) {
        if (date == 0) {
            return;
        }
        if (save) {
            try {
                SpaceSizeControl.updateActual(spaceId, date);
            } catch (Exception e) {
                log.info("--- 空间未找到 ----");
            }
        }
        Update update = new Update();
        if (!type.equals("zip") && !type.equals("web-update") && !type.equals("web-copy")) {
            // Operations within the space are not included in inflow statistics
            update.inc("inSize", date);
            dataFlow(type, date, spaceId, SpaceDataInStatistic.class);
        }
        synchronized (this) {
            Query query1 = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
            update.inc("dataSize", date);
            mongoTemplate.findAndModify(query1, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceDataStatistic.class);
        }
    }

    /**
     * Data outflow (type - ftp, webDav, fairLink, web)
     */
    public void dataOut(String type, long date, String spaceId) {
        if (date <= 0) {
            return;
        }
        dataFlow(type, date, spaceId, SpaceDataOutStatistic.class);
        synchronized (this) {
            Update update = new Update();
            Query query1 = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
            update.inc("outSize", date);
            mongoTemplate.findAndModify(query1, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceDataStatistic.class);
        }
    }

    /**
     * Data volume - file deletion
     */
    public void deleteData(String spaceId, long data) {
        Query query1 = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
        Long aLong = Long.valueOf("-" + data);
        synchronized (this) {
            if (aLong < 0) {
                SpaceDataStatistic spaceDataStatistic = mongoTemplate.findOne(query1, SpaceDataStatistic.class);
                if (spaceDataStatistic.getDataSize() == 0) {
                    return;
                }
                long size = spaceDataStatistic.getDataSize() + aLong;
                if (size < 0) {
                    Update update = new Update();
                    update.set("dataSize", 0);
                    mongoTemplate.findAndModify(query1, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceDataStatistic.class);
                } else {
                    Update update = new Update();
                    update.inc("dataSize", aLong);
                    mongoTemplate.findAndModify(query1, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceDataStatistic.class);
                }
            } else {
                Update update = new Update();
                update.inc("dataSize", aLong);
                mongoTemplate.findAndModify(query1, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceDataStatistic.class);
            }
        }
    }

    private void dataFlow(String type, long date, String spaceId, Class cla) {
        int currentYear = CommonUtils.getCurrentYearTo();
        int currentMonth = CommonUtils.getCurrentMonth();
        int currentDay = CommonUtils.getCurrentDay();
        Update staUpdate = new Update();
        Query query = new Query();
        staUpdate.setOnInsert("spaceId", spaceId);
        staUpdate.setOnInsert("year", currentYear);
        staUpdate.setOnInsert("month", currentMonth);
        staUpdate.setOnInsert("day", currentDay);
        staUpdate.setOnInsert("createTime", new Date());
        Criteria criteria = Criteria.where("year").is(currentYear);
        criteria.and("spaceId").is(spaceId);
        criteria.and("month").is(currentMonth);
        criteria.and("day").is(currentDay);
        query.addCriteria(criteria);
        String key = "";
        if (type.equals("ftp")) {
            key = "ftpData";
        } else if (type.equals("webDav")) {
            key = "webDavData";
        } else if (type.equals("web")) {
            key = "webData";
        } else if (type.equals("fairLink")) {
            key = "fairLinkData";
        } else if (type.equals("released")) {
            key = "releasedData";
        } else {
            return;
        }
        if (!key.equals("webData")) {
            staUpdate.setOnInsert("webData", 0L);
        }
        if (!key.equals("fairLinkData")) {
            staUpdate.setOnInsert("fairLinkData", 0L);
        }
        if (!key.equals("ftpData")) {
            staUpdate.setOnInsert("ftpData", 0L);
        }
        if (!key.equals("webDavData")) {
            staUpdate.setOnInsert("webDavData", 0L);
        }
        if (!key.equals("releasedData")) {
            staUpdate.setOnInsert("releasedData", 0L);
        }
        synchronized (this) {
            staUpdate.inc(key, date);
            staUpdate.inc("totalData", date);
            mongoTemplate.findAndModify(query, staUpdate, new FindAndModifyOptions().returnNew(true).upsert(true), cla);
        }
    }

    private void updateSpaceCount(Query query, String key, long value) {
        Update update = new Update();
        update.inc(key, value);
        mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), Space.class);
        synchronized (this) {
            if (key.equals("downSize")) {
                Object spaceDownSize = spaceStatistic.getIfPresent("spaceDownSize");
                if (spaceDownSize != null) {
                    spaceStatistic.put("spaceDownSize", (long) spaceDownSize + value);
                }
            }
        }
    }

    private void updateStatistic(Query query, String key, long value) {
        Update staUpdate = new Update();
        staUpdate.inc(key, value);
        if (!key.equals("download")) {
            staUpdate.setOnInsert("download", 0L);
        }
        if (!key.equals("memberCount")) {
            staUpdate.setOnInsert("memberCount", 0L);
        }
        if (!key.equals("viewCount")) {
            staUpdate.setOnInsert("viewCount", 0L);
        }
        int currentYear = CommonUtils.getCurrentYearTo();
        int currentMonth = CommonUtils.getCurrentMonth();
        int currentDay = CommonUtils.getCurrentDay();
        staUpdate.setOnInsert("year", currentYear);
        staUpdate.setOnInsert("month", currentMonth);
        staUpdate.setOnInsert("day", currentDay);
        staUpdate.setOnInsert("createTime", new Date());
        Criteria criteria = Criteria.where("year").is(currentYear);
        criteria.and("month").is(currentMonth);
        criteria.and("day").is(currentDay);
        query.addCriteria(criteria);
        synchronized (this) {
            mongoTemplate.findAndModify(query, staUpdate, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceStatistic.class);
        }
    }

    /**
     * Remove User
     */
    public void spaceDeleteUser(String spaceId) {
        Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
        Update update = new Update();
        update.inc("memberCount", -1);
        Update update2 = new Update();
        update2.inc("personNum", -1);
        synchronized (this) {
            mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceStatistic.class);
            mongoTemplate.findAndModify(query, update2, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceDataStatistic.class);
        }
    }

    /**
     * Capacity modification
     */
    public void spaceCapacityUpdate(String spaceId, long size, boolean judge) {
        Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
        Update update = new Update();
        update.set("capacity", size);
        if (judge) {
            update.set("state", 1);
        }
        mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceDataStatistic.class);
    }

    /**
     * Calculation of homepage information statistics data
     */
    public void computeHotSpace() {
        CacheData cacheData = mongoTemplate.findOne(new Query(), CacheData.class);
        boolean judge = false;
        String fileSize = cacheData.getFileSize();
        if (StringUtils.isEmpty(fileSize)) {
        }
        long spaceCount = cacheData.getSpaceCount();
        if (spaceCount == 0) {
            judge = true;
            cacheData.setSpaceCount(mongoTemplate.count(new Query(), Space.class));
        }
        long userCount = cacheData.getUserCount();
        if (userCount == 0) {
            judge = true;
            cacheData.setUserCount(mongoTemplate.count(new Query(), ConsumerDO.class));
        }
        // String downloadCount = cacheData.getDownloadCount();
        // if(StringUtils.isEmpty(downloadCount)){
        // judge = true;
        // long aggregationCount = getAggregationCount("downSize", "space");
        // cacheData.setDownloadCount(fileUtils.formFileSize(aggregationCount));
        // }
        // 
        // long acc = cacheData.getAccTotal();
        // if(acc == 0){
        // judge = true;
        // long aggregationCount = getAggregationCount("accCount", "access_record");
        // cacheData.setAccTotal(aggregationCount);
        // }
        long publicCount = cacheData.getPublicCount();
        if (publicCount == 0) {
            judge = true;
            cacheData.setPublicCount(mongoTemplate.count(new Query().addCriteria(Criteria.where("type").is(Constants.PUBLISHED)), ResourceV2.class));
        }
        String hotSpaceId = cacheData.getHotSpaceId();
        if (StringUtils.isEmpty(hotSpaceId)) {
            judge = true;
            cacheData.setHotSpaceId(hotSpace());
        }
        if (judge) {
            mongoTemplate.save(cacheData);
        }
    }

    // Hottest space
    public String hotSpace() {
        String timeOne = CommonUtils.getDateString(new Date());
        String timeTow = CommonUtils.getDateString(CommonUtils.getPastDate(6));
        Map<String, Date> timeQuery = getTimeQuery(timeTow, timeOne);
        Criteria criteria = Criteria.where("createTime").gte(timeQuery.get("startTime")).lte(timeQuery.get("endTime"));
        long count = mongoTemplate.count(new Query().addCriteria(criteria), SpaceStatistic.class);
        if (count > 0) {
            // Count all quantities
            long viewTotal = 0L;
            long memberTotal = 0L;
            long downloadTotal = 0L;
            List<Document> aggregation = getAggregation(criteria);
            for (Document document : aggregation) {
                viewTotal = (Long) document.get("view");
                memberTotal = (Long) document.get("member");
                downloadTotal = (Long) document.get("download");
            }
            Query query = new Query();
            query.addCriteria(new Criteria().orOperator(Criteria.where("isPublic").is(1), Criteria.where("applyIs").is(1)));
            query.addCriteria(Criteria.where("state").is("1"));
            List<Space> spaceList = mongoTemplate.find(query, Space.class);
            Map<Space, Double> map = new HashMap<>();
            for (Space space : spaceList) {
                double target = 0.0;
                String spaceId = space.getSpaceId();
                Criteria lte = Criteria.where("spaceId").is(spaceId).and("createTime").gte(timeQuery.get("startTime")).lte(timeQuery.get("endTime"));
                List<Document> mappedResults = getAggregation(lte);
                for (Document mappedResult : mappedResults) {
                    long view = (Long) mappedResult.get("view");
                    long member = (Long) mappedResult.get("member");
                    long download = (Long) mappedResult.get("download");
                    if (viewTotal > 0) {
                        double v = new BigDecimal((float) view / viewTotal).setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
                        target += v;
                    }
                    if (memberTotal > 0) {
                        double v = new BigDecimal((float) member / memberTotal).setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
                        target += v;
                    }
                    if (downloadTotal > 0) {
                        double v = new BigDecimal((float) download / downloadTotal).setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
                        target += v;
                    }
                }
                map.put(space, target);
            }
            // Sort to the highest space
            if (map.size() > 0) {
                Map<Space, Double> sortMap = CommonUtils.sortMapDesc(map);
                ArrayList<Space> strings = new ArrayList<>(sortMap.keySet());
                Space spaceId = strings.get(0);
                return spaceId.getSpaceId();
            }
        }
        return null;
    }

    private List<Document> getAggregation(Criteria criteria) {
        List<AggregationOperation> aggList = new ArrayList<>(3);
        aggList.add(Aggregation.match(criteria));
        aggList.add(Aggregation.group().sum("viewCount").as("view").sum("memberCount").as("member").sum("download").as("download"));
        Aggregation aggregation = Aggregation.newAggregation(aggList);
        AggregationResults<Document> storage = mongoTemplate.aggregate(aggregation, "space_statistic", Document.class);
        return storage.getMappedResults();
    }

    /**
     * Obtain time query criteria
     */
    private Map<String, Date> getTimeQuery(String startTme, String endTime) {
        Date startDate = null;
        Date endDate = null;
        SimpleDateFormat startSdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Convert to Greenwich Mean Time Zone
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
        try {
            startDate = sdf.parse(sdf.format(startSdf.parse(startTme)));
            endDate = sdf.parse(sdf.format(startSdf.parse(endTime)));
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(endDate);
            calendar.add(Calendar.DATE, 1);
            endDate = calendar.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Date> result = new HashMap<>(2);
        result.put("startTime", startDate);
        result.put("endTime", endDate);
        return result;
    }

    public long getAggregationCount(String key, String collection) {
        long total = 0;
        List<AggregationOperation> aggList = new ArrayList<>();
        aggList.add(Aggregation.group().sum(key).as("count"));
        Aggregation count = Aggregation.newAggregation(aggList);
        AggregationResults<Document> storage = mongoTemplate.aggregate(count, collection, Document.class);
        List<Document> mappedResults = storage.getMappedResults();
        for (Document mappedResult : mappedResults) {
            try {
                total = (long) mappedResult.get("count");
            } catch (Exception e) {
                total = (long) (int) mappedResult.get("count");
            }
        }
        return total;
    }
}
