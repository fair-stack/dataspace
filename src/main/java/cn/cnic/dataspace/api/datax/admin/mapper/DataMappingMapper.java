package cn.cnic.dataspace.api.datax.admin.mapper;

import cn.cnic.dataspace.api.datax.admin.entity.DataMapping;
import cn.cnic.dataspace.api.datax.rpc.serialize.Serializer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.io.Serializable;
import java.util.List;

@Mapper
public interface DataMappingMapper extends BaseMapper<DataMapping> {

    DataMapping getByPrimaryKeyAndSpaceId(@Param("id") Serializable id, @Param("spaceId") String spaceId);

    IPage<DataMapping> getPagingBySpaceIdAndUserId(IPage<DataMapping> page, @Param("spaceId") String spaceId, @Param("userId") String userId, @Param("name") String name);

    List<DataMapping> getListBySpaceIdAndUserId(@Param("spaceId") String spaceId, @Param("userId") String userId, @Param("name") String name);

    IPage<DataMapping> selectPagingBySpaceIdAndName(IPage<DataMapping> page, @Param("spaceId") String spaceId, @Param("name") String name);

    List<DataMapping> selectListBySpaceIdAndName(@Param("spaceId") String spaceId, @Param("name") String name);
}
