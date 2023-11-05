package cn.cnic.dataspace.api.asynchronous;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.model.AccessRecord;
import cn.cnic.dataspace.api.model.IpInfo;
import cn.cnic.dataspace.api.model.apply.SpaceApply;
import cn.cnic.dataspace.api.model.backup.BackupSpaceMain;
import cn.cnic.dataspace.api.model.backup.BackupSpaceSubtasks;
import cn.cnic.dataspace.api.model.email.EmailRole;
import cn.cnic.dataspace.api.model.email.ToEmail;
import cn.cnic.dataspace.api.model.apply.Apply;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.harvest.ShareLink;
import cn.cnic.dataspace.api.model.open.Application;
import cn.cnic.dataspace.api.model.space.RecentView;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.user.Message;
import cn.cnic.dataspace.api.quartz.QuartzManager;
import cn.cnic.dataspace.api.util.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class AsyncDeal {

    @Autowired
    private EmailUtils emailUtils;

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private CacheLoading cacheLoading;

    private final Cache<String, Object> spaceStatistic = CaffeineUtil.getSpaceStatistic();

    /**
     * Asynchronous email sending
     */
    @Async
    public void send(Map<String, Object> map, EmailModel emailType, String emailRole) {
        List<String> emailList = new ArrayList<>(10);
        try {
            emailList.add((String) map.get("email"));
        } catch (Exception e) {
            String[] emails = (String[]) map.get("email");
            emailList.addAll(Arrays.asList(emails));
        }
        // check
        if (Arrays.asList(EmailRole.list).contains(emailRole)) {
            Iterator<String> iterator = emailList.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                List<String> userEmailRole = cacheLoading.getUserEmailRole(next);
                if (!userEmailRole.contains(emailRole)) {
                    log.info("用户已配置该类邮件接收通知关闭：{} " + next);
                    iterator.remove();
                }
            }
        }
        if (emailList.isEmpty()) {
            return;
        }
        ToEmail email = new ToEmail();
        email.setTos(emailList.toArray(new String[emailList.size()]));
        Map<String, Object> objectMap = (Map) cacheLoading.loadingConfig();
        String dataSpaceName = objectMap.get("dataSpaceName").toString();
        if (!emailType.getSubject().contains(dataSpaceName)) {
            emailType.setSubject("【" + dataSpaceName + emailType.getSubject());
        }
        email.setSubject(emailType.getSubject());
        map.put("org", dataSpaceName);
        // Encapsulation template information
        map.put("title", emailType.getTitle());
        String message = emailType.getMessage().replaceAll("name", map.get("name").toString()).replaceAll("email", map.get("email").toString()).replaceAll("dataSpaceName", dataSpaceName);
        String call = emailType.getCall().replaceAll("name", map.get("name").toString()).replaceAll("email", map.get("email").toString()).replaceAll("dataSpaceName", dataSpaceName);
        map.put("call", call);
        map.put("message", message);
        map.put("button", emailType.getButton());
        map.put("alert", emailType.getAlert());
        map.put("alertTo", emailType.getAlertTo());
        map.put("end", emailType.getEnd());
        map.put("copyright", objectMap.get("copyright").toString());
        map.put("homeUrl", spaceUrl.getWebUrl());
        emailUtils.sendTemplateMail(email, map, emailType.getTemplate());
        return;
    }

    /**
     * Test email sending - administrator
     */
    public boolean emailSend(ToEmail toEmail, Map<String, Object> map, EmailModel emailModel) {
        return emailUtils.sendTemplateMail(toEmail, map, emailModel.getTemplate());
    }

    @Async
    public void accessStatistics(String ipAddr, Update update, String email) {
        // Query IP
        int currentYear = CommonUtils.getCurrentYearTo();
        int currentMonth = CommonUtils.getCurrentMonth();
        int currentDay = CommonUtils.getCurrentDay();
        Query query = new Query().addCriteria(Criteria.where("accessIP").is(ipAddr).and("email").is(email));
        Criteria criteria = Criteria.where("year").is(currentYear);
        criteria.and("month").is(currentMonth);
        criteria.and("day").is(currentDay);
        query.addCriteria(criteria);
        update.setOnInsert("accessIP", ipAddr);
        update.setOnInsert("accessTime", new Date());
        update.set("lastTime", new Date());
        update.inc("accCount", 1);
        update.setOnInsert("year", currentYear);
        update.setOnInsert("month", currentMonth);
        update.setOnInsert("day", currentDay);
        IpInfo ipInfoBean = Ip2regionAnalysis.getInstance().getIpInfoBean(ipAddr);
        if (null != ipInfoBean) {
            update.setOnInsert("country", ipInfoBean.getCountry());
            update.setOnInsert("province", ipInfoBean.getProvince());
            update.setOnInsert("city", ipInfoBean.getCity());
            update.setOnInsert("isp", ipInfoBean.getIsp());
        }
        mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), AccessRecord.class);
        synchronized (this) {
            Object spaceDownSize = spaceStatistic.getIfPresent("accTotal");
            if (spaceDownSize != null) {
                spaceStatistic.put("accTotal", (long) spaceDownSize + 1);
            }
        }
    }

    /**
     * Asynchronous modification of user information
     */
    @Async
    public void userinfoUpdate(String userId, String avatar, String name) {
        // Modifying Space Members
        List<Space> spaces = mongoTemplate.find(new Query().addCriteria(Criteria.where("authorizationList.userId").is(userId)), Space.class);
        List<String> spaceIds = new ArrayList<>(spaces.size());
        for (Space space : spaces) {
            Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
            for (AuthorizationPerson authorizationPerson : authorizationList) {
                String id = authorizationPerson.getUserId();
                if (userId.equals(id)) {
                    if (avatar != null) {
                        authorizationPerson.setAvatar(avatar);
                    }
                    if (name != null) {
                        authorizationPerson.setUserName(name);
                    }
                    break;
                }
            }
            space.setAuthorizationList(authorizationList);
            mongoTemplate.save(space);
            spaceIds.add(space.getSpaceId());
        }
        if (StringUtils.isNotEmpty(name)) {
            // Modify space file author
            for (String spaceId : spaceIds) {
                Query query = new Query().addCriteria(Criteria.where("author.personId").is(userId));
                long count = mongoTemplate.count(query, FileMapping.class, spaceId);
                if (count > 0) {
                    Update update = new Update();
                    update.set("author.personName", name);
                    try {
                        mongoTemplate.updateMulti(query, update, FileMapping.class, spaceId);
                    } catch (Exception e) {
                        log.info("-----空间文件作者 用户信息 同步修改用户名称 失败 ------ {}  " + e.getLocalizedMessage());
                    }
                }
            }
            // Modify Approval Page
            List<Apply> applyList = mongoTemplate.find(new Query().addCriteria(Criteria.where("applicant.personId").is(userId)), Apply.class);
            for (Apply apply : applyList) {
                // applicant
                String content = apply.getContent();
                String userCont = getUserCont(content, name);
                apply.setContent(userCont);
                mongoTemplate.save(apply);
            }
            List<SpaceApply> spaceApplyList = mongoTemplate.find(new Query().addCriteria(Criteria.where("applicant.userId").is(userId)), SpaceApply.class);
            for (SpaceApply apply : spaceApplyList) {
                // applicant
                String content = apply.getTitle();
                String userCont = getUserCont(content, name);
                apply.setTitle(userCont);
                mongoTemplate.save(apply);
            }
            // authorization
            List<Application> applicationList = mongoTemplate.find(new Query().addCriteria(Criteria.where("person.personId").is(userId)), Application.class);
            for (Application application : applicationList) {
                application.getPerson().setPersonName(name);
                mongoTemplate.save(application);
            }
            // Auditing
            updateInfo("applicant.personId", "applicant.personName", userId, name, Apply.class);
            updateInfo("approver.personId", "approver.personName", userId, name, Apply.class);
            // news
            updateInfo("applicant.personId", "applicant.personName", userId, name, Message.class);
            updateInfo("approver.personId", "approver.personName", userId, name, Message.class);
            // Share Link
            updateInfo("founder.userId", "founder.userName", userId, name, ShareLink.class);
            // Space Log
            updateInfo("operator.personId", "operator.personName", userId, name, SpaceSvnLog.class);
            // Space Join Approval
            updateInfo("applicant.userId", "applicant.userName", userId, name, SpaceApply.class);
            updateInfo("approver.userId", "approver.userName", userId, name, SpaceApply.class);
            // Data backup
            updateInfo("person.userId", "person.userName", userId, name, BackupSpaceMain.class);
        }
    }

    @Async
    public void spaceInfoUpdate(String spaceId, String spaceName, String des, List<String> tag) {
        // Space Approval
        List<Apply> applyList = mongoTemplate.find(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), Apply.class);
        for (Apply apply : applyList) {
            boolean judge = false;
            if (StringUtils.isNotEmpty(spaceName)) {
                String content = apply.getContent();
                apply.setContent(getSpaceCont(content, spaceName));
                apply.setSpaceName(spaceName);
                judge = true;
            }
            if (StringUtils.isNotEmpty(des)) {
                apply.setSpaceDescription(des);
                judge = true;
            }
            if (null != tag) {
                apply.setSpaceTag(tag);
                judge = true;
            }
            if (judge) {
                mongoTemplate.save(apply);
            }
        }
        if (StringUtils.isNotEmpty(spaceName)) {
            // Browsing Records
            updateInfo("spaceId", "spaceName", spaceId, spaceName, RecentView.class);
            // Share Link
            updateInfo("content.id", "content.name", spaceId, spaceName, ShareLink.class);
            // Spatial Statistics
            updateInfo("spaceId", "spaceName", spaceId, spaceName, SpaceDataStatistic.class);
            // Data backup
            updateInfo("spaceId", "spaceName", spaceId, spaceName, BackupSpaceMain.class);
        }
    }

    private String getSpaceCont(String str, String name) {
        if (!str.contains("（")) {
            return str;
        }
        int i = str.lastIndexOf("（");
        String substring = str.substring(0, i);
        return substring + "（" + name + "）";
    }

    private String getUserCont(String str, String name) {
        if (!str.contains("（")) {
            return str;
        }
        int i = str.indexOf("（");
        String substring = str.substring(i);
        return "用户 " + name + substring;
    }

    /**
     * Unified modification
     */
    private void updateInfo(String queryKey, String updateKey, String queryValue, String updateValue, Class cla) {
        Query query = new Query().addCriteria(Criteria.where(queryKey).is(queryValue));
        long count = mongoTemplate.count(query, cla);
        if (count > 0) {
            Update update = new Update();
            update.set(updateKey, updateValue);
            try {
                mongoTemplate.updateMulti(query, update, cla);
            } catch (Exception e) {
                log.info("----- 用户信息 同步修改用户名称 失败 ------ {}  " + e.getLocalizedMessage());
            }
        }
    }

    @Async
    public void deleteBackupTask(String spaceId, boolean judge) {
        BackupSpaceMain backupSpaceMain = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), BackupSpaceMain.class);
        if (backupSpaceMain != null) {
            // Stop scheduled tasks
            if (backupSpaceMain.getStatus().equals(Constants.Backup.START)) {
                String triggerState = QuartzManager.getTriggerState(backupSpaceMain.getJobId(), spaceId);
                if (!triggerState.equals("NONE")) {
                    // suspend
                    QuartzManager.pauseJob(backupSpaceMain.getJobId(), spaceId);
                    // remove
                    QuartzManager.removeJob(backupSpaceMain.getJobId(), spaceId);
                }
            }
            if (judge) {
                backupSpaceMain.setStatus(Constants.Backup.STOP);
                mongoTemplate.save(backupSpaceMain);
            } else {
                mongoTemplate.remove(backupSpaceMain);
                Query query = new Query().addCriteria(Criteria.where("jobId").is(backupSpaceMain.getJobId()));
                mongoTemplate.remove(query, BackupSpaceSubtasks.class);
            }
        }
    }

    @Async
    public void updateBackupTask(AuthorizationPerson person, String spaceId) {
        BackupSpaceMain backupSpaceMain = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), BackupSpaceMain.class);
        if (backupSpaceMain != null) {
            boolean judge = false;
            AuthorizationPerson sourcePerson = backupSpaceMain.getPerson();
            if (null != person && !sourcePerson.getEmail().equals(person.getEmail())) {
                backupSpaceMain.setPerson(person);
                judge = true;
            }
            if (judge) {
                mongoTemplate.save(backupSpaceMain);
            }
        }
    }
}
