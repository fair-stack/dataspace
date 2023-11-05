package cn.cnic.dataspace.api.controller.space;

import cn.cnic.dataspace.api.config.space.MsgUtil;
import cn.cnic.dataspace.api.elfinder.command.ElfinderCommand;
import cn.cnic.dataspace.api.elfinder.command.ElfinderCommandFactory;
import cn.cnic.dataspace.api.elfinder.command.ElfinderCommonService;
import cn.cnic.dataspace.api.elfinder.config.ContextImpl;
import cn.cnic.dataspace.api.elfinder.config.ElFinderConstants;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.elfinder.util.RequestUtil;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * elfinder controller
 *
 * @author wangCc
 * @date 2021-3-26 10:21:04
 */
@Controller
@RequestMapping("data/space")
@Slf4j
public class ElfinderController {

    @Resource(name = "commandFactory")
    private ElfinderCommandFactory elfinderCommandFactory;

    @Autowired
    private ElfinderStorageService elfinderStorageService;

    @Autowired
    private ElfinderCommonService commonService;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MsgUtil msgUtil;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final Map<String, Deque<String>> DEQUE_MAP = new ConcurrentHashMap<>(128);

    private static final Map<String, Set<String>> EMAIL_SET_MAP = new ConcurrentHashMap<>(128);

    private static final Map<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>(128);

    private static final String[] QUEUED_CMD = { "extract", "archive", "zipdl", "mkfile", "rename", "move", "upload", "rm", "paste" };

    private static final String[] STOP_CMD = { "extract", "archive", "zipdl", "file", "upload" };

    @RequestMapping
    public void executeRequest(HttpServletRequest request, final HttpServletResponse response) {
        final String command = request.getParameter(ElFinderConstants.ELFINDER_PARAMETER_COMMAND);
        final String spaceId = request.getParameter("spaceId");
        Token token = jwtTokenUtils.getToken(request.getHeader("Authorization"));
        boolean res = spaceAuth(command, spaceId, token, response);
        if (!res) {
            return;
        }
        if (!Arrays.asList(QUEUED_CMD).contains(command)) {
            connector(request, response, command);
        } else {
            // boolean res = spaceAuth(command,spaceId, token, response);
            // if(!res){
            // return;
            // }
            final Deque<String> queue;
            ReentrantLock lock;
            if (DEQUE_MAP.containsKey(spaceId)) {
                queue = DEQUE_MAP.get(spaceId);
                lock = LOCK_MAP.get(spaceId);
            } else {
                queue = new LinkedBlockingDeque<>(10);
                lock = new ReentrantLock(true);
                DEQUE_MAP.put(spaceId, queue);
                LOCK_MAP.put(spaceId, lock);
            }
            if (queue.add(token.getName() + " " + request.getParameter("content"))) {
                Set<String> emailSet;
                if (EMAIL_SET_MAP.containsKey(spaceId)) {
                    emailSet = EMAIL_SET_MAP.get(spaceId);
                } else {
                    emailSet = new HashSet<>();
                }
                emailSet.add(token.getEmailAccounts());
                EMAIL_SET_MAP.put(spaceId, emailSet);
                // sendMessage(spaceId, "added");
                lock.lock();
                try {
                    // if ((3, TimeUnit.MINUTES);) {
                    connector(request, response, command);
                    // sendMessage(spaceId, "1");
                    /*}
                } catch (InterruptedException e) {
                    e.printStackTrace();*/
                } finally {
                    lock.unlock();
                    queue.removeFirst();
                }
            } else {
                result(response, 202, "QUEUE_FULL");
                return;
                // response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                // response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                // Map<String, Object> result = new HashMap<>(16);
                // result.put("code", 202);
                // result.put("data", CommonUtils.messageInternational("QUEUE_FULL"));
                // try {
                // response.getWriter().write(msgUtil.mapToString(result));
                // } catch (IOException e) {
                // e.printStackTrace();
                // }
            }
        }
    }

    private void connector(HttpServletRequest request, final HttpServletResponse response, String command) {
        try {
            request = new RequestUtil().processMultipartContent(request);
            ElfinderCommand elfinderCommand = elfinderCommandFactory.get(command);
            elfinderCommand.execute(new ContextImpl(request, response, elfinderStorageService), commonService, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean spaceAuth(String cmd, String spaceId, Token token, HttpServletResponse response) {
        // Verify permissions
        if (Arrays.asList(STOP_CMD).contains(cmd)) {
            result(response, 500, "GENERAL_DISABLE");
            return false;
        }
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId)), Space.class);
        if (null == space || !space.getState().equals("1")) {
            result(response, 500, "SPACE_REVIEW");
            return false;
        }
        if (!token.getUserId().equals(space.getUserId())) {
            Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
            boolean judge = false;
            for (AuthorizationPerson authorizationPerson : authorizationList) {
                if (authorizationPerson.getUserId().equals(token.getUserId())) {
                    judge = true;
                    if (authorizationPerson.getRole().equals("普通") && StringUtils.isNotEmpty(space.getFileRole()) && space.getFileRole().equals(Constants.SpaceRole.MANAGE)) {
                        if (!cmd.equals("rename") && !cmd.equals("rm")) {
                            return true;
                        }
                        result(response, 403, "PERMISSION_FORBIDDEN");
                        return false;
                    }
                }
            }
            if (!judge) {
                result(response, 403, "ACCESS_FORBIDDEN");
                return false;
            }
        }
        return true;
    }

    /**
     * sending message when queue changed
     */
    private void sendMessage(String spaceId, String content) {
        final Set<String> strings = EMAIL_SET_MAP.get(spaceId);
        if ((!Objects.isNull(strings))) {
            for (String email : strings) {
                msgUtil.sendMsg(email, content);
            }
        }
    }

    /**
     * Error return
     */
    private void result(HttpServletResponse response, int code, String message) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        Map<String, Object> result = new HashMap<>(16);
        result.put("code", code);
        result.put("data", CommonUtils.messageInternational(message));
        try {
            response.getWriter().write(msgUtil.mapToString(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * queue list
     * record user email when click queue list for sending task completed signal
     */
    @ResponseBody
    @GetMapping("/queue")
    public ResponseResult<List<String>> queue(@RequestHeader("Authorization") String token, String spaceId) {
        final String email = jwtTokenUtils.getEmail(token);
        // record user email and send msg
        Set<String> emailSet;
        if (EMAIL_SET_MAP.containsKey(spaceId)) {
            emailSet = EMAIL_SET_MAP.get(spaceId);
        } else {
            emailSet = new HashSet<>();
        }
        emailSet.add(email);
        EMAIL_SET_MAP.put(spaceId, emailSet);
        // queue list
        List<String> result = new ArrayList<>();
        if (DEQUE_MAP.containsKey(spaceId)) {
            result.addAll(DEQUE_MAP.get(spaceId));
        }
        return ResultUtil.success(result);
    }
}
