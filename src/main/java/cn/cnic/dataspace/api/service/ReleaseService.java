package cn.cnic.dataspace.api.service;

import cn.cnic.dataspace.api.util.ResponseResult;

public interface ReleaseService {

    ResponseResult<Object> releaseSearch(String token, int page, int size, int state, String releaseName);

    // ResponseResult<Object> templateParsing(String orgId,String url);
    ResponseResult<Object> getVersion(String resourceId);

    ResponseResult<Object> getSubjectList(String param);

    ResponseResult<Object> resourceUpdate(String data);

    ResponseResult<Object> resourceDelete(String token, String id);

    ResponseResult<Object> judgeRelease(String orgId);

    ResponseResult<Object> releaseCount(String token);
}
