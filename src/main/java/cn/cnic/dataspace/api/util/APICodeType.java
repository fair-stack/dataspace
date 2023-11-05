package cn.cnic.dataspace.api.util;

/**
 * @ author chl
 */
public enum APICodeType {

    SUCCESS(200, "SUCCESS"),
    /**
     * Verification
     */
    AUTH_ROLE(403, "无权操作"),
    AUTH_APP_KEY(301, "appKey不合法！"),
    AUTH_SIGN(302, "sign 不匹配！"),
    Current_limiting(304, "当前请求过于频繁，请稍后再试..."),
    /**
     * Other abnormalities
     */
    SYSTEM_ERROR(500, "系统错误-请稍后重试"),
    /**
     * Missing parameter
     */
    MISSING_PARAMETERS(501, "缺少参数"),
    EMAIL_FORMAT(502, "邮箱格式错误"),
    SPECIAL_CHARACTERS(503, "字段包含特殊字符"),
    PASSWORD_STRENGTH(504, "密码强度太低"),
    STRING_LENGTH(505, "字符串长度超过限制"),
    SPACE_LOGO(506, "空间标识不符合规定"),
    SPACE_CODE(507, "空间标识重复"),
    USER_NOT_EXIST(508, "用户不存在"),
    SPACE_REPEAT(509, "空间名称重复"),
    SPACE_DB(510, "空间数据库创建失败-请稍后重试"),
    USER_DIS(511, "用户已被禁用"),
    USER_ACTIVATE(512, "用户未激活"),
    SPACE_NOT_EXIST(601, "空间不存在"),
    SPACE_REVIEWED(602, "空间待审核"),
    SPACE_GO_OFFLINE(603, "空间已下线");

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    private int code;

    private String msg;

    APICodeType(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
