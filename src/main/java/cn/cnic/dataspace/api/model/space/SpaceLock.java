package cn.cnic.dataspace.api.model.space;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * SpaceLock
 *
 * @author wangCc
 * @date 2021-11-08 19:10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "spaceLock")
public class SpaceLock {

    @Id
    private String id;

    private String spaceId;

    private int lock;
}
