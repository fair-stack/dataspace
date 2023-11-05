package cn.cnic.dataspace.api.model.backup;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class RequestAdd {

    // Update or stop/start
    private String id;

    @NotNull(message = "spaceId is not null")
    private String // Space ID
    spaceId;

    @NotNull(message = "status is not null")
    private String // Status start stop
    status;

    // Backup mode
    private String strategy;

    // Strategy day week month
    private String executionCycle;

    // Time 13:23:22
    private String time;

    // Monthly (1-31) (last day transmitted)//Weekly (1-7)
    private String value;
}
