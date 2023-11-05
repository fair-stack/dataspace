package cn.cnic.dataspace.api.exception;

import cn.cnic.dataspace.api.util.APICodeType;

/**
 * @ author chl
 */
public class OpenApiException extends RuntimeException {

    private final int code;

    private final String msg;

    public OpenApiException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public OpenApiException(APICodeType type) {
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
