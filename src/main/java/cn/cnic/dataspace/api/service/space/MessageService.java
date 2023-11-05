package cn.cnic.dataspace.api.service.space;

import cn.cnic.dataspace.api.model.user.Message;
import cn.cnic.dataspace.api.model.space.child.Person;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.repository.MessageRepository;
import cn.cnic.dataspace.api.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.util.*;
import static cn.cnic.dataspace.api.util.CommonUtils.generateSnowflake;
import static cn.cnic.dataspace.api.util.CommonUtils.getCurrentDateTimeString;

/**
 * message record service
 *
 * @author wangCc
 * @date 2021-11-20 13:25
 */
@Component
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * applicant send to admin
     */
    public void sendToAdmin(String title, String content, ConsumerDO applicant, String linkUrl) {
        messageRepository.save(Message.builder().messageId(generateSnowflake()).title(title).content(content).applicant(new Person(applicant)).msgDate(getCurrentDateTimeString()).linkUrl(linkUrl).adminMsg(1).haveRead(0).build());
    }

    /**
     * feedback to the applicant for admin
     */
    public void sendToApplicant(String title, String content, Person approver, Person applicant, int result) {
        messageRepository.save(Message.builder().messageId(generateSnowflake()).title(title).content(content).applicant(applicant).approver(approver).msgDate(getCurrentDateTimeString()).result(result).adminMsg(0).haveRead(0).build());
    }

    /**
     * send applicant without admin
     */
    public void sendToApplicant(String title, String content, Person operator, Person applicant, String linkUrl) {
        messageRepository.save(Message.builder().messageId(generateSnowflake()).title(title).content(content).applicant(applicant).approver(operator).msgDate(getCurrentDateTimeString()).linkUrl(linkUrl).adminMsg(0).haveRead(0).build());
    }

    /**
     * send applicant with result
     */
    public void sendToApplicant(String title, String content, Person applicant, int result) {
        messageRepository.save(Message.builder().messageId(generateSnowflake()).title(title).content(content).applicant(applicant).msgDate(getCurrentDateTimeString()).result(result).adminMsg(0).haveRead(0).build());
    }

    /**
     * send applicant with result - linkUrl
     */
    public void sendToApplicant(String title, String content, Person applicant, int result, String linkUrl) {
        messageRepository.save(Message.builder().messageId(generateSnowflake()).title(title).content(content).applicant(applicant).msgDate(getCurrentDateTimeString()).result(result).adminMsg(0).haveRead(0).linkUrl(linkUrl).build());
    }

    /**
     * message send to admin
     */
    public void sendToAdminVersion(String title, String content) {
        messageRepository.save(Message.builder().messageId(generateSnowflake()).title(title).content(content).msgDate(getCurrentDateTimeString()).adminMsg(1).haveRead(0).build());
    }

    /**
     * message read
     */
    public ResponseResult<Object> read(String token, String msgId) {
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        Message message = messageRepository.findById(msgId).get();
        boolean flag = false;
        if (!Objects.isNull(message.getApplicant())) {
            flag = StringUtils.equals(message.getApplicant().getPersonId(), userId);
        }
        if (flag || (isSystemAdmin(token) && message.getAdminMsg() == 1)) {
            message.setHaveRead(1);
            messageRepository.save(message);
            return ResultUtil.success();
        } else {
            return ResultUtil.errorInternational("OPERATION_FAILED");
        }
    }

    /**
     * my message list
     */
    public ResponseResult<Object> message(String token, String pageOffset, String pageSize) {
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        Map<String, Object> map = new HashMap<>(16);
        Query query = new Query();
        final Criteria criteria = Criteria.where("applicant.personId").is(userId);
        if (isSystemAdmin(token)) {
            query.addCriteria(new Criteria().orOperator(criteria, Criteria.where("adminMsg").is(1)));
        } else {
            query.addCriteria(criteria).addCriteria(Criteria.where("adminMsg").is(0));
        }
        map.put("count", mongoTemplate.count(query, Message.class));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Message message : mongoTemplate.find(query.with(PageRequest.of(Integer.parseInt(pageOffset), Integer.parseInt(pageSize))).with(Sort.by(Sort.Order.desc("msgDate"))), Message.class)) {
            Map<String, Object> msgMap = new HashMap<>(16);
            msgMap.put("messageId", message.getMessageId());
            msgMap.put("title", message.getTitle());
            msgMap.put("content", message.getContent());
            msgMap.put("haveRead", message.getHaveRead());
            msgMap.put("msgDate", message.getMsgDate());
            msgMap.put("linkUrl", message.getLinkUrl());
            list.add(msgMap);
        }
        map.put("content", list);
        return ResultUtil.success(map);
    }

    /**
     * the count of un-read message
     */
    public ResponseResult<Object> msgUnread(String token) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query();
        Criteria criteria = Criteria.where("applicant.personId").is(userIdFromToken);
        if (isSystemAdmin(token)) {
            query.addCriteria(new Criteria().orOperator(criteria, Criteria.where("adminMsg").is(1))).addCriteria(Criteria.where("haveRead").is(0));
        } else {
            query.addCriteria(criteria).addCriteria(Criteria.where("adminMsg").is(0)).addCriteria(Criteria.where("haveRead").is(0));
        }
        return ResultUtil.success(mongoTemplate.count(query, Message.class));
    }

    private boolean isSystemAdmin(String token) {
        Token utilsToken = jwtTokenUtils.getToken(token);
        return utilsToken.getRoles().contains(Constants.ADMIN);
    }
}
