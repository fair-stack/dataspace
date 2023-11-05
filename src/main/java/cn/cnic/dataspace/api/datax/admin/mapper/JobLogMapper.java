package cn.cnic.dataspace.api.datax.admin.mapper;

import cn.cnic.dataspace.api.datax.admin.entity.JobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * job log
 *
 * @author xuxueli 2016-1-12 18:03:06
 */
@Mapper
public interface JobLogMapper {

    // exist jobId not use jobGroup, not exist use jobGroup
    List<JobLog> pageList(@Param("offset") int offset, @Param("pagesize") int pagesize, @Param("jobGroup") int jobGroup, @Param("jobId") int jobId, @Param("triggerTimeStart") Date triggerTimeStart, @Param("triggerTimeEnd") Date triggerTimeEnd, @Param("logStatus") int logStatus);

    List<JobLog> getAllList(@Param("spaceId") String spaceId, @Param("dataMappingId") Long dataMappingId);

    int pageListCount(@Param("offset") int offset, @Param("pagesize") int pagesize, @Param("jobGroup") int jobGroup, @Param("jobId") int jobId, @Param("triggerTimeStart") Date triggerTimeStart, @Param("triggerTimeEnd") Date triggerTimeEnd, @Param("logStatus") int logStatus);

    JobLog load(@Param("id") long id);

    long save(JobLog jobLog);

    int updateTriggerInfo(JobLog jobLog);

    int updateHandleInfo(JobLog jobLog);

    int updateProcessId(@Param("id") long id, @Param("processId") String processId);

    int delete(@Param("jobId") int jobId);

    Map<String, Object> findLogReport(@Param("from") Date from, @Param("to") Date to);

    List<Long> findClearLogIds(@Param("jobGroup") int jobGroup, @Param("jobId") int jobId, @Param("clearBeforeTime") Date clearBeforeTime, @Param("clearBeforeNum") int clearBeforeNum, @Param("pagesize") int pagesize);

    int clearLog(@Param("logIds") List<Long> logIds);

    List<Long> findFailJobLogIds(@Param("pagesize") int pagesize);

    int updateAlarmStatus(@Param("logId") long logId, @Param("oldAlarmStatus") int oldAlarmStatus, @Param("newAlarmStatus") int newAlarmStatus);

    List<JobLog> getSuccessTriggerListByJobId(@Param("jobId") int jobId);

    List<JobLog> getListByHandleCode(@Param("handleCode") int handleCode);
}
