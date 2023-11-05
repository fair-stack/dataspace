package cn.cnic.dataspace.api.elfinder.config;

import cn.cnic.dataspace.api.elfinder.core.ElfinderContext;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageFactory;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import lombok.Data;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ContextImpl for anonymous inner class avoid transforming parameter
 *
 * @author wangCc
 * @date 2022-01-07 15:52
 */
@Data
public class ContextImpl implements ElfinderContext {

    private HttpServletRequest request;

    private HttpServletResponse response;

    private ElfinderStorageService elfinderStorageService;

    public ContextImpl(HttpServletRequest request, HttpServletResponse response, ElfinderStorageService elfinderStorageService) {
        this.request = request;
        this.response = response;
        this.elfinderStorageService = elfinderStorageService;
    }

    @Override
    public ElfinderStorageFactory getVolumeSourceFactory() {
        return elfinderStorageService.getElfinderStorageFactory(getRequest(), getResponse());
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public HttpServletResponse getResponse() {
        return response;
    }
}
