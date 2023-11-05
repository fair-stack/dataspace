package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * Building JSON DTO
 */
@Data
public class RdbmsReaderDto implements Serializable {

    private String readerSplitPk;

    private String whereParams;

    private String querySql;
}
