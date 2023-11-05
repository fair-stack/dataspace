package cn.cnic.dataspace.api.datax.admin.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * DataX JSON username and password decryption
 */
public class JSONUtils {

    /**
     * Decrypt decryption
     */
    public static Integer decrypt = 0;

    /**
     * Decrypt encryption
     */
    public static Integer encrypt = 1;

    /**
     * @ param content
     */
    public static JSONObject change(String content, String key, Integer changeType) {
        JSONObject keyObj = JSONObject.parseObject(JSONObject.parseObject(content).getString(key));
        JSONObject params = JSONObject.parseObject(keyObj.getString("parameter"));
        String dUsername = null, dPassword = null;
        if (decrypt.equals(changeType)) {
            // Decryption
            dUsername = AESUtil.decrypt(params.getString("username"));
            dPassword = AESUtil.decrypt(params.getString("password"));
        } else if (encrypt.equals(changeType)) {
            // encryption
            dUsername = AESUtil.encrypt(params.getString("username"));
            dPassword = AESUtil.encrypt(params.getString("password"));
        }
        String username = dUsername == null ? params.getString("username") : dUsername;
        String password = dPassword == null ? params.getString("password") : dPassword;
        params.put("username", username);
        params.put("password", password);
        keyObj.put("parameter", params);
        return keyObj;
    }

    /**
     * @ param jsonStr
     */
    public static String changeJson(String jsonStr, Integer changeType) {
        JSONObject json = JSONObject.parseObject(jsonStr);
        JSONObject job = json.getJSONObject("job");
        JSONArray contents = job.getJSONArray("content");
        for (int i = 0; i < contents.size(); i++) {
            String contentStr = contents.getString(i);
            Object obj = contents.get(i);
            if (decrypt.equals(changeType)) {
                // Decryption
                ((JSONObject) obj).put("reader", change(contentStr, "reader", decrypt));
                ((JSONObject) obj).put("writer", change(contentStr, "writer", decrypt));
            } else if (encrypt.equals(changeType)) {
                // encryption
                ((JSONObject) obj).put("reader", change(contentStr, "reader", encrypt));
                ((JSONObject) obj).put("writer", change(contentStr, "writer", encrypt));
            }
        }
        job.put("content", contents);
        json.put("job", job);
        return json.toJSONString();
    }
}
