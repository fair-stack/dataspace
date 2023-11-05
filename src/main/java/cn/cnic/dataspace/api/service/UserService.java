package cn.cnic.dataspace.api.service;

import cn.cnic.dataspace.api.model.user.ConsumerDTO;
import cn.cnic.dataspace.api.model.user.ConsumerInfoDTO;
import cn.cnic.dataspace.api.model.user.ManualAddList;
import cn.cnic.dataspace.api.util.ResponseResult;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public interface UserService {

    // User addition
    ResponseResult<Object> add(ConsumerDTO consumerDTO, HttpServletRequest request);

    ResponseResult<Object> update(String token, ConsumerInfoDTO consumerInfoDTO);

    ResponseResult<Object> query(String token);

    ResponseResult<Object> find(String token, String email, String spaceId);

    ResponseResult<Object> importUser(String token, ManualAddList manualAddList);

    ResponseResult<Object> setEmailList(String token);

    ResponseResult<Object> setEmail(String token, String type, Boolean value);
}
