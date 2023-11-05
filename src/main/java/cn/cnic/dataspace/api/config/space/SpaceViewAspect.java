package cn.cnic.dataspace.api.config.space;

import cn.cnic.dataspace.api.model.space.RecentView;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.repository.RecentViewRepository;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import static cn.cnic.dataspace.api.util.CommonUtils.generateSnowflake;
import static cn.cnic.dataspace.api.util.CommonUtils.getCurrentDateTimeString;

/**
 * space recent view aspect
 *
 * @author wangCc
 * @date 2021-10-09 16:29
 */
@Aspect
@Component
@Slf4j
public class SpaceViewAspect {

    @Autowired
    private RecentViewRepository recentViewRepository;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // @Autowired
    // private QueueTaskConfig queueTask;
    // private static final BlockingQueue<String> BLOCKING_QUEUE = new LinkedBlockingQueue<>(10);
    // private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(5);
    @Pointcut("@annotation(cn.cnic.dataspace.api.config.space.View)")
    public void spaceRecent() {
    }

    @AfterReturning(value = "spaceRecent() && @annotation(view)")
    public void createLog(JoinPoint point, View view) {
        String spaceId = point.getArgs()[1].toString();
        String email = jwtTokenUtils.getEmail(point.getArgs()[0].toString());
        Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        List<RecentView> recentViewList = recentViewRepository.findByEmailAndSpaceId(email, spaceId);
        // remove this older one and insert new
        if (recentViewList.size() > 0) {
            recentViewRepository.deleteAll(recentViewList);
        }
        if (spaceOptional.isPresent() && StringUtils.isNotBlank(email)) {
            Space space = spaceOptional.get();
            recentViewRepository.save(RecentView.builder().recentId(generateSnowflake()).spaceName(space.getSpaceName()).spaceLogo(space.getSpaceLogo()).homeUrl(space.getHomeUrl()).dateTime(getCurrentDateTimeString()).email(email).spaceId(spaceId).build());
            // if (space.getIsPublic() == 1) {
            // viewCount(spaceId, space.getHomeUrl());
            // }
        }
        recentViewRepository.deleteAll(mongoTemplate.find(new Query().addCriteria(Criteria.where("email").is(email)).skip(10).with(Sort.by(Sort.Direction.DESC, "dateTime")), RecentView.class));
    }
    // /**
    // * space view count increase
    // */
    // @SneakyThrows
    // public void viewCount(String spaceId, String homeUrl) {
    // queueTask.setBlockingQueue(BLOCKING_QUEUE);
    // BLOCKING_QUEUE.put(Statistic.TYPE_VIEW);
    // queueTask.setSpaceId(spaceId);
    // queueTask.setHomeUrl(homeUrl);
    // EXECUTOR_SERVICE.execute(queueTask);
    // }
}
