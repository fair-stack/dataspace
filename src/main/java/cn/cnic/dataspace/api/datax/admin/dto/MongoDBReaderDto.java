package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * Building mongodb reader dto
 */
@Data
public class MongoDBReaderDto implements Serializable {

    private String queryJson;
}
