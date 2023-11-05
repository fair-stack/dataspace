package cn.cnic.dataspace.api.datax.admin.core.scheduler;

import cn.cnic.dataspace.api.datax.admin.core.conf.JobAdminConfig;
import cn.cnic.dataspace.api.datax.admin.core.thread.*;
import cn.cnic.dataspace.api.datax.admin.core.util.I18nUtil;
import cn.cnic.dataspace.api.datax.core.biz.ExecutorBiz;
import cn.cnic.dataspace.api.datax.core.enums.ExecutorBlockStrategyEnum;
import cn.cnic.dataspace.api.datax.rpc.remoting.invoker.call.CallType;
import cn.cnic.dataspace.api.datax.rpc.remoting.invoker.reference.XxlRpcReferenceBean;
import cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route.LoadBalance;
import cn.cnic.dataspace.api.datax.rpc.remoting.net.impl.netty_http.client.NettyHttpClient;
import cn.cnic.dataspace.api.datax.rpc.serialize.impl.HessianSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xuxueli 2018-10-28 00:18:17
 */
public class JobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    public void init() throws Exception {
        // init i18n
        initI18n();
        // admin registry monitor run
        JobRegistryMonitorHelper.getInstance().start();
        // admin monitor run
        JobFailMonitorHelper.getInstance().start();
        // admin trigger pool start
        JobTriggerPoolHelper.toStart();
        // admin log report start
        JobLogReportHelper.getInstance().start();
        // start-schedule
        JobScheduleHelper.getInstance().start();
        logger.info(">>>>>>>>> init datax-web admin success.");
    }

    public void destroy() throws Exception {
        // stop-schedule
        JobScheduleHelper.getInstance().toStop();
        // admin log report stop
        JobLogReportHelper.getInstance().toStop();
        // admin trigger pool stop
        JobTriggerPoolHelper.toStop();
        // admin monitor stop
        JobFailMonitorHelper.getInstance().toStop();
        // admin registry stop
        JobRegistryMonitorHelper.getInstance().toStop();
    }

    // ---------------------- I18n ----------------------
    private void initI18n() {
        for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }

    // ---------------------- executor-client ----------------------
    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();

    public static ExecutorBiz getExecutorBiz(String address) throws Exception {
        // valid
        if (address == null || address.trim().length() == 0) {
            return null;
        }
        // load-cache
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }
        // set-cache
        XxlRpcReferenceBean referenceBean = new XxlRpcReferenceBean();
        referenceBean.setClient(NettyHttpClient.class);
        referenceBean.setSerializer(HessianSerializer.class);
        referenceBean.setCallType(CallType.SYNC);
        referenceBean.setLoadBalance(LoadBalance.ROUND);
        referenceBean.setIface(ExecutorBiz.class);
        referenceBean.setVersion(null);
        referenceBean.setTimeout(3000);
        referenceBean.setAddress(address);
        referenceBean.setAccessToken(JobAdminConfig.getAdminConfig().getAccessToken());
        referenceBean.setInvokeCallback(null);
        referenceBean.setInvokerFactory(null);
        executorBiz = (ExecutorBiz) referenceBean.getObject();
        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }
}
