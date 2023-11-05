package cn.cnic.dataspace.api.service;

import cn.cnic.dataspace.api.model.center.*;
import cn.cnic.dataspace.api.util.ResponseResult;

public interface ExternalInterService {

    ResponseResult<Object> accessOrgList(String name, String type);

    ResponseResult<Object> accessProjectList(String projectName);

    ResponseResult<Object> accessPaperList(String paperName);

    ResponseResult<Object> authorSearch(String param);

    ResponseResult<Object> objectDel(String id, String type);

    ResponseResult<Object> applyDOI();

    ResponseResult<Object> applyCSTR(CSTR cstr);

    ResponseResult<Object> orgList(String orgName);

    ResponseResult<Object> personAdd(Person person);

    ResponseResult<Object> orgAdd(Org org);

    ResponseResult<Object> projectAdd(Project project);

    ResponseResult<Object> paperAdd(Paper paper);

    ResponseResult<Object> checkCstr(String cstrCode);

    ResponseResult<Object> licenseList(String orgId, String url);

    ResponseResult<Object> getProType(String type);

    ResponseResult<Object> forDetails(String type, String[] ids);
}
