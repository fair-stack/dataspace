package cn.cnic.dataspace.api.util;

import com.google.code.kaptcha.text.impl.DefaultTextCreator;

public class KaptchaTextCreator extends DefaultTextCreator {

    @Override
    public String getText() {
        return CommonUtils.getVerificationCode(4);
    }
}
