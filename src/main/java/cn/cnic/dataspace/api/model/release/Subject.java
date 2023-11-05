package cn.cnic.dataspace.api.model.release;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "subject")
public class Subject {

    private String one_rank_no;

    private String one_rank_name;

    private String two_rank_no;

    private String two_rank_name;

    private String three_rank_no;

    private String three_rank_name;
}
