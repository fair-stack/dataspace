package cn.cnic.dataspace.api.datax.admin.service;

import cn.cnic.dataspace.api.datax.admin.dto.DataXJsonBuildDto;

/**
 * Com.wugui.datax json Building Service Layer Interface
 */
public interface DataxJsonService {

    /**
     * build datax json
     *
     * @param dto
     * @return
     */
    String buildJobJson(DataXJsonBuildDto dto);
}
