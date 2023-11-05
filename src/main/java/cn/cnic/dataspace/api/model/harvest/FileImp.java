package cn.cnic.dataspace.api.model.harvest;

import lombok.Data;
import java.util.List;

@Data
public class FileImp {

    private String taskId;

    private String rootId;

    private String fileName;

    private String path;

    private long size;

    private Boolean file;

    private List<FileImp> fileImpList;
}
