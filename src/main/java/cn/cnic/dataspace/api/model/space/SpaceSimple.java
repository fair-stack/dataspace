package cn.cnic.dataspace.api.model.space;

import lombok.Data;

/**
 * @ author chl
 */
@Data
public class SpaceSimple {

    public SpaceSimple() {
    }

    public SpaceSimple(String spaceId, String spaceName, String filePath, String state) {
        this.spaceId = spaceId;
        this.spaceName = spaceName;
        this.filePath = filePath;
        this.state = state;
    }

    private String spaceId;

    private String spaceName;

    private String filePath;

    private String state;
}
