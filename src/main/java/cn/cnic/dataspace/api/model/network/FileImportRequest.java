package cn.cnic.dataspace.api.model.network;

import lombok.Data;
import java.util.List;

@Data
public class FileImportRequest {

    private String spaceId;

    private String hash;

    private List<Long> fileIds;
}
