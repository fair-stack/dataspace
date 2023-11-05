package cn.cnic.dataspace.api.service;

import cn.cnic.dataspace.api.model.release.ResourceDo;
import cn.cnic.dataspace.api.model.release.ResourceV2;
import cn.cnic.dataspace.api.util.ResponseResult;
import java.util.List;
import java.util.Map;

public interface ScidbService {

    ResponseResult submitSCIDB(ResourceV2 resource, String type, List<ResourceDo> resourceDoList, Map fileParam);
}
