package cn.cnic.dataspace.api.config.space;

import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.util.Constants;
import cn.cnic.dataspace.api.websocket.SocketManager;
import cn.cnic.dataspace.api.websocket.WebSocketProcess;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import javax.websocket.Session;
import java.util.List;
import java.util.Map;

/**
 * real-time push message with websocket
 *
 * @author wangCc
 * @date 2021-10-31 18:24
 */
@Slf4j
@Component
public class MsgUtil {

    @Autowired
    private WebSocketProcess webSocketProcess;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * send message to client by email
     */
    public void sendMsg(@RequestParam String email, @RequestParam String content) {
        try {
            Map map = JSONObject.parseObject(content, Map.class);
            map.put("mark", "message");
            webSocketProcess.sendMessage(email, JSONObject.toJSONString(map));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * send message to administrator
     */
    public void sendAdminMsg(@RequestParam String content) {
        // for (Map.Entry<String, Session> stringWebSocketProcessEntry : SocketManager.entrySet()) {
        // String key = stringWebSocketProcessEntry.getKey();
        // int i = key.lastIndexOf("_");
        // String email = key.substring(0, i);
        // if (isSystemAdmin(email)) {
        // try {
        // sendMsg(email, content);
        // } catch (Exception ignored) {
        // }
        // }
        // }
        List<String> allEmails = SocketManager.getAllEmails();
        for (String email : allEmails) {
            if (isSystemAdmin(email)) {
                try {
                    sendMsg(email, content);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * message map transform string
     */
    public String mapToString(Map<String, Object> requestMap) {
        return JSONObject.toJSONString(requestMap);
    }

    /**
     * system email check
     */
    public boolean isSystemAdmin(String email) {
        boolean flag = false;
        if (StringUtils.isNotBlank(email)) {
            Query query = new Query().addCriteria(Criteria.where("emailAccounts").is(email));
            ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class);
            if (consumerDO != null) {
                for (String role : consumerDO.getRoles()) {
                    if (StringUtils.equals(role, Constants.ADMIN)) {
                        flag = true;
                        break;
                    }
                }
            }
        }
        return flag;
    }
}
