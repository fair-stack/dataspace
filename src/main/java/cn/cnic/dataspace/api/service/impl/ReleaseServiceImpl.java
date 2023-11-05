package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.manage.ReleaseAccount;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.release.ResourceShow;
import cn.cnic.dataspace.api.model.release.ResourceV2;
import cn.cnic.dataspace.api.model.release.Subject;
import cn.cnic.dataspace.api.service.ReleaseService;
import cn.cnic.dataspace.api.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@EnableAsync
public class ReleaseServiceImpl implements ReleaseService {

    @Autowired
    private MongoTemplate mongoTemplate;

    private final Cache<String, String> publicModel = CaffeineUtil.getPublicModel();

    public static final String COLLECTION_NAME = "resource_v2";

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private SpaceUrl spaceUrl;

    @Override
    public ResponseResult<Object> releaseSearch(String token, int page, int size, int state, String releaseName) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        // UpdateState (username)// Update Status
        Criteria criteria = Criteria.where("founderId").is(userIdFromToken);
        criteria.and("type").is(state);
        if (StringUtils.isNotEmpty(releaseName)) {
            Pattern name = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(releaseName.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.and("titleCH").is(name);
        }
        Query query = new Query();
        query.addCriteria(criteria);
        long count = mongoTemplate.count(query, COLLECTION_NAME);
        List<ResourceShow> resources = null;
        if (count > 0) {
            query.with(PageRequest.of(page - 1, size));
            if (state == Constants.DRAFT) {
                query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
            } else if (state == Constants.AUDIT) {
                query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
            } else if (state == Constants.REJECT) {
                query.with(Sort.by(Sort.Direction.DESC, "dismissTime"));
            } else {
                query.with(Sort.by(Sort.Direction.DESC, "publicTime"));
            }
            resources = mongoTemplate.find(query, ResourceShow.class, COLLECTION_NAME);
            for (ResourceShow resource : resources) {
                Space id = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(resource.getSpaceId())), Space.class);
                if (id == null) {
                    resource.setSpaceName(CommonUtils.messageInternational("RELEASE_SPACE_DELETE"));
                } else {
                    resource.setSpaceName(id.getSpaceName());
                }
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("data", resources);
        resultMap.put("total", count);
        return ResultUtil.success(resultMap);
    }

    @Override
    public ResponseResult<Object> getVersion(String resourceId) {
        if (StringUtils.isEmpty(resourceId)) {
            return ResultUtil.success(Constants.VERSION);
        }
        Query query = new Query().addCriteria(Criteria.where("resourceId").is(resourceId).and("latest").is(true));
        ResourceV2 one = mongoTemplate.findOne(query, ResourceV2.class, ReleaseServiceImpl.COLLECTION_NAME);
        if (one == null) {
            return ResultUtil.success(Constants.VERSION);
        }
        String version = one.getVersion();
        String substring = version.substring(1, 2);
        int v = Integer.parseInt(substring) + 1;
        String newVersion = "V" + v;
        Query judgeQuery = new Query().addCriteria(Criteria.where("resourceId").is(resourceId).and("version").is(newVersion).and("type").is(Constants.AUDIT));
        long count = mongoTemplate.count(judgeQuery, COLLECTION_NAME);
        if (count > 0) {
            return ResultUtil.errorInternational("RELEASE_AUDIT");
        }
        return ResultUtil.success(newVersion);
    }

    @Override
    public ResponseResult<Object> getSubjectList(String param) {
        String subjectList = publicModel.getIfPresent("subject");
        if (StringUtils.isEmpty(subjectList)) {
            Map<String, Object> map = new HashMap<>();
            Map<String, Object> oneSubjectMap = new HashMap<>();
            List<Subject> all = mongoTemplate.findAll(Subject.class);
            all.stream().forEachOrdered(subject -> {
                String oneName = subject.getOne_rank_name();
                if (map.containsKey(oneName)) {
                    Map twoMap = (Map<String, Object>) map.get(oneName);
                    String two_rank_name = subject.getTwo_rank_name();
                    if (twoMap.containsKey(two_rank_name)) {
                        ((List) twoMap.get(two_rank_name)).add(subject.getThree_rank_name());
                    } else {
                        twoMap.put(two_rank_name, new ArrayList<String>() {

                            {
                                add(subject.getThree_rank_name());
                            }
                        });
                    }
                } else {
                    Map<String, List<String>> twoMap = new HashMap<>();
                    twoMap.put(subject.getTwo_rank_name(), new ArrayList<String>() {

                        {
                            add(subject.getThree_rank_name());
                        }
                    });
                    map.put(oneName, twoMap);
                    Map<String, String> subMap = new HashMap<>();
                    subMap.put("code", subject.getOne_rank_no());
                    subMap.put("enName", "");
                    oneSubjectMap.put(oneName, subMap);
                }
            });
            List<Map<String, Object>> resultList = new LinkedList<>();
            map.entrySet().stream().forEachOrdered(sub -> {
                String key = sub.getKey();
                List<Map<String, Object>> twoList = new LinkedList<>();
                Map<String, Object> twoMap = (Map<String, Object>) sub.getValue();
                twoMap.entrySet().stream().forEachOrdered(subTwo -> {
                    String key1 = subTwo.getKey();
                    Map<String, Object> threeMap = new HashMap<>();
                    threeMap.put("value", key1);
                    threeMap.put("label", key1);
                    List<String> list = (List<String>) subTwo.getValue();
                    List<Map<String, Object>> threeList = new LinkedList<>();
                    Iterator<String> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        String next = iterator.next();
                        Map<String, Object> mm = new HashMap<>();
                        mm.put("value", next);
                        mm.put("label", next);
                        threeList.add(mm);
                    }
                    threeMap.put("children", threeList);
                    twoList.add(threeMap);
                });
                Map<String, Object> oneMap = new HashMap<>();
                oneMap.put("label", key);
                oneMap.put("value", key);
                oneMap.put("children", twoList);
                resultList.add(oneMap);
            });
            String s = JSON.toJSONString(resultList);
            publicModel.put("subject", s);
            publicModel.put("querySubject", JSON.toJSONString(oneSubjectMap));
            return ResultUtil.success(resultList);
        }
        List list = JSONObject.parseObject(subjectList, List.class);
        return ResultUtil.success(list);
    }

    @Override
    public ResponseResult<Object> resourceUpdate(String data) {
        if (StringUtils.isEmpty(data)) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        Map<String, Object> map = null;
        try {
            map = JSONObject.parseObject(data, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.errorInternational("RELEASE_FORMAT_ERROR");
        }
        if (map == null) {
            return ResultUtil.errorInternational("RELEASE_FORMAT_ERROR");
        }
        String resourcesOnlyId = map.get("resourcesId").toString();
        String version = map.get("version").toString();
        Query query = new Query().addCriteria(Criteria.where("traceId").is(resourcesOnlyId).and("version").is(version).and("type").is(Constants.AUDIT));
        ResourceV2 resource = mongoTemplate.findOne(query, ResourceV2.class);
        if (resource == null) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        return ResultUtil.success();
    }

    @Override
    public ResponseResult<Object> resourceDelete(String token, String id) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        ResourceV2 one = mongoTemplate.findOne(query, ResourceV2.class);
        if (!one.getFounderId().equals(userIdFromToken)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        if (one != null && one.getType() == Constants.DRAFT) {
            mongoTemplate.remove(query, COLLECTION_NAME);
        }
        // Delete published images
        String avatar = one.getImage();
        if (StringUtils.isNotEmpty(avatar) && avatar.contains("/")) {
            try {
                Files.delete(new File(spaceUrl.getRootDir() + avatar).toPath());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        return ResultUtil.success();
    }

    /**
     * Determine if the publishing institution has a publishing account
     */
    @Override
    public ResponseResult<Object> judgeRelease(String orgId) {
        Query query = new Query().addCriteria(Criteria.where("orgId").is(orgId));
        ReleaseAccount one = mongoTemplate.findOne(query, ReleaseAccount.class);
        return ResultUtil.success(null != one);
    }

    @Override
    public ResponseResult<Object> releaseCount(String token) {
        String username = jwtTokenUtils.getUserIdFromToken(token);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("0", mongoTemplate.count(new Query().addCriteria(Criteria.where("founderId").is(username).and("type").is(0)), COLLECTION_NAME));
        resultMap.put("1", mongoTemplate.count(new Query().addCriteria(Criteria.where("founderId").is(username).and("type").is(1)), COLLECTION_NAME));
        resultMap.put("2", mongoTemplate.count(new Query().addCriteria(Criteria.where("founderId").is(username).and("type").is(2)), COLLECTION_NAME));
        resultMap.put("3", mongoTemplate.count(new Query().addCriteria(Criteria.where("founderId").is(username).and("type").is(3)), COLLECTION_NAME));
        return ResultUtil.success(resultMap);
    }

    /**
     * Version verification
     */
    public void judgeVersion(String resourceId, String version) {
        // Version verification
        ResponseResult<Object> responseResult = getVersion(resourceId);
        int code = responseResult.getCode();
        if (code != 0) {
            throw new CommonException(-1, responseResult.getMessage().toString());
        }
        if (!version.equals(responseResult.getData().toString())) {
            throw new CommonException(-1, CommonUtils.messageInternational("RELEASE_VERSION_ERROR"));
        }
    }
}
