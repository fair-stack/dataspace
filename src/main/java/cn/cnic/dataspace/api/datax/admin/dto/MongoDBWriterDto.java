package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * Build mongodb write dto
 */
@Data
public class MongoDBWriterDto implements Serializable {

    private UpsertInfo upsertInfo;
}
