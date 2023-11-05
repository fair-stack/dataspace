package cn.cnic.dataspace.api.model.open;

import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import lombok.Data;
import org.springframework.data.annotation.Id;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class SpaceInfo {

    @Id
    private String id;

    private String spaceName;

    // Space type
    private String type;

    // Space - Document Label
    private List<String> tags;

    // Description of space document
    private String description;

    // Space logo
    private String spaceLogo;

    private Set<AuthorizationPerson> authorizationList;

    // Space capacity 1024 * 1024 * 1024 * 1024 * 1024=1T
    private Long spaceSize;

    private String spaceCode;

    // Upload method ftp webDav
    private Map<String, String> uploadLink;

    private String createDateTime;

    private String updateDateTime;
}
