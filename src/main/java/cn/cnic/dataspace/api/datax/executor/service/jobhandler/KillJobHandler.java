package cn.cnic.dataspace.api.datax.executor.service.jobhandler;

import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.datax.core.biz.model.TriggerParam;
import cn.cnic.dataspace.api.datax.core.handler.IJobHandler;
import cn.cnic.dataspace.api.datax.core.handler.annotation.JobHandler;
import cn.cnic.dataspace.api.datax.core.util.ProcessUtil;
import cn.hutool.core.io.FileUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.io.File;

/**
 * DataX task termination
 */
@JobHandler(value = "killJobHandler")
@Component
public class KillJobHandler extends IJobHandler {

    @Override
    public ReturnT<String> execute(TriggerParam tgParam) {
        String processId = tgParam.getProcessId();
        boolean result = ProcessUtil.killProcessByPid(processId);
        // Delete temporary files
        if (!CollectionUtils.isEmpty(jobTmpFiles)) {
            String pathname = jobTmpFiles.get(processId);
            if (pathname != null) {
                FileUtil.del(new File(pathname));
                jobTmpFiles.remove(processId);
            }
        }
        return result ? IJobHandler.SUCCESS : IJobHandler.FAIL;
    }
}
