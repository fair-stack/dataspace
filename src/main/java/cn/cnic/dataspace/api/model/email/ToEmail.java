package cn.cnic.dataspace.api.model.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToEmail {

    /**
     * Email recipient, can have multiple people
     */
    private String[] tos;

    /**
     * Email CC party, can have multiple people
     */
    private String[] cc;

    /**
     * Email Subject
     */
    private String subject;

    /**
     * Email Content
     */
    private String content;

    public ToEmail(String[] tos, String subject, String content) {
        this.tos = tos;
        this.subject = subject;
        this.content = content;
    }
}
