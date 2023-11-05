package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.asynchronous.AsyncDeal;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.email.EmailRole;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.service.SendEmailService;
import cn.cnic.dataspace.api.util.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

@Service
@Slf4j
@EnableAsync
public class SendEmailServiceImpl implements SendEmailService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AsyncDeal asyncDeal;

    @Resource
    private SpaceUrl spaceUrl;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    private final Cache<String, String> spaceInvite = CaffeineUtil.getSpaceInvite();

    @Override
    public void sendInviteEmail(String token, String userId, String spaceId, String role) {
        log.info("空间邮箱邀请: " + userId);
        if (userId == null || userId.equals("")) {
            return;
        }
        Token user = jwtTokenUtils.getToken(token);
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(userId));
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (one == null) {
            String ifPresent = spaceInvite.getIfPresent(userId);
            if (ifPresent == null) {
                return;
            }
            ConsumerDO consumerDO = new ConsumerDO();
            consumerDO.setState(2);
            consumerDO.setEmailAccounts(ifPresent);
            consumerDO.setCreateTime(LocalDateTime.now());
            consumerDO.setId(userId);
            consumerDO.setAddWay("空间邀请");
            one = mongoTemplate.save(consumerDO);
            spaceInvite.invalidate(userId);
            spaceInvite.invalidate(ifPresent);
        }
        // 0 not activated 1 activated 2 not registered 3
        int state = one.getState();
        String email = one.getEmailAccounts();
        // Send different links based on user status
        // Sending time
        long time = new Date().getTime();
        EmailModel emailType = EmailModel.EMAIL_SPACE_INVITE();
        String encryption = SMS4.Encryption(emailType.getType() + "&" + email + "&" + time + "&" + spaceId + "&" + user.getUserId() + "&" + role);
        // not active
        String code = "";
        if (state == 0) {
            // Activate Link
            code = spaceUrl.getEmailActivation() + encryption;
        } else if (state == 1) {
            code = spaceUrl.getSpaceDetailUrl().replaceAll("spaceId", spaceId);
        } else if (state == 2) {
            // Registration page
            code = spaceUrl.getWebRegister() + encryption + "&email=" + one.getEmailAccounts();
            emailType.setMessage(messageInternational("SEND_MESSAGE"));
            emailType.setAlert(messageInternational("SEND_ALERT"));
            emailType.setButton(messageInternational("SEND_BUTTON"));
            emailType.setAlertTo(messageInternational("SEND_ALERT_TO"));
            emailType.setEnd(messageInternational("SEND_SEND_TO"));
        }
        // Send code
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId)), Space.class);
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("name", user.getName());
        attachment.put("email", one.getEmailAccounts());
        attachment.put("url", code);
        emailType.setMessage(emailType.getMessage().replaceAll("email", user.getEmailAccounts()));
        emailType.setMessage(emailType.getMessage().replaceAll("spaceName", space.getSpaceName()));
        asyncDeal.send(attachment, emailType, EmailRole.SPACE_INVITE);
        return;
    }
}
