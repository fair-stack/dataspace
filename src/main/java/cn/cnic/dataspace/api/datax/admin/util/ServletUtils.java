package cn.cnic.dataspace.api.datax.admin.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Http and Servlet Tool Class
 */
public class ServletUtils {

    // Login extension parameter (JSON string) has priority over extension parameter prefix
    public static final String DEFAULT_PARAMS_PARAM = "params";

    // Extension parameter prefix
    public static final String DEFAULT_PARAM_PREFIX_PARAM = "param_";

    // Define static file suffixes; Static File Exclusion URI Address
    private static String[] staticFiles;

    private static String[] staticFileExcludeUri;

    /**
     * Get the current request object
     */
    public static HttpServletRequest getRequest() {
        HttpServletRequest request = null;
        try {
            request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            if (request == null) {
                return null;
            }
            return request;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtain the current corresponding object
     */
    public static HttpServletResponse getResponse() {
        HttpServletResponse response;
        try {
            response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
            if (response == null) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return response;
    }

    /**
     * Obtain the content of the JSON string in the request
     */
    public static String getRequestJsonString(HttpServletRequest request) throws IOException {
        String submitMehtod = request.getMethod();
        // GET
        if (submitMehtod.equals("GET")) {
            if (StrUtil.isNotEmpty(request.getQueryString())) {
                return new String(request.getQueryString().getBytes("iso-8859-1"), "utf-8").replaceAll("%22", "\"");
            } else {
                return new String("".getBytes("iso-8859-1"), "utf-8").replaceAll("%22", "\"");
            }
            // POST
        } else {
            return getRequestPostStr(request);
        }
    }

    /**
     * Description: Gets the byte [] array of post requests
     */
    public static byte[] getRequestPostBytes(HttpServletRequest request) throws IOException {
        int contentLength = request.getContentLength();
        if (contentLength < 0) {
            return null;
        }
        byte[] buffer = new byte[contentLength];
        for (int i = 0; i < contentLength; ) {
            int readlen = request.getInputStream().read(buffer, i, contentLength - i);
            if (readlen == -1) {
                break;
            }
            i += readlen;
        }
        return buffer;
    }

    /**
     * Description: Obtain post request content
     */
    public static String getRequestPostStr(HttpServletRequest request) throws IOException {
        byte[] buffer = getRequestPostBytes(request);
        String charEncoding = request.getCharacterEncoding();
        if (charEncoding == null) {
            charEncoding = "UTF-8";
        }
        return new String(buffer, charEncoding);
    }

    /**
     * Is it an Ajax asynchronous request
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        String accept = request.getHeader("accept");
        if (accept != null && accept.indexOf("application/json") != -1) {
            return true;
        }
        String xRequestedWith = request.getHeader("X-Requested-With");
        if (xRequestedWith != null && xRequestedWith.indexOf("XMLHttpRequest") != -1) {
            return true;
        }
        String uri = request.getRequestURI();
        if (StrUtil.containsAnyIgnoreCase(uri, ".json", ".xml")) {
            return true;
        }
        String ajax = request.getParameter("__ajax");
        if (StrUtil.containsAnyIgnoreCase(ajax, "json", "xml")) {
            return true;
        }
        return false;
    }

    /**
     * Rendering strings to the client
     */
    public static String renderString(HttpServletResponse response, String string) {
        return renderString(response, string, null);
    }

    /**
     * Rendering strings to the client
     */
    public static String renderString(HttpServletResponse response, String string, String type) {
        try {
            // Response. reset()// Comment it out first, otherwise the previously set header will be cleared, such as remember my cookie for ajax login settings
            response.setContentType(type == null ? "application/json" : type);
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Obtain request parameter values
     */
    public static String getParameter(String name) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getParameter(name);
    }

    /**
     * Obtain Request Parameter Map
     */
    public static Map<String, Object> getParameters() {
        return getParameters(getRequest());
    }

    /**
     * Obtain Request Parameter Map
     */
    public static Map<String, Object> getParameters(ServletRequest request) {
        if (request == null) {
            return new HashMap<>();
        }
        return getParametersStartingWith(request, "");
    }

    /**
     * Obtain Request Parameters with the same prefix, copy from spring WebUtils
     */
    @SuppressWarnings("rawtypes")
    public static Map<String, Object> getParametersStartingWith(ServletRequest request, String prefix) {
        Enumeration paramNames = request.getParameterNames();
        Map<String, Object> params = new TreeMap<String, Object>();
        String pre = prefix;
        if (pre == null) {
            pre = "";
        }
        while (paramNames != null && paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            if ("".equals(pre) || paramName.startsWith(pre)) {
                String unprefixed = paramName.substring(pre.length());
                String[] values = request.getParameterValues(paramName);
                if (values == null || values.length == 0) {
                    values = new String[] {};
                    // Do nothing, no values found at all.
                } else if (values.length > 1) {
                    params.put(unprefixed, values);
                } else {
                    params.put(unprefixed, values[0]);
                }
            }
        }
        return params;
    }

    /**
     * Combine Parameters to generate the Parameter section of the Query String, and add prefix to the parameter name
     */
    public static String encodeParameterStringWithPrefix(Map<String, Object> params, String prefix) {
        StringBuilder queryStringBuilder = new StringBuilder();
        String pre = prefix;
        if (pre == null) {
            pre = "";
        }
        Iterator<Entry<String, Object>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();
            queryStringBuilder.append(pre).append(entry.getKey()).append("=").append(entry.getValue());
            if (it.hasNext()) {
                queryStringBuilder.append("&");
            }
        }
        return queryStringBuilder.toString();
    }

    /**
     * Extend parameter data from the request object in JSON or param format_ Starting parameter
     */
    public static Map<String, Object> getExtParams(ServletRequest request) {
        Map<String, Object> paramMap = null;
        String params = StrUtil.trim(request.getParameter(DEFAULT_PARAMS_PARAM));
        if (StrUtil.isNotBlank(params) && StrUtil.startWith(params, "{")) {
            paramMap = (Map) JSONUtil.parseObj(params);
        } else {
            paramMap = getParametersStartingWith(ServletUtils.getRequest(), DEFAULT_PARAM_PREFIX_PARAM);
        }
        return paramMap;
    }

    /**
     * Set the header for the client cache expiration time
     */
    public static void setExpiresHeader(HttpServletResponse response, long expiresSeconds) {
        // Http 1.0 header, set a fix expires date.
        response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + expiresSeconds * 1000);
        // Http 1.1 header, set a time after now.
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + expiresSeconds);
    }

    /**
     * Set a header that prohibits client caching
     */
    public static void setNoCacheHeader(HttpServletResponse response) {
        // Http 1.0 header
        response.setDateHeader(HttpHeaders.EXPIRES, 1L);
        response.addHeader(HttpHeaders.PRAGMA, "no-cache");
        // Http 1.1 header
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0");
    }

    /**
     * Set LastModified Header
     */
    public static void setLastModifiedHeader(HttpServletResponse response, long lastModifiedDate) {
        response.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModifiedDate);
    }

    /**
     * Set Etag Header
     */
    public static void setEtag(HttpServletResponse response, String etag) {
        response.setHeader(HttpHeaders.ETAG, etag);
    }

    /**
     * Calculate whether the file has been modified based on the browser If Modified Since Header
     */
    public static boolean checkIfModifiedSince(HttpServletRequest request, HttpServletResponse response, long lastModified) {
        long ifModifiedSince = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
        if ((ifModifiedSince != -1) && (lastModified < ifModifiedSince + 1000)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return false;
        }
        return true;
    }

    /**
     * Calculate whether Etag is no longer valid based on the browser If None Match Header
     */
    public static boolean checkIfNoneMatchEtag(HttpServletRequest request, HttpServletResponse response, String etag) {
        String headerValue = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (headerValue != null) {
            boolean conditionSatisfied = false;
            if (!"*".equals(headerValue)) {
                StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ",");
                while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
                    String currentToken = commaTokenizer.nextToken();
                    if (currentToken.trim().equals(etag)) {
                        conditionSatisfied = true;
                    }
                }
            } else {
                conditionSatisfied = true;
            }
            if (conditionSatisfied) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                response.setHeader(HttpHeaders.ETAG, etag);
                return false;
            }
        }
        return true;
    }
}
