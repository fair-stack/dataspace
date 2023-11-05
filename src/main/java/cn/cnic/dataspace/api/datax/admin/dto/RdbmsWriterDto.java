package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * Building JSON DTO
 */
@Data
public class RdbmsWriterDto implements Serializable {

    private String dbName;

    private String preSql;

    private String postSql;
}
