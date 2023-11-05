package cn.cnic.dataspace.api.elfinder.command;

import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.repository.SvnSpaceLogRepository;
import cn.cnic.dataspace.api.repository.UserRepository;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import com.google.common.util.concurrent.RateLimiter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.*;
import static cn.cnic.dataspace.api.util.CommonUtils.generateSnowflake;
import static cn.cnic.dataspace.api.util.CommonUtils.getCurrentDateTimeString;

/**
 * common service for elfinder plugins
 *
 * @author wangCc
 * @date 2021-4-19 16:06:20
 */
@Component
public class ElfinderCommonService {

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    // @Autowired
    // private QueueTaskConfig queueTask;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SvnSpaceLogRepository svnSpaceLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    // private static final BlockingQueue<String> BLOCKING_QUEUE = new LinkedBlockingQueue<>(20);
    // private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(5);
    private static final Map<String, RateLimiter> DOWNLOAD_RATE_LIMITER_MAP = new ConcurrentHashMap<>(256);

    /**
     * sensitive check
     */
    public boolean sensitive(String word) {
        return false;
    }

    /**
     * delete
     */
    public boolean delete(HttpServletRequest request) {
        String userId = jwtTokenUtils.getUserIdFromToken(jwtTokenUtils.getToken(request));
        Space space = spaceRepository.findById(request.getParameter("spaceId")).get();
        boolean flag = false;
        for (AuthorizationPerson authorizationPerson : space.getAuthorizationList()) {
            if (StringUtils.equals(authorizationPerson.getUserId(), userId) || StringUtils.equals(userId, space.getUserId())) {
                flag = true;
            }
            break;
        }
        return flag;
    }

    /**
     * download statistic
     */
    // @SneakyThrows
    // public void download(String spaceId) {
    // BLOCKING_QUEUE.put(Statistic.TYPE_DOWNLOAD);
    // queueTask.setSpaceId(spaceId);
    // queueTask.setHomeUrl(spaceId);
    // queueTask.setBlockingQueue(BLOCKING_QUEUE);
    // EXECUTOR_SERVICE.execute(queueTask);
    // }
    @SneakyThrows
    public void download(String spaceId, String key, long value) {
        spaceControlConfig.spaceStat(spaceId, key, value);
    }

    @SneakyThrows
    public void stateDownload(String spaceId, long value) {
        spaceControlConfig.dataOut("web", value, spaceId);
    }

    /**
     * get space path
     */
    String spacePath(String spaceId) {
        return mongoTemplate.find(new Query().addCriteria(new Criteria().orOperator(Criteria.where("spaceId").is(spaceId), Criteria.where("homeUrl").is(spaceId))), Space.class).get(0).getFilePath();
    }

    /**
     * compress log record
     */
    public void compressLog(HttpServletRequest request, Target targetArchive) {
        if (StringUtils.isNotBlank(jwtTokenUtils.getToken(request))) {
            final String userId = jwtTokenUtils.getUserIdFromToken(jwtTokenUtils.getToken(request));
            final ConsumerDO consumerDO = userRepository.findById(userId).get();
            final String name = targetArchive.getVolume().getName(targetArchive);
            svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(request.getParameter("spaceId")).action(SpaceSvnLog.ACTION_FILE).description(consumerDO.getName() + " 压缩了文件（夹）" + name).operatorId(userId).operator(new Operator(consumerDO)).method(SpaceSvnLog.ELFINDER).version(-2).updateDateTime(getCurrentDateTimeString()).build());
        }
    }

    /**
     * decompress log record
     */
    public void deCompressLog(HttpServletRequest request, Target targetCompressed) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(jwtTokenUtils.getToken(request));
        final ConsumerDO consumerDO = userRepository.findById(userIdFromToken).get();
        final String name = targetCompressed.getVolume().getName(targetCompressed);
        svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(request.getParameter("spaceId")).action(SpaceSvnLog.ACTION_FILE).description(consumerDO.getName() + " 解压了文件 " + name).operatorId(userIdFromToken).operator(new Operator(consumerDO)).method(SpaceSvnLog.ELFINDER).version(-2).updateDateTime(getCurrentDateTimeString()).build());
    }

    /**
     * download rate limiter
     */
    public boolean downloadRateLimiter(HttpServletRequest request) {
        String ipAddr = CommonUtils.getIpAddr(request);
        System.out.println("ipAddr = " + ipAddr);
        boolean flag = false;
        if (StringUtils.isNotBlank(ipAddr)) {
            if (DOWNLOAD_RATE_LIMITER_MAP.containsKey(ipAddr)) {
                flag = DOWNLOAD_RATE_LIMITER_MAP.get(ipAddr).tryAcquire();
            } else {
                DOWNLOAD_RATE_LIMITER_MAP.put(ipAddr, RateLimiter.create(2, 1, TimeUnit.SECONDS));
                flag = true;
            }
        }
        return flag;
    }
}
