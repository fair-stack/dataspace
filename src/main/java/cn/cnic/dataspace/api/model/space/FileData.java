package cn.cnic.dataspace.api.model.space;

import cn.cnic.dataspace.api.model.file.FileMapping;
import lombok.Data;
import org.springframework.data.annotation.Id;
import java.util.List;

/**
 * @ author chl
 */
@Data
public class FileData {

    @Id
    private String id;

    private String hash;

    private String path;

    private String spaceId;

    // 0 files 1 folder
    private int type;

    private List<FileMapping.Data> data;
}
