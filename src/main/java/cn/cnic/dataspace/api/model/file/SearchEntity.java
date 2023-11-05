package cn.cnic.dataspace.api.model.file;

import lombok.Data;

@Data
public class SearchEntity {

    private String hash;

    private String phash;

    private String name;

    private Boolean isNull;

    private Boolean notFolder;

    private String mime;
}
