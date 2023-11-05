package cn.cnic.dataspace.api.datax.admin.core.route.strategy;

import cn.cnic.dataspace.api.datax.admin.core.route.ExecutorRouter;
import cn.cnic.dataspace.api.datax.admin.core.scheduler.JobScheduler;
import cn.cnic.dataspace.api.datax.admin.core.util.I18nUtil;
import cn.cnic.dataspace.api.datax.core.biz.ExecutorBiz;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.datax.core.biz.model.TriggerParam;
import java.util.List;

/**
 * Created by xuxueli on 17/3/10.
 */
public class ExecutorRouteBusyover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuffer idleBeatResultSB = new StringBuffer();
        for (String address : addressList) {
            // beat
            ReturnT<String> idleBeatResult = null;
            try {
                ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(address);
                idleBeatResult = executorBiz.idleBeat(triggerParam.getJobId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                idleBeatResult = new ReturnT<String>(ReturnT.FAIL_CODE, "" + e);
            }
            idleBeatResultSB.append((idleBeatResultSB.length() > 0) ? "<br><br>" : "").append(I18nUtil.getString("jobconf_idleBeat") + "：").append("<br>address：").append(address).append("<br>code：").append(idleBeatResult.getCode()).append("<br>msg：").append(idleBeatResult.getMsg());
            // beat success
            if (idleBeatResult.getCode() == ReturnT.SUCCESS_CODE) {
                idleBeatResult.setMsg(idleBeatResultSB.toString());
                idleBeatResult.setContent(address);
                return idleBeatResult;
            }
        }
        return new ReturnT<String>(ReturnT.FAIL_CODE, idleBeatResultSB.toString());
    }
}
