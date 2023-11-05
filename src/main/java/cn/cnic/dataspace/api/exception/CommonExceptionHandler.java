package cn.cnic.dataspace.api.exception;

import cn.cnic.dataspace.api.datax.admin.util.DataXException;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.ResponseResult;
import cn.cnic.dataspace.api.util.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.tmatesoft.svn.core.SVNException;
import java.nio.file.FileSystemException;

/**
 * Unified exception handling
 */
@ControllerAdvice
@Slf4j
public class CommonExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseResult<Object> exceptionHandler(Exception e) {
        log.error(e.getMessage(), e);
        if (null != e.getMessage() && e.getMessage().equals("权限不足！")) {
            return ResultUtil.error(403, "权限不足！");
        }
        return ResultUtil.error(ExceptionType.SYSTEM_ERROR.getCode(), ExceptionType.SYSTEM_ERROR.getMsg());
    }

    @ExceptionHandler(CommonException.class)
    @ResponseBody
    public ResponseResult<Object> exceptionHandler(CommonException e) {
        log.info(e.getMessage(), e);
        return ResultUtil.error(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseResult<Object> exceptionHandler(MissingServletRequestParameterException e) {
        log.info(e.getMessage());
        return ResultUtil.error(ExceptionType.MISSING_PARAMETERS.getCode(), String.format("缺少参数%s", e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseResult<Object> exceptionHandler(MethodArgumentNotValidException e) {
        log.info(e.getMessage());
        return ResultUtil.error(ExceptionType.MISSING_PARAMETERS.getCode(), e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(BindException.class)
    @ResponseBody
    public ResponseResult<Object> exceptionHandler(BindException e) {
        log.info(e.getMessage());
        return ResultUtil.error(ExceptionType.MISSING_PARAMETERS.getCode(), e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void exceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.info(e.getMessage());
    }

    @ExceptionHandler(FileSystemException.class)
    public ResponseResult<Object> exceptionHandler(FileSystemException e) {
        log.info(e.getMessage());
        return ResultUtil.error(CommonUtils.messageInternational("INTERNAL_ERROR"));
    }

    @ExceptionHandler(SVNException.class)
    public ResponseResult<Object> authInsufficient() {
        return ResultUtil.error(CommonUtils.messageInternational("INTERNAL_ERROR"));
    }

    @ExceptionHandler(DataXException.class)
    @ResponseBody
    public ResponseResult<Object> exceptionHandler(DataXException e) {
        log.error(e.getMessage(), e);
        return ResultUtil.error(ExceptionType.SYSTEM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(OpenApiException.class)
    @ResponseBody
    public ResponseResult<Object> openApiException(OpenApiException e) {
        return ResultUtil.error(e.getCode(), e.getMsg());
    }
}
