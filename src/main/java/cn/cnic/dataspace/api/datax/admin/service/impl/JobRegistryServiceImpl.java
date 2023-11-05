package cn.cnic.dataspace.api.datax.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.cnic.dataspace.api.datax.admin.entity.JobRegistry;
import cn.cnic.dataspace.api.datax.admin.mapper.JobRegistryMapper;
import cn.cnic.dataspace.api.datax.admin.service.JobRegistryService;
import org.springframework.stereotype.Service;

/**
 * JobRegistryServiceImpl
 * @author
 * @since 2019-03-15
 * @version v2.1.1
 */
@Service("jobRegistryService")
public class JobRegistryServiceImpl extends ServiceImpl<JobRegistryMapper, JobRegistry> implements JobRegistryService {
}
