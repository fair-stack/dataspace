package cn.cnic.dataspace.api.model.release;

import cn.cnic.dataspace.api.util.CommonUtils;
import lombok.Data;
import java.util.List;

@Data
public class SendFile {

    private String fileId = CommonUtils.generateUUID();

    private String fileName;

    private long size;

    private List<SendFile> children;

    private Boolean isFile;
}
