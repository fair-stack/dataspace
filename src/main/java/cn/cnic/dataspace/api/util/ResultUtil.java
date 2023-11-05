package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.exception.ExceptionType;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

/**
 * ResultUtil
 *
 * @author jmal
 */
public class ResultUtil {

    private ResultUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> ResponseResult<T> genResult() {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(0);
        result.setMessage(true);
        return result;
    }

    public static <T> ResponseResult<T> success(T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(0);
        result.setMessage(true);
        result.setData(data);
        return result;
    }

    public static <T> ResponseResult<T> success(String message, T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(0);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> ResponseResult<T> success(int code, String message, T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> ResponseResult<T> error(int code, T message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(message);
        return result;
    }

    public static <T> ResponseResult<T> successMsg(String message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(0);
        result.setMessage(message);
        return result;
    }

    public static <T> ResponseResult<T> successInternational(String message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(0);
        result.setMessage(messageInternational(message));
        return result;
    }

    public static <T> ResponseResult<T> errorInternational(String message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(-1);
        result.setMessage(messageInternational(message));
        return result;
    }

    public static <T> ResponseResult<T> success() {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(0);
        result.setMessage(true);
        return result;
    }

    public static <T> ResponseResult<T> error(int code, String msg, T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(code);
        result.setMessage(msg);
        result.setData(data);
        return result;
    }

    public static <T> ResponseResult<T> error(String msg) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(-1);
        result.setMessage(msg);
        return result;
    }

    public static <T> ResponseResult<T> publicError(String msg) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(404);
        result.setMessage(messageInternational(msg));
        return result;
    }

    public static <T> ResponseResult<T> error(ExceptionType exceptionType) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(exceptionType.getCode());
        result.setMessage(exceptionType.getMsg());
        return result;
    }

    public static <T> ResponseResult<T> warning(String msg) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(-2);
        result.setMessage(msg);
        return result;
    }
}
