package cn.cnic.dataspace.api.datax.admin.mapper;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * Jdbc data source configuration table database access layer
 */
@Mapper
public interface JobDatasourceMapper extends BaseMapper<JobDatasource> {

    int update(JobDatasource datasource);

    int incrementCitationNum(@Param("id") Long id);

    List<String> selectHostDataSourceType(@Param("spaceId") String spaceId);

    JobDatasource selectTopOneBySourceType(@Param("spaceId") String spaceId, @Param("sourceType") String sourceType);
}
