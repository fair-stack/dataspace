package cn.cnic.dataspace.api.model.statistics;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Space Weekly Access Statistics
 */
@Data
@Document(collection = "space_statistic")
public class SpaceStatistic {

    @Id
    private String id;

    private String spaceId;

    private long download;

    private long memberCount;

    private long viewCount;

    private int year;

    private int month;

    private int day;

    private Date createTime;
}
