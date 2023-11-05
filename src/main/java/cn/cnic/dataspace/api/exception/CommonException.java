package cn.cnic.dataspace.api.exception;

/**
 * @ author jmal
 */
public class CommonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    private final String msg;

    public CommonException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public CommonException(String msg) {
        this.code = -1;
        this.msg = msg;
    }

    public CommonException(ExceptionType type) {
        this.code = type.getCode();
        this.msg = type.getMsg();
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
