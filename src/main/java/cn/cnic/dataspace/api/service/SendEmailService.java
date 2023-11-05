package cn.cnic.dataspace.api.service;

public interface SendEmailService {

    // Space invitation
    void sendInviteEmail(String token, String email, String spaceId, String role);
}
