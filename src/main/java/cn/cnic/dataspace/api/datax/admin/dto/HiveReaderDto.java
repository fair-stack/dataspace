package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * Building a hive reader dto
 */
@Data
public class HiveReaderDto implements Serializable {

    private String readerPath;

    private String readerDefaultFS;

    private String readerFileType;

    private String readerFieldDelimiter;

    private Boolean readerSkipHeader;
}
