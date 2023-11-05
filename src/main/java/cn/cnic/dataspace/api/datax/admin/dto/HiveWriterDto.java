package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * Build hive write dto
 */
@Data
public class HiveWriterDto implements Serializable {

    private String writerDefaultFS;

    private String writerFileType;

    private String writerPath;

    private String writerFileName;

    private String writeMode;

    private String writeFieldDelimiter;
}
