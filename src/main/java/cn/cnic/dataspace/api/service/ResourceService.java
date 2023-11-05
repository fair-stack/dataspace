package cn.cnic.dataspace.api.service;

import cn.cnic.dataspace.api.util.ResponseResult;

public interface ResourceService {

    ResponseResult<Object> resourceSearch(String token, int page, int size, String spaceId, String resourceTitle);
}
