package cn.cnic.dataspace.api.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Properties;

@Configuration
public class CaptchaConfig {

    @Bean(name = "captchaProducerMath")
    public DefaultKaptcha getKaptchaBeanMath() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // Is there a border that defaults to true? We can set 'yes' or' no 'ourselves
        properties.setProperty("kaptcha.border", "yes");
        // The default border color is Color.BLACK
        properties.setProperty("kaptcha.border.color", "105,179,90");
        // The default color for verification code text characters is Color.BLACK
        properties.setProperty("kaptcha.textproducer.font.color", "blue");
        // The default width of the verification code image is 200
        properties.setProperty("kaptcha.image.width", "160");
        // The default height of the verification code image is 50
        properties.setProperty("kaptcha.image.height", "60");
        // The default character size for the verification code text is 40
        properties.setProperty("kaptcha.textproducer.font.size", "35");
        // KAPTCHA_SESSION_KEY
        properties.setProperty("kaptcha.session.key", "kaptchaCodeMath");
        // Verification code text generator
        properties.setProperty("kaptcha.textproducer.impl", "cn.cnic.dataspace.api.util.KaptchaTextCreator");
        // The default character spacing for verification code text is 2
        properties.setProperty("kaptcha.textproducer.char.space", "3");
        // The default character length for the verification code text is 5
        properties.setProperty("kaptcha.textproducer.char.length", "6");
        // The font style of the verification code text defaults to new Font ("Arial", 1, fontSize), new Font ("Courier", 1,
        // fontSize)
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Courier");
        // The verification code noise color defaults to Color.BLACK
        properties.setProperty("kaptcha.noise.color", "white");
        // Interference implementation class
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");
        // Image Style Water Ripple com.google.code.kaptcha.impl.WaterRipple
        // FishEyeGimpy
        // Shadow com.google.code.kaptcha.impl.ShadowGimpy
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.WaterRipple");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
