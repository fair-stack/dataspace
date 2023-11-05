package cn.cnic.dataspace.api.util;

import lombok.Data;

@Data
public final class EmailModel {

    private String type;

    private String subject;

    private String template;

    private String title;

    private String call;

    private String message;

    private String alert;

    private String button;

    private String alertTo;

    private String end;

    public EmailModel() {
    }

    public EmailModel(String type, String subject, String template, String title, String call, String message, String alert, String button, String alertTo, String end) {
        this.type = type;
        this.subject = subject;
        this.template = template;
        this.title = title;
        this.call = call;
        this.message = message;
        this.alert = alert;
        this.button = button;
        this.alertTo = alertTo;
        this.end = end;
    }

    /**
     * Test email sending
     */
    public static EmailModel EMAIL_TEST_SEND() {
        return new EmailModel("test", "-系统通知】", "error", "邮件发送测试!", "测试人员 您好!", "邮件已成功发送！", "", "", "", "如仍有需要，请联系管理员。");
    }

    /**
     * Space invitation (adding space members)
     */
    public static EmailModel EMAIL_SPACE_INVITE() {
        return new EmailModel("spaceInvite", "-系统通知】", "spaceInvite", "邀请加入通知!", "您好!", "您的朋友name（email）已邀请您加入空间（spaceName）！", "请点击下方按钮进入空间。", "进入空间", "按钮无法工作？请把下面的地址复制到浏览器地址栏进入空间!", "");
    }

    /**
     * Space application
     */
    public static EmailModel EMAIL_APPLY() {
        return new EmailModel("apply", "-系统通知】", "spaceInvite", "待审批提醒!", "dataSpaceName 管理员您好:", "name（email）申请开通空间（spaceName），需要您去审批", "请点击下方按钮，跳转至审批页面进行审批！", "前往审批", "按钮无法工作？请把下面的地址复制到浏览器地址栏进行审批，如果该申请已经被审批过请忽略！", "为了更好的服务平台用户，创造即时高效的协同环境，请您在收到通知的48小时内尽快前往平台进行审批。");
    }

    /**
     * Space expansion
     */
    public static EmailModel EMAIL_CAPACITY() {
        return new EmailModel("capacity", "-系统通知】", "spaceInvite", "待审批提醒!", "dataSpaceName 管理员您好:", "name（email）申请扩容空间（spaceName），需要您去审批", "请点击下方按钮，跳转至审批页面进行审批！", "前往审批", "按钮无法工作？请把下面的地址复制到浏览器地址栏进行审批，如果该申请已经被审批过请忽略！", "为了更好的服务平台用户，创造即时高效的协同环境，请您在收到通知的48小时内尽快前往平台进行审批。");
    }

    /**
     * Data review
     */
    public static EmailModel EMAIL_AUDIT() {
        return new EmailModel("audit", "-系统通知】", "resource", "您提交的数据资源已通过审核!", "name 您好!", "您提交的数据资源《resourceName》已通过审核，可在数据发布里查看详情！", "请点击下方按钮查看详情。", "查看详情", "按钮无法工作？请把下面的地址复制到浏览器地址栏查看详情。", "");
    }

    /**
     * Register Activation
     */
    public static EmailModel EMAIL_REGISTER() {
        return new EmailModel("register", "-系统通知】", "spaceInvite", "待激活通知!", "name 您好! 欢迎您加入 dataSpaceName ！", "您的账号 email 已经申请成功!", "为了保障您的账户安全，我们需要确保您是本邮箱账户的拥有者 请点击下方按钮，来激活并确认您的邮箱地址！", "激活", "按钮无法工作？请把下面的地址复制到浏览器地址栏进行激活。", "请在24小时内激活，否则本次验证将会失效，您需要重新注册验证码。");
    }

    /**
     * Forgot password, reset password, administrator initiated password setting
     */
    public static EmailModel EMAIL_PASS() {
        return new EmailModel("pass", "-系统通知】", "spaceInvite", "设定密码!", "name 您好!", "如果不是您本人申请重设密码，请忽略！", "您申请了重新设定密码，请点击下方按钮完成设定！", "设定密码", "按钮无法工作？请把下面的地址复制到浏览器地址栏进行设定。", "为了保障您的账户安全，请勿将账户密码交给他人使用！");
    }

    /**
     * Administrator Add User
     */
    public static EmailModel EMAIL_INVITE() {
        return new EmailModel("invite", "-系统通知】", "spaceInvite", "邀请加入通知!", "管理员name（email）邀请您加入 dataSpaceName 。", "在 dataSpaceName 您可以归档和分享您的数据，并邀请团队一起工作！", "请点击下方按钮为账户设定密码。", "设定密码", "按钮无法工作？请把下面的地址复制到浏览器地址栏进行设定。", "为了保障您的账户安全，请勿将账户密码交给他人使用!");
    }

    public static EmailModel EMAIL_SUCCESS() {
        return new EmailModel("success", "-系统通知】", "spaceInvite", "申请已通过!", "name 您好!", "您创建空间（spaceName）的申请已通过管理员审批！", "请点击下方按钮，进入数据空间页面！", "进入数据空间", "按钮无法工作？请把下面的地址复制到浏览器地址栏进入数据空间。", "");
    }

    public static EmailModel EMAIL_ERROR() {
        return new EmailModel("error", "-系统通知】", "error", "申请未通过审批!", "name 您好!", "很遗憾您创建空间（spaceName）的申请未通过，未通过原因：", "", "", "", "如仍有需要，请联系管理员。");
    }

    public static EmailModel SPACE_APPLY() {
        // Space application to join email notification
        return new EmailModel("space_apply", "-系统通知】", "spaceInvite", "待审批提醒!", "name 您好!", "username （email）申请加入空间（spaceName），需要您去审批！", "请点击下方按钮，跳转至审批页面进行审批！", "前往审批", "按钮无法工作？请把下面的地址复制到浏览器地址栏进行审批，如果该申请已经被审批过请忽略！", "");
    }

    public static EmailModel APPLY_RESULT_PASS() {
        // Space Application Result Notification - Passed
        return new EmailModel("apply_result", "-系统通知】", "spaceInvite", "申请已通过!", "name 您好!", "您加入空间（spaceName）的申请已通过审批！", "请点击下方按钮进入空间！", "进入空间", "按钮无法工作？请把下面的地址复制到浏览器地址进入空间！", "");
    }

    public static EmailModel APPLY_RESULT_REJECT() {
        // Notification of Space Application Result - Rejection
        return new EmailModel("apply_result", "-系统通知】", "error", "申请未通过审批!", "name 您好!", "很遗憾您加入空间（spaceName）的申请未通过审批,未通过原因：", "", "", "", "如仍有需要，请联系管空间管理人员。");
    }
}
