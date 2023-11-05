package cn.cnic.dataspace.api.util;

import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import java.io.IOException;

public class CheckEmail {

    // "no-reply@domain.com";
    public static final String SENDER_EMAIL = "";

    // "domain.com";
    public static final String SENDER_EMAIL_SERVER = SENDER_EMAIL.split("@")[1];

    /**
     * @param email  The recipient's email address, it need to be validate if it is real exists or doesn't exists.
     * @return True if email is real exists, false if not.
     */
    public boolean checkEmailMethod(String email) {
        if (!email.matches("[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+")) {
            System.err.println("Format error");
            return false;
        }
        String host = "";
        String hostName = email.split("@")[1];
        Record[] result = null;
        SMTPClient client = new SMTPClient();
        // Set the connection timeout, some servers are slower
        client.setConnectTimeout(80000);
        try {
            // Find MX records
            Lookup lookup = new Lookup(hostName, Type.MX);
            lookup.run();
            if (lookup.getResult() != Lookup.SUCCESSFUL) {
                return false;
            } else {
                result = lookup.getAnswers();
            }
            /*If (result. length>1) {//Priority sorting*/
            // Connect to mailbox server
            // for (int i = 0; i < result.length; i++) {
            // System.out.println(result[i].getAdditionalName().toString());
            // System.out.println(((MXRecord)result[i]).getPriority());
            // }
            int count = 0;
            for (int i = 0; i < result.length; i++) {
                host = result[i].getAdditionalName().toString();
                try {
                    // Connect to the email server that receives the email address
                    client.connect(host);
                } catch (Exception e) {
                    // Catch exceptions thrown when connection timeout occurs
                    count++;
                    if (count >= result.length) {
                        // If the result servers obtained by MX cannot connect, the email is considered invalid
                        return false;
                    }
                }
                if (!SMTPReply.isPositiveCompletion(client.getReplyCode())) {
                    // Server communication unsuccessful
                    client.disconnect();
                    continue;
                } else {
                    // HELO <$SENDER_EMAIL_SERVER>   //domain.com
                    try {
                        // This step may result in a null pointer exception
                        client.login(SENDER_EMAIL_SERVER);
                    } catch (Exception e) {
                        return false;
                    }
                    client.setSender(SENDER_EMAIL);
                    if (client.getReplyCode() != 250) {
                        // To address the issue of MX in Hotmail, there may be=550 OU-001 (SNT004-MC1F43) Unfortunate, messages from 116.246.2.245 were not sent
                        client.disconnect();
                        // Place client.login and client.setSender within the loop, so that if one mx fails, all other mx will be replaced. However, this will perform all mx traversals on invalid mailboxes, which takes time
                        continue;
                    }
                    // RCPT TO: <$email>
                    try {
                        client.addRecipient(email);
                    } catch (Exception e) {
                        return false;
                    }
                    // Finally, true is returned from the recipient email server, indicating that the recipient address can be found in the server and the email is valid
                    if (250 == client.getReplyCode()) {
                        return true;
                    }
                    client.disconnect();
                }
            }
            // log+=tempLog;
            // log += ">MAIL FROM: <"+SENDER_EMAIL+">\n";
            // log += "=" + client.getReplyString();
            // 
            // // RCPT TO: <$email>
            // try{
            // client.addRecipient(email);
            // }catch(Exception e){
            // return false;
            // }
            // log += ">RCPT TO: <" + email + ">\n";
            // log += "=" + client.getReplyString();
            // 
            // //Finally, return true from the recipient email server, indicating that the server can find this recipient address and the email is valid
            // if (250 == client.getReplyCode()) {
            // return true;
            // }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
            }
        }
        return false;
    }

    /**
     * This method is more accurate than checkEmailMethod(String email);
     *
     * @param email  The recipient's email address, it need to be validate if it is real exists or doesn't exists.
     * @return True if email is real exists, false if not.
     */
    public boolean checkEmail(String email) {
        if (!email.matches("[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+")) {
            System.err.println("Format error");
            return false;
        }
        if (email.split("@")[1].equals("qq.com")) {
            if (checkEmailMethod(email) && checkEmailMethod(email) && checkEmailMethod(email)) {
                return true;
            } else {
                return false;
            }
        }
        return checkEmailMethod(email);
    }
}
