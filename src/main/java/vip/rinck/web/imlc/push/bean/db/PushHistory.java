package vip.rinck.web.imlc.push.bean.db;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 消息推送历史记录表
 */
@Entity
@Table(name = "TB_PUSH_HISTORY")
public class PushHistory {

    //主键
    @Id
    @PrimaryKeyJoinColumn
    //主键生成存储的类型为UUID
    @GeneratedValue(generator = "uuid")
    //把uuid的生成器定义为uuid2，uuid2是常规的UUID
    @GenericGenerator(name = "uuid",strategy = "uuid2")
    @Column(updatable = false,nullable = false)
    private String id;

    //推送的具体实体存储的都是JSON字符串
    @Column(nullable = false,columnDefinition = "BLOB")
    private String entity;

    //推送的实体类型
    @Column(nullable = false)
    private int entityType;

    //接收者不允许为空
    //接收者可接收很多推送消息
    //加载一条推送消息 同时加载用户信息
    @ManyToOne(optional = false,fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    @JoinColumn(name = "receiverId")
    private User receiver;
    @Column(nullable = false,updatable = false,insertable = false)
    private String receiverId;


    //发送者可为空
    //发送者可发送很多推送消息
    //加载一条推送消息 同时加载用户信息
    @ManyToOne(fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    @JoinColumn(name = "senderId")
    private User sender;
    @Column(nullable = false,updatable = false,insertable = false)
    private String senderId;

    //接收者当前状态下的设备推送ID
    @Column
    private String receiverPushId;

    //定义为创建时间戳，在创建时写入
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createAt = LocalDateTime.now();

    //定义为更新时间戳，在创建时写入
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateAt = LocalDateTime.now();

    //消息送达时间，可为空
    @Column
    private LocalDateTime arrivalAt = LocalDateTime.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public int getEntityType() {
        return entityType;
    }

    public void setEntityType(int entityType) {
        this.entityType = entityType;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverPushId() {
        return receiverPushId;
    }

    public void setReceiverPushId(String receiverPushId) {
        this.receiverPushId = receiverPushId;
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

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public void setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
    }
}
