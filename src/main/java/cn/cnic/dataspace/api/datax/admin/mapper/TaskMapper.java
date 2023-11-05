package cn.cnic.dataspace.api.datax.admin.mapper;

import cn.cnic.dataspace.api.datax.admin.dto.DataMappingTaskVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TaskMapper extends BaseMapper<DataMappingTaskVO> {

    IPage<DataMappingTaskVO> selectByPaging(IPage<DataMappingTaskVO> page, @Param("dataMappingId") Long dataMappingId, @Param("searchStatus") Integer searchStatus, @Param("spaceId") String spaceId);

    List<DataMappingTaskVO> selectList(@Param("dataMappingId") Long dataMappingId, @Param("searchStatus") Integer searchStatus, @Param("spaceId") String spaceId);
}
