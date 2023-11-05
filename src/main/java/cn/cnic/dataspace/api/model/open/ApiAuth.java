package cn.cnic.dataspace.api.model.open;

import lombok.Data;

/**
 * API authorization management
 */
@Data
public class ApiAuth {

    private String appName;

    private String appKey;

    // Authorized person
    private String authorizer;

    // Long term, short term
    private String authType;

    // Short term maturity
    private String authTime;

    // Expiration true Expiration false Not expired
    private boolean expire;
}
