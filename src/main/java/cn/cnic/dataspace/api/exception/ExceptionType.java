package cn.cnic.dataspace.api.exception;

/**
 * @ author jmal
 */
public enum ExceptionType {

    /**
     * Other abnormalities
     */
    SYSTEM_ERROR(-1, "系统错误-请稍后重试!"),
    /**
     * Missing parameter
     */
    MISSING_PARAMETERS(1, "缺少参数"),
    /**
     * Incorrect time format
     */
    UNPARSEABLE_DATE(2, "时间格式不正确"),
    /**
     * Not logged in or login timeout
     */
    LOGIN_EXCEPRION(5, "未登录或登录超时");

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    private int code;

    private String msg;

    ExceptionType(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
