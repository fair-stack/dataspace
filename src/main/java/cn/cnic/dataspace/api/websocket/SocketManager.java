package cn.cnic.dataspace.api.websocket;

import com.google.common.collect.Lists;
import org.springframework.util.StringUtils;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * socket manager
 *
 * @author wangCc
 * @date 2021-10-30 19:53:03
 */
public class SocketManager {

    private static final ConcurrentHashMap<String, Session> MANAGER = new ConcurrentHashMap<>(50);

    public static String generateKey(String email, String sessionId) {
        return email + "_" + sessionId;
    }

    static void add(String key, Session webSocketSession) {
        if (MANAGER.containsKey(key)) {
            MANAGER.remove(key);
        }
        MANAGER.put(key, webSocketSession);
    }

    public static void remove(String key) {
        MANAGER.remove(key);
    }

    public static void removeByEmail(String email) {
        Set<String> keys = MANAGER.keySet().stream().filter(new Predicate<String>() {

            @Override
            public boolean test(String s) {
                if (!StringUtils.isEmpty(s) && s.startsWith(email)) {
                    return true;
                } else {
                    return false;
                }
            }
        }).collect(Collectors.toSet());
        keys.forEach(var -> {
            MANAGER.remove(var);
        });
    }

    public static boolean contains(String key) {
        return MANAGER.containsKey(key);
    }

    public static List<String> getAllEmails() {
        Set<String> collect = MANAGER.keySet().stream().map(var -> {
            int i = var.lastIndexOf("_");
            String email = var.substring(0, i);
            return email;
        }).collect(Collectors.toSet());
        return new ArrayList<>(collect);
    }

    public static List<Session> getByEmail(String email) {
        List<Session> sessions = Lists.newArrayList();
        if (StringUtils.isEmpty(email)) {
            return sessions;
        }
        MANAGER.entrySet().forEach(var -> {
            String key = var.getKey();
            if (key != null && key.startsWith(email)) {
                sessions.add(var.getValue());
            }
        });
        return sessions;
    }

    public static Session get(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return MANAGER.get(key);
    }
}
