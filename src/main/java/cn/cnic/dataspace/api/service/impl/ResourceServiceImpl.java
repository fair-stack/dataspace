package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.release.ResourceShow;
import cn.cnic.dataspace.api.service.ResourceService;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.ResponseResult;
import cn.cnic.dataspace.api.util.ResultUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public ResponseResult<Object> resourceSearch(String token, int page, int size, String spaceId, String resourceTitle) {
        if (StringUtils.isEmpty(spaceId)) {
            return ResultUtil.errorInternational("RESOURCE_SPACE_ID");
        }
        Query query = new Query();
        Criteria criteria = Criteria.where("type").is(2);
        criteria.and("spaceId").is(spaceId);
        if (StringUtils.isNotEmpty(resourceTitle)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(resourceTitle.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.and("titleCH").regex(pattern);
        }
        query.addCriteria(criteria);
        long count = mongoTemplate.count(query, ReleaseServiceImpl.COLLECTION_NAME);
        Space id = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId)), Space.class);
        List<ResourceShow> resourceShows = null;
        if (count > 0) {
            query.with(PageRequest.of(page - 1, size));
            query.with(Sort.by(Sort.Direction.DESC, "publicTime"));
            resourceShows = mongoTemplate.find(query, ResourceShow.class, ReleaseServiceImpl.COLLECTION_NAME);
            for (ResourceShow resourceShow : resourceShows) {
                resourceShow.setSpaceName(id.getSpaceName());
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("data", resourceShows);
        resultMap.put("total", count);
        return ResultUtil.success(resourceShows);
    }
}
