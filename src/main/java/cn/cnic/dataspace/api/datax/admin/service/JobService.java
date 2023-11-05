package cn.cnic.dataspace.api.datax.admin.service;

import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.datax.admin.dto.DataXBatchJsonBuildDto;
import cn.cnic.dataspace.api.datax.admin.entity.JobInfo;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * core job action for datax-web
 *
 * @author xuxueli 2016-5-28 15:30:33
 */
public interface JobService {

    JobInfo getJobByDataMappingId(Long dataMappingId);

    /**
     * add job
     *
     * @param jobInfo
     * @return
     */
    ReturnT<String> add(JobInfo jobInfo);

    /**
     * update job
     *
     * @param jobInfo
     * @return
     */
    ReturnT<String> update(JobInfo jobInfo);

    /**
     * remove job
     * *
     *
     * @param id
     * @return
     */
    ReturnT<String> remove(int id);

    /**
     * start job
     *
     * @param id
     * @return
     */
    ReturnT<String> start(int id);

    /**
     * stop job
     *
     * @param id
     * @return
     */
    ReturnT<String> stop(int id);

    /**
     * dashboard info
     *
     * @return
     */
    Map<String, Object> dashboardInfo();

    /**
     * chart info
     *
     * @return
     */
    ReturnT<Map<String, Object>> chartInfo();
}
