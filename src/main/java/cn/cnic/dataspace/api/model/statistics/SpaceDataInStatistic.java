package cn.cnic.dataspace.api.model.statistics;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Spatial data inflow information statistics
 */
@Data
@Document(collection = "space_data_in_statistic")
public class SpaceDataInStatistic {

    @Id
    private String id;

    private String spaceId;

    private long webData;

    private long fairLinkData;

    private long ftpData;

    private long webDavData;

    private long totalData;

    private int year;

    private int month;

    private int day;

    private Date createTime;
}
