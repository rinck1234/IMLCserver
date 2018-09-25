package vip.rinck.web.imlc.push.bean.card;

import com.google.gson.annotations.Expose;
import vip.rinck.web.imlc.push.bean.db.User;

import java.time.LocalDateTime;

public class UserCard {

    @Expose
    private String id;

    @Expose
    private String username;

    @Expose
    private String phone;

    @Expose
    private String portrait;

    @Expose
    private String desc;

    @Expose
    private int sex;

    //用户关注的人的数量
    @Expose
    private int follows;

    //用户粉丝的数量
    @Expose
    private int following;

    //我与当前User的关注状态
    @Expose
    private boolean isFollow;

    //用户最后的更新时间
    @Expose
    private LocalDateTime modifyAt;

    public UserCard(final User user){
        this(user,false);
    }

    public UserCard(final User user,boolean isFollow){
        this.isFollow = isFollow;

        this.id = user.getId();
        this.username = user.getUsername();
        this.phone = user.getPhone();
        this.portrait = user.getPortrait();
        this.desc = user.getDescription();
        this.sex = user.getSex();
        this.modifyAt = user.getUpdateAt();

        //TODO 得到关注人和粉丝的数量
        //user.getFollowers().size();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public int getFollows() {
        return follows;
    }

    public void setFollows(int follows) {
        this.follows = follows;
    }

    public int getFollowing() {
        return following;
    }

    public void setFollowing(int following) {
        this.following = following;
    }

    public boolean isFollow() {
        return isFollow;
    }

    public void setFollow(boolean follow) {
        isFollow = follow;
    }

    public LocalDateTime getModifyAt() {
        return modifyAt;
    }

    public void setModifyAt(LocalDateTime modifyAt) {
        this.modifyAt = modifyAt;
    }
}
