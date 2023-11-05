package cn.cnic.dataspace.api.datax.admin.config;

import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MybatisPlus configuration class Spring boot method
 */
@EnableTransactionManagement
@Configuration
@MapperScan("cn.cnic.dataspace.api.datax.admin.mapper")
public class MybatisPlusConfig {

    /**
     * Paging plugin
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        return paginationInterceptor.setOverflow(true);
    }

    /**
     * MyBatisPlus logic deletion, needs to be configured and enabled in yml
     */
    @Bean
    public ISqlInjector sqlInjector() {
        return new DefaultSqlInjector();
    }
}
