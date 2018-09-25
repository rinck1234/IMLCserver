package vip.rinck.web.imlc.push.bean.api.account;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;

public class RegisterModel {
    @Expose
    private String account;
    @Expose
    private String password;
    @Expose
    private String username;
    @Expose
    private String pushId;

    public String getPushId() {
        return pushId;
    }

    public void setPushId(String pushId) {
        this.pushId = pushId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    //校验
    public static boolean check(RegisterModel model){
        return model!=null
                && !Strings.isNullOrEmpty(model.account)
                && !Strings.isNullOrEmpty(model.password)
                && !Strings.isNullOrEmpty(model.username);
    }
}
