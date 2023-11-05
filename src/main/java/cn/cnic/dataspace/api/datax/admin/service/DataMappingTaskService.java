package cn.cnic.dataspace.api.datax.admin.service;

import cn.cnic.dataspace.api.datax.admin.dto.DataMappingTaskVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import java.util.List;

public interface DataMappingTaskService {

    IPage<DataMappingTaskVO> getPaging(String spaceId, Long dataMappingId, Integer status, Integer current, Integer size);

    List<DataMappingTaskVO> getAll(String spaceId, Long dataMappingId, Integer status);
}
