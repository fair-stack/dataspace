package cn.cnic.dataspace.api.datax.admin.core.kill;

import cn.cnic.dataspace.api.datax.admin.core.trigger.JobTrigger;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.datax.core.biz.model.TriggerParam;
import cn.cnic.dataspace.api.datax.core.enums.ExecutorBlockStrategyEnum;
import cn.cnic.dataspace.api.datax.core.glue.GlueTypeEnum;
import java.util.Date;

/**
 * datax-job trigger
 * Created by  on 2019/12/15.
 */
public class KillJob {

    /**
     * @param logId
     * @param address
     * @param processId
     */
    public static ReturnT<String> trigger(long logId, Date triggerTime, String address, String processId) {
        ReturnT<String> triggerResult;
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(-1);
        triggerParam.setExecutorHandler("killJobHandler");
        triggerParam.setProcessId(processId);
        triggerParam.setLogId(logId);
        triggerParam.setGlueType(GlueTypeEnum.DATAX.getDesc());
        triggerParam.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.SERIAL_EXECUTION.getTitle());
        triggerParam.setLogDateTime(triggerTime.getTime());
        if (address != null) {
            triggerResult = JobTrigger.runExecutor(triggerParam, address);
        } else {
            triggerResult = new ReturnT<>(ReturnT.FAIL_CODE, null);
        }
        return triggerResult;
    }
}
