package vip.rinck.web.imlc.push.bean.db;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户关系的Model
 * 记录用户直接的好友关系实现
 */

@Entity
@Table(name = "TB_USER_FOLLOW")
public class UserFollow {

    @Id
    @PrimaryKeyJoinColumn
    //主键生成存储的类型为UUID
    @GeneratedValue(generator = "uuid")
    //把uuid的生成器定义为uuid2，uuid2是常规的UUID
    @GenericGenerator(name = "uuid",strategy = "uuid2")
    @Column(updatable = false,nullable = false)
    private String id;

    //定义一个发起人
    //多对1 可以关注多人，每个关注是一条记录
    @ManyToOne(optional = false)
    //定义关联的表字段名为originId，对应User.id
    //定义数据库中存储的字段
    @JoinColumn(name = "originId")
    private User origin;

    //把这个列提取到Model中,不允许null，不允许更新，插入
    @Column(nullable = false,updatable = false,insertable = false)
    private String originId;

    //定义关注的目标
    @ManyToOne(optional = false)
    //定义关联的表字段名为targetId，对应User.id
    //定义数据库中存储的字段
    @JoinColumn(name = "targetId")
    private User target;

    //把这个列提取到Model中,不允许null，不允许更新，插入
    @Column(nullable = false,updatable = false,insertable = false)
    private String targetId;


    //别名，对关注对象的备注
    @Column
    private String alias;

    //定义为创建时间戳，在创建时写入
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createAt = LocalDateTime.now();

    //定义为更新时间戳，在创建时写入
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateAt = LocalDateTime.now();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getOrigin() {
        return origin;
    }

    public void setOrigin(User origin) {
        this.origin = origin;
    }

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public User getTarget() {
        return target;
    }

    public void setTarget(User target) {
        this.target = target;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }
}
