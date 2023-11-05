package cn.cnic.dataspace.api.service;

import cn.cnic.dataspace.api.util.ResponseResult;
import javax.servlet.http.HttpServletRequest;

public interface HomeService {

    ResponseResult<Object> spaceSearch(int page, int size, HttpServletRequest request);

    ResponseResult<Object> spaceActive(HttpServletRequest request);

    ResponseResult<Object> informationStatistics();

    ResponseResult<Object> hotWordsList(HttpServletRequest request);

    ResponseResult<Object> setAcc(String acc, String pwd, boolean isOpen);

    ResponseResult<Object> versionInfo(String code, String version, String details);

    ResponseResult<Object> tagCount();
}
