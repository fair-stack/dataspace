package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.bigdataprocessing.SpatialLogRule;
import cn.cnic.dataspace.api.exception.ExceptionType;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.util.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class SafetyServiceImpl {

    private final Cache<String, String> disable = CaffeineUtil.getDisable();

    private final Cache<String, Integer> errorPwd = CaffeineUtil.getErrorPwd();

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpatialLogRule spatialLogRule;

    public ResponseResult<Object> liftPwd(String email, HttpServletRequest request) {
        String token = jwtTokenUtils.getToken(request);
        if (StringUtils.isEmpty(token.trim())) {
            return ResultUtil.errorInternational("NEED_TOKEN");
        }
        if (StringUtils.isEmpty(email.trim())) {
            return ResultUtil.error(ExceptionType.MISSING_PARAMETERS);
        }
        Token admin = jwtTokenUtils.getToken(token);
        if (!admin.getRoles().contains(Constants.ADMIN)) {
            return ResultUtil.errorInternational("ACCESS_FORBIDDEN");
        }
        Query emailAccounts = new Query().addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO user = mongoTemplate.findOne(emailAccounts, ConsumerDO.class);
        if (user == null) {
            return ResultUtil.errorInternational("AUTH_USER_NOT_FOUND");
        } else if (user.getState() == 0) {
            return ResultUtil.errorInternational("USER_UNACTIVATED");
        } else if (user.getDisable() == 1) {
            return ResultUtil.errorInternational("SAFE_DISABLE");
        } else if (user.getState() == 2) {
            return ResultUtil.errorInternational("USER_UNREGISTERED");
        }
        if (!user.isDisablePwd()) {
            return ResultUtil.errorInternational("SAFE_LIMIT");
        }
        user.setDisablePwd(false);
        user.setDisablePwdTime(null);
        mongoTemplate.save(user);
        disable.invalidate(email);
        errorPwd.invalidate(email + "_login");
        return ResultUtil.success(CommonUtils.messageInternational("SAFE_LIFTED"), null);
    }

    /**
     * Manually synchronizing spatial file data
     */
    public ResponseResult<Object> synFile(String token, String spaceId) {
        Query query1 = new Query().addCriteria(Criteria.where("_id").is(spaceId));
        Space space = mongoTemplate.findOne(query1, Space.class);
        SpaceDataStatistic spaceDataStatistic = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), SpaceDataStatistic.class);
        spatialLogRule.spaceFileUpload(1, space, spaceDataStatistic);
        log.info("--- 任务结束");
        return ResultUtil.success();
    }
}
