package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.model.CacheData;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.service.HomeService;
import cn.cnic.dataspace.api.service.space.MessageService;
import cn.cnic.dataspace.api.service.space.SettingService;
import cn.cnic.dataspace.api.util.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Slf4j
@Service
public class HomeServiceImpl implements HomeService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private SettingService settingService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CacheLoading cacheLoading;

    private final Cache<String, Object> spaceStatistic = CaffeineUtil.getSpaceStatistic();

    @Override
    public ResponseResult<Object> spaceSearch(int page, int size, HttpServletRequest request) {
        String userId = getUserId(request);
        Query query = getQuery(userId);
        long count = mongoTemplate.count(query, Space.class);
        List<Map<String, Object>> spaces = new ArrayList<>(10);
        List<String> spaceIds = null;
        if (StringUtils.isNotEmpty(userId)) {
            spaceIds = cacheLoading.getUserApplySpaces(userId);
        }
        if (count > 0) {
            query.with(PageRequest.of(page - 1, size));
            List<Space> spaceList = mongoTemplate.find(query, Space.class);
            for (Space space : spaceList) {
                Map<String, Object> spaceMap = getSpaceMap(space, spaceIds, userId);
                spaces.add(spaceMap);
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("data", spaces);
        resultMap.put("total", count);
        return ResultUtil.success(resultMap);
    }

    @Override
    public ResponseResult<Object> spaceActive(HttpServletRequest request) {
        String userId = getUserId(request);
        Query query = getQuery(userId);
        List<String> spaceIds = null;
        if (StringUtils.isNotEmpty(userId)) {
            spaceIds = cacheLoading.getUserApplySpaces(userId);
        }
        query.addCriteria(Criteria.where("viewCount").ne(null));
        query.with(PageRequest.of(0, 5));
        query.with(Sort.by(DESC, "viewCount"));
        List<Space> spaces = mongoTemplate.find(query, Space.class);
        return result(spaces, spaceIds, userId);
    }

    private ResponseResult<Object> result(List<Space> spaceList, List<String> spaceId, String userId) {
        List<Map<String, Object>> resultList = new ArrayList<>(spaceList.size());
        if (!spaceList.isEmpty()) {
            for (Space space : spaceList) {
                Map<String, Object> spaceMap = getSpaceMap(space, spaceId, userId);
                spaceMap.put("description", space.getDescription());
                resultList.add(spaceMap);
            }
        }
        return ResultUtil.success(resultList);
    }

    @Override
    public ResponseResult<Object> informationStatistics() {
        Object spaceDownSize = spaceStatistic.getIfPresent("spaceDownSize");
        Object accTotal = spaceStatistic.getIfPresent("accTotal");
        CacheData cacheData = mongoTemplate.findOne(new Query(), CacheData.class);
        cacheData.setDownloadCount(FileUtils.formFileSize((spaceDownSize == null ? 0L : (Long) spaceDownSize)));
        cacheData.setAccTotal(accTotal == null ? 0L : (Long) accTotal);
        return ResultUtil.success(cacheData);
    }

    @Override
    public ResponseResult<Object> hotWordsList(HttpServletRequest request) {
        Map<String, Object> resultMap = new HashMap<>(2);
        String userId = getUserId(request);
        Query query = getQuery(userId);
        List<String> spaceIds = null;
        if (StringUtils.isNotEmpty(userId)) {
            spaceIds = cacheLoading.getUserApplySpaces(userId);
        }
        query.with(PageRequest.of(0, 1));
        query.with(Sort.by(DESC, "createDateTime"));
        List<Space> spaces = mongoTemplate.find(query, Space.class);
        if (!spaces.isEmpty()) {
            Space space = spaces.get(0);
            Map<String, Object> spaceMap = getSpaceMap(space, spaceIds, userId);
            spaceMap.put("description", space.getDescription());
            resultMap.put("newSpace", spaceMap);
        } else {
            resultMap.put("newSpace", null);
        }
        CacheData cacheData = mongoTemplate.findOne(new Query(), CacheData.class);
        if (null != cacheData && StringUtils.isNotEmpty(cacheData.getHotSpaceId())) {
            Space id = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(cacheData.getHotSpaceId())), Space.class);
            if (null == id) {
                resultMap.put("hotSpace", null);
            } else {
                Map<String, Object> spaceMap = getSpaceMap(id, spaceIds, userId);
                spaceMap.put("description", id.getDescription());
                resultMap.put("hotSpace", spaceMap);
            }
        } else {
            resultMap.put("hotSpace", null);
        }
        return ResultUtil.success(resultMap);
    }

    private Map<String, Object> getSpaceMap(Space space, List<String> spaceIds, String userId) {
        Map<String, Object> newMap = new HashMap<>(10);
        newMap.put("spaceId", space.getSpaceId());
        newMap.put("spaceName", space.getSpaceName());
        newMap.put("spaceLogo", space.getSpaceLogo());
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        if (authorizationList != null && authorizationList.size() > 0) {
            ArrayList<AuthorizationPerson> authorizationPeople = new ArrayList<>(authorizationList);
            newMap.put("authorizationList", authorizationPeople.subList(0, 1));
        } else {
            newMap.put("authorizationList", null);
        }
        newMap.put("tags", space.getTags());
        newMap.put("applyIs", space.getApplyIs());
        newMap.put("isPublic", space.getIsPublic());
        newMap.put("homeUrl", space.getHomeUrl());
        newMap.put("haveApply", getApply(space, spaceIds, userId));
        newMap.put("memberCount", space.getAuthorizationList().size());
        newMap.put("viewCount", space.getViewCount());
        return newMap;
    }

    @Override
    public ResponseResult<Object> setAcc(String acc, String pwd, boolean isOpen) {
        String decrypt = RSAEncrypt.decrypt(pwd);
        if (StringUtils.isEmpty(decrypt)) {
            return ResultUtil.errorInternational("PWD_ERROR");
        }
        Object object = cacheLoading.loadingCenter();
        if (object != null) {
            return ResultUtil.errorInternational("HOME_CONFIG");
        }
        return settingService.setAcc(acc, pwd, isOpen);
    }

    @Override
    public ResponseResult<Object> versionInfo(String code, String version, String message) {
        String codeTo = "XXX";
        if (code.equals(codeTo)) {
            // Message reminder
            String title = messageInternational("HOME_NEW_VERSION") + " " + version;
            String content = messageInternational("HOME_ADMIN_VERSION") + version + messageInternational("HOME_VERSION_UPDATE") + message;
            messageService.sendToAdminVersion(title, content);
        }
        return ResultUtil.success();
    }

    @Override
    public ResponseResult<Object> tagCount() {
        List<AggregationOperation> aggList = new ArrayList<>(5);
        aggList.add(Aggregation.match(new Criteria().orOperator(Criteria.where("isPublic").is(1), Criteria.where("applyIs").is(1))));
        aggList.add(Aggregation.match(Criteria.where("state").is("1")));
        // Split embedded documents)
        aggList.add(Aggregation.unwind("$tags"));
        aggList.add(Aggregation.group("tags").count().as("count"));
        aggList.add(Aggregation.sort(Sort.Direction.DESC, "count"));
        Aggregation count = Aggregation.newAggregation(aggList);
        AggregationResults<Document> storage = mongoTemplate.aggregate(count, "space", Document.class);
        List<Document> mappedResults = storage.getMappedResults();
        List<Map<String, Object>> resultList = new ArrayList<>(mappedResults.size());
        for (Document mappedResult : mappedResults) {
            String tag = mappedResult.get("_id").toString();
            Object count1 = mappedResult.get("count");
            Map<String, Object> map = new HashMap<>(2);
            map.put("key", tag);
            map.put("value", count1);
            resultList.add(map);
        }
        return ResultUtil.success(resultList);
    }

    private int getApply(Space space, List<String> spaceIds, String userId) {
        // Get token
        int apply = 0;
        if (StringUtils.isNotEmpty(userId)) {
            if (space.getApplyIs() == 1 || space.getIsPublic() == 1) {
                if (judgeMember(space.getAuthorizationList(), userId)) {
                    apply = 2;
                } else {
                    apply = spaceIds.contains(space.getSpaceId()) ? 1 : 0;
                }
            }
        }
        return apply;
    }

    private boolean judgeMember(Set<AuthorizationPerson> authorizationPersonList, String userId) {
        for (AuthorizationPerson authorizationPerson : authorizationPersonList) {
            if (authorizationPerson.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtain user ID
     */
    private String getUserId(HttpServletRequest request) {
        // Get token
        String token = jwtTokenUtils.getToken(request);
        if (StringUtils.isNotEmpty(token) && jwtTokenUtils.validateToken(token)) {
            return jwtTokenUtils.getUserIdFromToken(token);
        }
        return null;
    }

    private Query getQuery(String userId) {
        Query query = new Query();
        // if(StringUtils.isNotEmpty(userId)){
        // query.addCriteria(new Criteria().orOperator(Criteria.where("isPublic").is(1),Criteria.where("applyIs").is(1).and("authorizationList.userId").ne(userId)));
        // }else {
        // query.addCriteria(new Criteria().orOperator(Criteria.where("isPublic").is(1),Criteria.where("applyIs").is(1)));
        // }
        query.addCriteria(new Criteria().orOperator(Criteria.where("isPublic").is(1), Criteria.where("applyIs").is(1)));
        query.addCriteria(Criteria.where("state").is("1"));
        return query;
    }
}
