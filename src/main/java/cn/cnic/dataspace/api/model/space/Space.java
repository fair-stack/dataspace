package cn.cnic.dataspace.api.model.space;

import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

/**
 * Space
 *
 * @author wangCc
 * @date 2021-03-19 15:35
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "space")
public class Space {

    @Id
    private String spaceId;

    @NotNull(message = "空间名不能为空")
    private String spaceName;

    @JsonIgnore
    private String // File Path
    filePath;

    // File identification
    private String spaceShort;

    private String userId;

    // Whether to disclose 0 or not; 1 Public
    private Integer isPublic;

    // Allow application 0 not allowed 1 allowed
    private int applyIs;

    // Have you applied for it
    private int haveApply;

    // Space - Document Label
    private List<String> tags;

    @NotNull(message = "描述不能为空")
    private String // Description of space document
    description;

    @NotNull(message = "封面不能为空")
    private String // Space logo
    spaceLogo;

    private String createDateTime;

    private String updateDateTime;

    private Set<AuthorizationPerson> authorizationList;

    // Space capacity 1024 * 1024 * 1024 * 1024 * 1024=1T
    private Long spaceSize;

    // Spatial identifier
    private String identifier;

    // light //dark
    private String topic;

    private String homeUrl;

    private String markdown;

    private String owned;

    // Private Long spaceUsageSize// Space used
    private String userName;

    private String userAvatar;

    // Space status 0 pending approval 1 normal 2 offline
    private String state;

    // Everyone has permission//manage Ordinary users do not have permission to edit or delete
    private String fileRole;

    // Number of members
    private int memberCount;

    // Number of Releases
    private int dataSetCount;

    // View volume
    private long viewCount;

    // Download size
    private long downSize;

    // downCount
    // Number of downloads
    private long download;

    private String dbName;
}
