package cn.cnic.dataspace.api.model.statistics;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Spatial data information statistics
 */
@Data
@Document(collection = "space_data_statistic")
public class SpaceDataStatistic {

    @Id
    private String id;

    private String spaceId;

    private String spaceName;

    private long personNum;

    // capacity
    private long capacity;

    // Data volume
    private long dataSize;

    // Inflow volume
    private long inSize;

    // Outflow volume
    private long outSize;

    private int fileNum;

    // Space status 0 pending approval 1 normal 2 offline
    private int state;

    private Date createTime;
}
