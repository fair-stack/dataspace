package cn.cnic.dataspace.api.datax.admin.tool.datax;

import java.util.Map;

/**
 * Build the basic interface for com.wugui.datax JSON
 */
public interface DataxJsonInterface {

    Map<String, Object> buildJob();

    Map<String, Object> buildSetting();

    Map<String, Object> buildContent();

    Map<String, Object> buildReader();

    Map<String, Object> buildHiveReader();

    Map<String, Object> buildHiveWriter();

    Map<String, Object> buildMongoDBReader();

    Map<String, Object> buildMongoDBWriter();

    Map<String, Object> buildWriter();
}
