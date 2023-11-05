package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * Entity used to initiate task reception
 */
@Data
public class TriggerJobDto implements Serializable {

    private String executorParam;

    private int jobId;
}
