package cn.cnic.dataspace.api;

import cn.cnic.dataspace.api.elfinder.command.CommandFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * data space service application
 *
 * @author wangCc
 * @date 2021-3-18 17:19:11
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class DataSpaceServiceApplication {

    // /142 storage location mnt/sdb/data/disk/62845617e2401861dcfc4c3a/1540527093467602944
    public static void main(String[] args) {
        SpringApplication.run(DataSpaceServiceApplication.class, args);
    }

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addAllowedMethod("*");
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildConfig());
        return new CorsFilter(source);
    }

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(connector -> connector.setProperty("relaxedQueryChars", "|{}[]\\"));
        return factory;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CommandFactory commandFactory() {
        CommandFactory commandFactory = new CommandFactory();
        commandFactory.setClassNamePattern("cn.cnic.dataspace.api.elfinder.command.%sCommand");
        return commandFactory;
    }
}
