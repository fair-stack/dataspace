package cn.cnic.dataspace.api.webdav;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * milton config
 *
 * @author wangCc
 * @date 2021-3-23 19:27:57
 */
@Configuration
public class MiltonConfig {

    @Autowired
    private SpringMiltonFilterBean springMiltonFilterBean;

    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean bean = new FilterRegistrationBean();
        bean.setFilter(springMiltonFilterBean);
        bean.addUrlPatterns("/webDAV/*");
        return bean;
    }
}
