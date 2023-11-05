package cn.cnic.dataspace.api.config;

import cn.cnic.dataspace.api.datax.admin.intercept.HasSpacePermissionInterceptor;
import cn.cnic.dataspace.api.interceport.AccessInterceptor;
import cn.cnic.dataspace.api.interceport.WebInterceptor;
import cn.cnic.dataspace.api.websocket.WebSocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * InterceptorConfig
 *
 * @author chl
 * @date 2019-12-09 18:11
 */
@Configuration
@Import(ServerEndpointExporter.class)
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private WebInterceptor webInterceptor;

    @Autowired
    private WebSocketInterceptor webSocketInterceptor;

    @Autowired
    private SvnSpaceLockInterceptor svnSpaceLockInterceptor;

    @Autowired
    private HasSpacePermissionInterceptor hasSpacePermissionInterceptor;

    @Autowired
    private AccessInterceptor accessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // datax
        // datax
        // datax
        // datax
        // datax
        // datax
        // datax
        registry.addInterceptor(webInterceptor).// datax
        addPathPatterns(// datax
        "/common/**", // datax
        "/access/**", // datax
        "/release/**", // datax
        "/resource/**", // datax
        "/user/**", // datax
        "/setting/**", // datax
        "/share/space.link", // datax
        "/share/file.link", // datax
        "/share/list", // datax
        "/share/set", // datax
        "/space/**", // Swagger Path Interception - Production Environment
        "/network/**", // Swagger Path Interception - Production Environment
        "/get.u", // Swagger Path Interception - Production Environment
        "/safety/**", // Swagger Path Interception - Production Environment
        "/data/space", // Swagger Path Interception - Production Environment
        "/harvest/**", // Swagger Path Interception - Production Environment
        "/file/**", // Do not intercept paths
        "/data_mapping/**", // Communication interface between datax admin and executor
        "/data_mapping_oper/**", // Communication interface between datax admin and executor
        "/jobJdbcDatasource/**", // Communication interface between datax admin and executor
        "/metadata/**", // Communication interface between datax admin and executor
        "/datax/task/**", // Communication interface between datax admin and executor
        "/getOfficeInfo", // Communication interface between datax admin and executor
        "/doc.html").// Communication interface between datax admin and executor
        excludePathPatterns(// Communication interface between datax admin and executor
        "/user/add", // Communication interface between datax admin and executor
        "/setting/check", // Communication interface between datax admin and executor
        "/setting/basic", // Communication interface between datax admin and executor
        "/setting/set.accQuery", // Communication interface between datax admin and executor
        "/network/auth", // Communication interface between datax admin and executor
        "/network/callback", // Communication interface between datax admin and executor
        "/setting/umt.ck", // Communication interface between datax admin and executor
        "/msg/**", // Communication interface between datax admin and executor
        "/common/pwdEmail", // Communication interface between datax admin and executor
        "/common/restPwd", // Communication interface between datax admin and executor
        "/common/language", // Communication interface between datax admin and executor
        "/space/urlCheck", // Communication interface between datax admin and executor
        "/space/previewUrl", // Communication interface between datax admin and executor
        "/release/tem.down", // File Download
        "/harvest/isPwd", // File Download
        "/harvest/file", // File Download
        "/harvest/detail", // File Download
        "/harvest/ftp", "/harvest/fileList", "test/**", "/api/callback", "/api/processCallback", "/api/registry", "/api/registryRemove", "/file/download", "/space/log.down");
        registry.addInterceptor(webSocketInterceptor).addPathPatterns("/msg/**");
        registry.addInterceptor(svnSpaceLockInterceptor).addPathPatterns("/data/space/**");
        // registry.addInterceptor(rateLimiterInterceptor).addPathPatterns("/data/space/**", "/test/test");
        registry.addInterceptor(hasSpacePermissionInterceptor).addPathPatterns("/**").excludePathPatterns("/api/callback", "/api/processCallback", "/api/registry", "/api/registryRemove");
        registry.addInterceptor(accessInterceptor).addPathPatterns("/information.statistics", "/space/self", "/space/public", "/public/detail", "/share/detail", "/share/space.info", "/login", "/release/search");
    }
}
