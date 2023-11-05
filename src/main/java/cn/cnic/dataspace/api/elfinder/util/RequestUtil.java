package cn.cnic.dataspace.api.elfinder.util;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RequestUtil
 *
 * @author wangCc
 * @date 2021-11-15 20:27
 */
public class RequestUtil {

    public static final String OPEN_STREAM = "openStream";

    public static final String GET_PARAMETER = "getParameter";

    public HttpServletRequest processMultipartContent(final HttpServletRequest request) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request)) {
            return request;
        }
        Map<String, String[]> map = request.getParameterMap();
        final Map<String, Object> requestParams = new HashMap<>();
        for (String key : map.keySet()) {
            String[] obj = map.get(key);
            if (obj.length == 1) {
                requestParams.put(key, obj[0]);
            } else {
                requestParams.put(key, obj);
            }
        }
        AbstractMultipartHttpServletRequest multipartHttpServletRequest = (AbstractMultipartHttpServletRequest) request;
        ServletFileUpload servletFileUpload = new ServletFileUpload();
        String characterEncoding = request.getCharacterEncoding();
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        servletFileUpload.setHeaderEncoding(characterEncoding);
        List<MultipartFile> fileList = multipartHttpServletRequest.getFiles("upload[]");
        List<FileItemStream> listFiles = new ArrayList<>();
        for (MultipartFile file : fileList) {
            FileItemStream item = createFileItemStream(file);
            InputStream stream = item.openStream();
            String fileName = item.getName();
            if (fileName != null && !fileName.trim().isEmpty()) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                IOUtils.copy(stream, os);
                final byte[] bs = os.toByteArray();
                stream.close();
                listFiles.add((FileItemStream) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { FileItemStream.class }, (proxy, method, args) -> {
                    if (OPEN_STREAM.equals(method.getName())) {
                        return new ByteArrayInputStream(bs);
                    }
                    return method.invoke(item, args);
                }));
            }
        }
        request.setAttribute(FileItemStream.class.getName(), listFiles);
        return (HttpServletRequest) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { HttpServletRequest.class }, (arg0, arg1, arg2) -> {
            if (GET_PARAMETER.equals(arg1.getName())) {
                return requestParams.get(arg2[0]);
            }
            return arg1.invoke(request, arg2);
        });
    }

    public FileItemStream createFileItemStream(MultipartFile file) {
        return new FileItemStream() {

            @Override
            public InputStream openStream() throws IOException {
                return file.getInputStream();
            }

            @Override
            public String getContentType() {
                return file.getContentType();
            }

            @Override
            public String getName() {
                return file.getOriginalFilename();
            }

            @Override
            public String getFieldName() {
                return file.getName();
            }

            @Override
            public boolean isFormField() {
                return false;
            }

            @Override
            public FileItemHeaders getHeaders() {
                return null;
            }

            @Override
            public void setHeaders(FileItemHeaders fileItemHeaders) {
            }
        };
    }
}
