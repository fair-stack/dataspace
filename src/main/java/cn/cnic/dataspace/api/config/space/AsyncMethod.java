package cn.cnic.dataspace.api.config.space;

import cn.cnic.dataspace.api.model.apply.Apply;
import cn.cnic.dataspace.api.model.apply.SpaceApply;
import cn.cnic.dataspace.api.model.harvest.ShareLink;
import cn.cnic.dataspace.api.model.harvest.FTPShort;
import cn.cnic.dataspace.api.model.space.*;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.email.EmailRole;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.harvest.FtpUser;
import cn.cnic.dataspace.api.model.harvest.MiningTask;
import cn.cnic.dataspace.api.model.harvest.TaskFileImp;
import cn.cnic.dataspace.api.model.statistics.SpaceDataInStatistic;
import cn.cnic.dataspace.api.model.statistics.SpaceDataOutStatistic;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.statistics.SpaceStatistic;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.model.user.Message;
import cn.cnic.dataspace.api.repository.RecentViewRepository;
import cn.cnic.dataspace.api.service.space.SpaceService;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.SpaceUrl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * AsyncMethod
 *
 * @author wangCc
 * @date 2021-11-02 16:09
 */
@Slf4j
@Component
public class AsyncMethod {

    @Resource
    private MongoTemplate mongoTemplate;

    private final RecentViewRepository recentViewRepository;

    @Autowired
    public AsyncMethod(RecentViewRepository recentViewRepository) {
        this.recentViewRepository = recentViewRepository;
    }

    /**
     * Delete User
     */
    public void deleteUser(ConsumerDO consumerDO, SpaceUrl spaceUrl) {
        String userId = consumerDO.getId();
        Query query = new Query().addCriteria(Criteria.where("_id").is(userId));
        // delete user
        mongoTemplate.remove(query, ConsumerDO.class);
        // Remove spatial information
        Query query1 = new Query().addCriteria(Criteria.where("authorizationList.userId").is(userId));
        List<Space> spaces = mongoTemplate.find(query1, Space.class);
        for (Space space : spaces) {
            if (space.getState().equals("0")) {
                mongoTemplate.remove(space);
            } else {
                Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
                Iterator<AuthorizationPerson> iterator = authorizationList.iterator();
                while (iterator.hasNext()) {
                    AuthorizationPerson next = iterator.next();
                    if (next.getUserId().equals(userId)) {
                        iterator.remove();
                    }
                }
                space.setAuthorizationList(authorizationList);
                mongoTemplate.save(space);
            }
        }
        // Remove Pending Approval
        Query query2 = new Query().addCriteria(Criteria.where("approvedStates").is("待审批").and("applicant.personId").is(userId));
        long count = mongoTemplate.count(query2, Apply.class);
        if (count > 0) {
            mongoTemplate.remove(query2, Apply.class);
        }
        Query userIdQuery = new Query().addCriteria(Criteria.where("userId").is(userId));
        mongoTemplate.remove(userIdQuery, EmailRole.class);
        // FTP Short Chain - Space Sharing
        List<FTPShort> ftpShorts = mongoTemplate.find(userIdQuery, FTPShort.class);
        for (FTPShort ftpShort : ftpShorts) {
            if (StringUtils.isNotEmpty(ftpShort.getUserId())) {
                mongoTemplate.remove(new Query().addCriteria(Criteria.where("_id").is(ftpShort.getUserId())), FtpUser.class);
            }
        }
        mongoTemplate.remove(userIdQuery, FTPShort.class);
        mongoTemplate.remove(new Query().addCriteria(Criteria.where("applicant.personId").is(userId)), Message.class);
        mongoTemplate.remove(new Query().addCriteria(Criteria.where("email").is(consumerDO.getEmailAccounts())), RecentView.class);
        mongoTemplate.remove(new Query().addCriteria(Criteria.where("founder.userId").is(userId)), ShareLink.class);
        // Space file import
        List<MiningTask> miningTasks = mongoTemplate.find(userIdQuery, MiningTask.class);
        for (MiningTask miningTask : miningTasks) {
            mongoTemplate.remove(new Query().addCriteria(Criteria.where("rootId").is(miningTask.getTaskId())), TaskFileImp.class);
        }
        mongoTemplate.remove(userIdQuery, MiningTask.class);
        // Delete user image
        String avatar = consumerDO.getAvatar();
        if (StringUtils.isNotEmpty(avatar) && avatar.contains("/")) {
            try {
                Files.delete(new File(spaceUrl.getRootDir() + avatar).toPath());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Delete Space
     */
    @Async
    public void deleteSpace(Space space, SpaceUrl spaceUrl) {
        Query query = new Query().addCriteria(Criteria.where("spaceId").is(space.getSpaceId()));
        // Delete Space
        mongoTemplate.remove(new Query().addCriteria(Criteria.where("_id").is(space.getSpaceId())), Space.class);
        // Delete files
        deleteSpaceFile(space.getFilePath(), space.getSpaceId());
        // delete space log
        deleteSpaceLog(space.getSpaceId());
        // Delete Statistics
        mongoTemplate.remove(query, SpaceDataStatistic.class);
        mongoTemplate.remove(query, SpaceDataInStatistic.class);
        mongoTemplate.remove(query, SpaceDataOutStatistic.class);
        mongoTemplate.remove(query, SpaceStatistic.class);
        // FTP Short Chain - Space Sharing
        // List<FTPShort> ftpShorts = mongoTemplate.find(query, FTPShort.class);
        // for (FTPShort ftpShort : ftpShorts) {
        // if(StringUtils.isNotEmpty(ftpShort.getUserId())){
        // mongoTemplate.remove(new Query().addCriteria(Criteria.where("_id").is(ftpShort.getUserId())), FtpUser.class);
        // }
        // }
        mongoTemplate.remove(query, FTPShort.class);
        // Share Connection
        mongoTemplate.remove(query, ShareLink.class);
        // Space application information
        mongoTemplate.remove(query, SpaceApply.class);
        // Space permissions
        mongoTemplate.remove(query, SpaceRole.class);
        // Space file import
        List<MiningTask> miningTasks = mongoTemplate.find(query, MiningTask.class);
        for (MiningTask miningTask : miningTasks) {
            mongoTemplate.remove(new Query().addCriteria(Criteria.where("rootId").is(miningTask.getTaskId())), TaskFileImp.class);
        }
        mongoTemplate.remove(query, MiningTask.class);
        // Delete space image
        String avatar = space.getSpaceLogo();
        if (StringUtils.isNotEmpty(avatar) && avatar.contains("/")) {
            try {
                Files.delete(new File(spaceUrl.getRootDir() + avatar).toPath());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Clean up file and database mappings
     */
    private void deleteSpaceFile(String filePath, String spaceId) {
        File file = new File(filePath);
        log.info("delete space file path {}", file.getAbsolutePath());
        if (file.isDirectory() && file.exists()) {
            try {
                if (!file.delete()) {
                    FileUtils.deleteDirectory(file);
                }
            } catch (IOException e) {
                throw new RuntimeException(CommonUtils.messageInternational("INTERNAL_ERROR"));
            }
        }
        // delete mapping
        mongoTemplate.remove(new Query(), FileMapping.class, spaceId);
    }

    /**
     * delete space log
     */
    private void deleteSpaceLog(String spaceId) {
        mongoTemplate.remove(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), SpaceSvnLog.class);
        // recent view delete
        recentViewRepository.deleteBySpaceId(spaceId);
    }

    /**
     * schedule for clear map cache
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void clear() {
        SpaceService.limitMap.clear();
        log.info("the system has cleared cache of creating space ...");
    }
}
