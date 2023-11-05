package cn.cnic.dataspace.api.model.network;

import lombok.Data;

@Data
public class Auth {

    private String access_token;

    private int expires_in;

    private String refresh_token;

    private String scope;
}
