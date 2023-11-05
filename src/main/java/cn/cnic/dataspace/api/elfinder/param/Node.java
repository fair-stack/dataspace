package cn.cnic.dataspace.api.elfinder.param;

import lombok.Data;

@Data
public class Node {

    // private String source;
    private String alias;

    private String path;

    private Boolean isDefault;

    private String locale;

    private Constraint constraint;
}
