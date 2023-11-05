package cn.cnic.dataspace.api.model.release.instdb;

import lombok.Data;
import java.util.Map;

@Data
public class SendInsertDB {

    private String version;

    private Map<String, Object> organization;

    private Map<String, Object> publish;

    private String resourceType;

    private String rootId;

    private String templateName;

    private String fileIsZip = "no";

    private Map<String, Object> dataType;

    private Map<String, Object> callbackUrl;

    private Map<String, Object> metadata;
}
