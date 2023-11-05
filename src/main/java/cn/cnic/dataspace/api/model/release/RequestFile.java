package cn.cnic.dataspace.api.model.release;

import lombok.Data;
import java.util.List;

@Data
public class RequestFile {

    private String dirs;

    private String hash;

    private String phash;

    private String icon;

    private String name;

    private String suffix;

    private String mime;

    private List<RequestFile> children;
}
