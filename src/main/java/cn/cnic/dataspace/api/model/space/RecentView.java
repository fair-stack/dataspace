package cn.cnic.dataspace.api.model.space;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;

/**
 * RecentView
 *
 * @author wangCc
 * @date 2021-10-09 15:52
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recent")
public class RecentView implements Serializable {

    @Id
    @ApiModelProperty(hidden = true)
    private String recentId;

    private String email;

    private String spaceId;

    private String spaceName;

    private String spaceLogo;

    private String homeUrl;

    private String dateTime;
}
