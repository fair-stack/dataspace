package cn.cnic.dataspace.api.datax.executor.config;

import cn.cnic.dataspace.api.datax.core.executor.impl.JobSpringExecutor;
import cn.cnic.dataspace.api.datax.executor.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * xxl-job config
 *
 * @author xuxueli 2017-04-28
 */
@Configuration
public class DataXConfig {

    private Logger logger = LoggerFactory.getLogger(DataXConfig.class);

    private static final String DEFAULT_LOG_PATH = "log/executor/jobhandler";

    @Value("${datax.job.admin.addresses}")
    private String adminAddresses;

    @Value("${datax.job.executor.appname}")
    private String appName;

    @Value("${datax.job.executor.ip}")
    private String ip;

    @Value("${datax.job.executor.port}")
    private int port;

    @Value("${datax.job.accessToken}")
    private String accessToken;

    @Value("${datax.job.executor.logpath}")
    private String logPath;

    @Value("${datax.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Bean
    public JobSpringExecutor JobExecutor() {
        logger.info(">>>>>>>>>>> datax-web config init.");
        JobSpringExecutor jobSpringExecutor = new JobSpringExecutor();
        jobSpringExecutor.setAdminAddresses(adminAddresses);
        jobSpringExecutor.setAppName(appName);
        jobSpringExecutor.setIp(ip);
        jobSpringExecutor.setPort(port);
        jobSpringExecutor.setAccessToken(accessToken);
        String dataXHomePath = SystemUtils.getDataXHomePath();
        if (StringUtils.isEmpty(logPath)) {
            logPath = dataXHomePath + DEFAULT_LOG_PATH;
        }
        jobSpringExecutor.setLogPath(logPath);
        jobSpringExecutor.setLogRetentionDays(logRetentionDays);
        return jobSpringExecutor;
    }
    /**
     * For situations such as multiple network cards and internal deployment of containers, the "InetUtils" component provided by "spring cloud commons" can be used to flexibly customize the registration IP;
     */
}
