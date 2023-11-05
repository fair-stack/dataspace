package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.model.email.SysEmail;
import cn.cnic.dataspace.api.model.email.ToEmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.*;

/**
 * @Auther: wdd
 * @Date: 2021/03/17/16:52
 * @Description:
 */
@Slf4j
@Component
public class EmailUtils {

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private MongoTemplate mongoTemplate;

    private static JavaMailSenderImpl javaMailSender = null;

    public String from;

    /**
     * Load
     */
    public synchronized JavaMailSenderImpl getInstance() {
        if (javaMailSender == null) {
            CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
            SysEmail sysEmail = cacheLoading.getSysEmail();
            if (null == sysEmail) {
                return null;
            }
            this.from = sysEmail.getFrom();
            javaMailSender = new JavaMailSenderImpl();
            javaMailSender.setHost(sysEmail.getHost());
            javaMailSender.setPort(sysEmail.getPort());
            javaMailSender.setUsername(sysEmail.getUsername());
            javaMailSender.setPassword(RSAEncrypt.decrypt(sysEmail.getPassword()));
            javaMailSender.setProtocol(sysEmail.getProtocol());
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", true);
            properties.put("mail.smtp.starttls.enable", true);
            properties.put("mail.smtp.starttls.required", true);
            properties.put("mail.smtp.ssl.enable", true);
            javaMailSender.setJavaMailProperties(properties);
        }
        return javaMailSender;
    }

    /**
     * Initialize
     */
    public synchronized static void initInstance() {
        if (javaMailSender != null) {
            javaMailSender = null;
        }
    }

    public String getFrom() {
        return from;
    }

    /**
     * Send template emails using the thymleaf template
     */
    public boolean sendTemplateMail(ToEmail toEmail, Map<String, Object> attachment, String templateName) {
        try {
            List<String> list = new ArrayList<>(Arrays.asList(toEmail.getTos()));
            Iterator<String> iterator = list.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                if (next.equals("admin@dataspace.cn")) {
                    // if (!CommonUtils.sendEmailCheck(next)) {
                    // Log. info ("Invalid email: {}"+next);
                    // iterator.remove();
                    // }
                    iterator.remove();
                }
            }
            if (list.size() < 1) {
                return true;
            }
            MimeMessage mimeMessage = getInstance().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from, attachment.get("org").toString());
            String[] strArray = new String[list.size()];
            helper.setTo(list.toArray(list.toArray(strArray)));
            if (null != toEmail.getCc() && toEmail.getCc().length > 0)
                helper.setCc(toEmail.getCc());
            helper.setSubject(toEmail.getSubject());
            if (attachment.isEmpty()) {
                throw new RuntimeException(CommonUtils.messageInternational("EMAIL_EMPTY"));
            }
            attachment.put("image", image());
            if (javaMailSender.getUsername().equals("")) {
                attachment.put("des", "本邮件为系统自动发送，请勿回复");
            } else {
                attachment.put("des", "");
            }
            Context context = new Context();
            // Define template data
            context.setVariables(attachment);
            helper.setText(templateEngine.process(templateName, context), true);
            getInstance().send(mimeMessage);
            log.info("发送邮件成功：发件人：{},收件人：{},主题：{},时间：{}", "DataSpace", list.toString(), toEmail.getSubject(), new Date());
            return true;
        } catch (Exception e) {
            log.error("模板邮件发送失败->message:{}", e.getMessage());
        }
        return false;
    }

    private String image() {
        String image = "";
        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        Map<String, Object> map = (Map) cacheLoading.loadingConfig();
        if (map != null) {
            image = map.get("logo").toString();
        }
        return image;
    }
}
