package cn.cnic.dataspace.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Cached data (homepage)
 */
@Data
@Document(collection = "cache_data")
public class CacheData {

    @Id
    private String id;

    // Total data volume
    private String fileSize;

    private long fileSizeLong;

    // Number of spaces
    private long spaceCount;

    // Number of users
    private long userCount;

    // Download volume
    private String downloadCount;

    // Number of Releases
    private long publicCount;

    // Number of Releases
    private long accTotal;

    // Hotest Space ID
    private String hotSpaceId;

    public CacheData(String fileSize, long spaceCount, long userCount, String downloadCount, long publicCount, long accTotal, String hotSpaceId) {
        this.fileSize = fileSize;
        this.spaceCount = spaceCount;
        this.userCount = userCount;
        this.downloadCount = downloadCount;
        this.publicCount = publicCount;
        this.accTotal = accTotal;
        this.hotSpaceId = hotSpaceId;
    }

    public CacheData() {
    }
}
