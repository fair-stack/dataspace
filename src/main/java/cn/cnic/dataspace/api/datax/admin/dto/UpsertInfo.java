package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;

/**
 * Created by mac on 2020/3/16.
 */
@Data
public class UpsertInfo {

    /**
     * When set to true, it means updating for the same upsertKey
     */
    private Boolean isUpsert;

    /**
     * UpsertKey specifies a business primary key with no row records. Used for updates.
     */
    private String upsertKey;
}
