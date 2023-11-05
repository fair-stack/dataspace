package cn.cnic.dataspace.api.webdav;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.milton.config.HttpManagerBuilder;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.http11.DefaultHttp11ResponseHandler;
import io.milton.http.http11.auth.SecurityManagerBasicAuthHandler;
import io.milton.servlet.MiltonServlet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * SpringMiltonFilterBean
 * A refactoring of the {@link io.milton.servlet.SpringMiltonFilter} to be better suited to SpringBoot.
 * The filter implements the {@link GenericFilterBean} so that it doesn't have to be responsible for looking the context.
 *
 * @author wangCc
 * @date 2021-3-29 14:44:05
 */
@Slf4j
@Component
public class SpringMiltonFilterBean extends GenericFilterBean {

    @Value("${file.rootDir}")
    private String rootDir;

    @Autowired
    private DataSpaceSecurityManager dataSpaceSecurityManager;

    private HttpManager httpManager;

    private ServletContext servletContext;

    @Override
    protected void initFilterBean() throws ServletException {
        log.info("init");
        super.initFilterBean();
        servletContext = getServletContext();
        HttpManagerBuilder builder = new HttpManagerBuilder();
        builder.setResourceFactory(new FileSystemResourceFactoryExt(new File(rootDir), dataSpaceSecurityManager, "webDAV"));
        builder.setBuffering(DefaultHttp11ResponseHandler.BUFFERING.never);
        builder.setEnableCompression(false);
        builder.setAuthenticationHandlers(Arrays.asList(new SecurityManagerBasicAuthHandler[] { new SecurityManagerBasicAuthHandler(dataSpaceSecurityManager) }));
        builder.setEnableCookieAuth(false);
        builder.setEnableOptionsAuth(true);
        builder.setEnableBasicAuth(false);
        builder.init();
        builder.init();
        httpManager = builder.buildHttpManager();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws IOException, ServletException {
        log.info("doFilter");
        if (request instanceof HttpServletRequest) {
            HttpServletRequest hsr = (HttpServletRequest) request;
            log.trace("doFilter: begin milton processing");
            try {
                /*String standardPath = "/webDAV/6062cf4d0662c663bcb7fe4b/1414835294892457984";
                if (hsr.getRequestURI().length() > standardPath.length()) {*/
                doMiltonProcessing(hsr, (HttpServletResponse) response);
                // }
            } catch (Exception ignored) {
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                httpServletResponse.setStatus(401);
                return;
            }
        } else {
            log.trace("doFilter: request is not a supported type, continue with filter chain");
            fc.doFilter(request, response);
        }
    }

    private void doMiltonProcessing(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Request request;
        Response response;
        try {
            request = new io.milton.servlet.ServletRequest(req, servletContext);
            response = new io.milton.servlet.ServletResponse(resp);
        } catch (Throwable e) {
            // OK, I know its not cool to log AND throw. But we really want to log the error
            // so it goes to the log4j logs, but we also want the container to handle
            // the exception because we're outside the milton response handling framework
            // So log and throw it is. But should never happen anyway...
            log.error("Exception creating milton request/response objects", e);
            throw new IOException("Exception creating milton request/response objects", e);
        }
        try {
            MiltonServlet.setThreadlocals(req, resp);
            httpManager.process(request, response);
        } finally {
            MiltonServlet.clearThreadlocals();
            // resp.getOutputStream().flush();
            resp.flushBuffer();
        }
    }

    @Override
    public void destroy() {
        if (httpManager != null) {
            httpManager.shutdown();
        }
    }
}
