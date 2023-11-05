package cn.cnic.dataspace.api.model.space;

import cn.cnic.dataspace.api.model.space.child.Operator;
import lombok.Data;
import java.util.Date;

@Data
public class SpaceLog {

    private String spaceSvnId;

    private String spaceId;

    /**
     * -2 User behavior
     */
    private long version;

    private Operator operator;

    private String description;

    private Date createTime;

    private String updateDateTime;

    private String method;

    private String tag;

    private String action;

    private String fileAction;

    private String operatorId;
}
