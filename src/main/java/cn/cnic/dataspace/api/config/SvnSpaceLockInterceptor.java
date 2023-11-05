package cn.cnic.dataspace.api.config;

import cn.cnic.dataspace.api.service.space.SvnService;
import cn.cnic.dataspace.api.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Svn Space Lock Interceptor
 *
 * @author wangcc
 * @date 2021-11-08 19:19:46
 */
@Slf4j
@Configuration
public class SvnSpaceLockInterceptor extends HandlerInterceptorAdapter {

    private final SvnService svnService;

    @Autowired
    public SvnSpaceLockInterceptor(SvnService svnService) {
        this.svnService = svnService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String[] forbidden = { "archive", "extract", "mkdir", "mkfile", "paste", "put", "rename", "rm", "upload" };
        if (Arrays.asList(forbidden).contains(request.getParameter("cmd"))) {
            if (svnService.checkSpaceLock(request.getParameter("spaceId"))) {
                throw new RuntimeException(CommonUtils.messageInternational("SVN_LOCK"));
            }
        }
        return true;
    }
}
