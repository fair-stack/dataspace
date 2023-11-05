package cn.cnic.dataspace.api.model.space.child;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Set;

/**
 * Space
 *
 * @author wangCc
 * @date 2021-03-19 15:35
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "space")
public class SimpleSpace {

    @Id
    private String spaceId;

    private String spaceName;

    private String userId;

    // Whether to disclose 0 or not; 1 Public
    private Integer isPublic;

    // Allow application 0 not allowed 1 allowed
    private int applyIs;

    // Have you applied for it
    private Integer haveApply;

    // Space - Document Label
    private List<String> tags;

    // Space logo
    private String spaceLogo;

    private String description;

    private String createDateTime;

    private String spaceRole;

    // Everyone has permission//manage Ordinary users do not have permission to edit or delete
    private String fileRole;

    // light //dark
    private String topic;

    private String homeUrl;

    private String markdown;

    private String owned;

    private Set<AuthorizationPerson> authorizationList;

    // Space capacity 10 * 1024 * 1024 * 1024=10G
    private Long spaceSize;
    // Private Long spaceUsageSize// Space used
}
