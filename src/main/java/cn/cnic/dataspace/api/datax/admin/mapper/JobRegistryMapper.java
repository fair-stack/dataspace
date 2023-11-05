package cn.cnic.dataspace.api.datax.admin.mapper;

import cn.cnic.dataspace.api.datax.admin.entity.JobRegistry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

/**
 * Created by  on 2019/11/17
 */
@Mapper
public interface JobRegistryMapper extends BaseMapper<JobRegistry> {

    public List<Integer> findDead(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

    public int removeDead(@Param("ids") List<Integer> ids);

    public List<JobRegistry> findAll(@Param("timeout") int timeout, @Param("nowTime") Date nowTime);

    public int registryUpdate(@Param("registryGroup") String registryGroup, @Param("registryKey") String registryKey, @Param("registryValue") String registryValue, @Param("cpuUsage") double cpuUsage, @Param("memoryUsage") double memoryUsage, @Param("loadAverage") double loadAverage, @Param("updateTime") Date updateTime);

    public int registrySave(@Param("registryGroup") String registryGroup, @Param("registryKey") String registryKey, @Param("registryValue") String registryValue, @Param("cpuUsage") double cpuUsage, @Param("memoryUsage") double memoryUsage, @Param("loadAverage") double loadAverage, @Param("updateTime") Date updateTime);

    public int registryDelete(@Param("registryGroup") String registryGroup, @Param("registryKey") String registryKey, @Param("registryValue") String registryValue);
}
