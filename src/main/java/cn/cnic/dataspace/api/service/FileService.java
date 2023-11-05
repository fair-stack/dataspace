package cn.cnic.dataspace.api.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * file service
 *
 * @author wangCc
 * @date 2021-03-18 18:23
 */
public interface FileService {

    void download(String code, HttpServletRequest request, HttpServletResponse response);
}
