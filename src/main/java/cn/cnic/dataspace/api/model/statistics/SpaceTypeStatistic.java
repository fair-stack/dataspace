package cn.cnic.dataspace.api.model.statistics;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Space type information statistics
 */
@Data
@Document(collection = "space_type_statistic")
public class SpaceTypeStatistic {

    @Id
    private String id;

    private int year;

    private int month;

    private int pri;

    private int lim;

    private int pub;

    private int sort;

    private Date createTime;
}
