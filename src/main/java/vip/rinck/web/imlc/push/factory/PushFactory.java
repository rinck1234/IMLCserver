package vip.rinck.web.imlc.push.factory;

import com.google.common.base.Strings;
import vip.rinck.web.imlc.push.bean.api.base.PushModel;
import vip.rinck.web.imlc.push.bean.card.GroupMemberCard;
import vip.rinck.web.imlc.push.bean.card.MessageCard;
import vip.rinck.web.imlc.push.bean.card.UserCard;
import vip.rinck.web.imlc.push.bean.db.*;
import vip.rinck.web.imlc.push.utils.Hib;
import vip.rinck.web.imlc.push.utils.PushDispatcher;
import vip.rinck.web.imlc.push.utils.TextUtil;

import javax.xml.soap.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息存储于处理的工具类
 */
public class PushFactory {
    //发送一条消息，并在当前的发送历史中存储记录
    public static void pushNewMessage(User sender, Message message) {
        if(sender==null||message==null)
            return;

        //消息卡片用于发送
        MessageCard card = new MessageCard(message);
        //要推送的字符串
        String entity = TextUtil.toJson(card);

        //发送者
        PushDispatcher dispatcher = new PushDispatcher();

        if(message.getGroup()==null&& Strings.isNullOrEmpty(message.getGroupId())){
            //给朋友发送消息
            User receiver = UserFactory.findById(message.getReceiverId());
            if(receiver==null)
                return;

            //历史记录字段表建立
            PushHistory history = new PushHistory();
            //普通消息类型
            history.setEntityType(PushModel.ENTITY_TYPE_MESSAGE);
            history.setEntity(entity);
            history.setReceiver(receiver);
            //接收者当前的设备推送Id
            history.setReceiverPushId(receiver.getPushId());

            //推送的真实Model
            PushModel pushModel = new PushModel();
            //每一条历史记录都是独立的，可以单独发送
            pushModel.add(history.getEntityType(),history.getEntity());

            //把需要发送的数据，丢给发送者进行发送
            dispatcher.add(receiver,pushModel);

            //保存到数据库
            Hib.queryOnly(session -> session.save(history));


        }else {
            Group group = message.getGroup();
            //因为延迟加载，可能为空，需要通过Id查询
            if(group==null)
                group = GroupFactory.findById(message.getGroupId());

            //如果未找到群，则返回
            if(group==null)
                return;

            //给群成员发送消息
            Set<GroupMember> members = GroupFactory.getMembers(group);
            if(members==null||members.size()==0)
                return;

            //过滤自己
            members = members.stream()
                    .filter(groupMember -> !groupMember.getUserId()
                            .equalsIgnoreCase(sender.getId()))
                    .collect(Collectors.toSet());

            if(members.size()==0)
                return;

            //一个历史记录列表
            List<PushHistory> histories = new ArrayList<>();

            addGroupMembersPushModel(dispatcher,//推送的发送器
                    histories,//数据要存储的列表
                    members,//所有成员
                    entity,//要发送的数据
                    PushModel.ENTITY_TYPE_MESSAGE);//发送的类型

            //保存到数据库的操作
            Hib.queryOnly(session -> {
                for (PushHistory history : histories) {
                    session.saveOrUpdate(history);
                }
            });

        }

        //发送者进行真实的提交
        dispatcher.submit();
    }

    /**
     * 给群成员构建一个消息
     * 把消息存储到数据库中，每个人，每条消息都是一个记录
     * @param dispatcher
     * @param histories
     * @param members
     * @param entity
     * @param entityTypeMessage
     */
    private static void addGroupMembersPushModel(PushDispatcher dispatcher,
                                                 List<PushHistory> histories,
                                                 Set<GroupMember> members,
                                                 String entity,
                                                 int entityTypeMessage) {

        for(GroupMember member:members){
            //无需通过Id再找用户
            User receiver = member.getUser();
            if(receiver==null)
                return;

            //历史记录字段表建立
            PushHistory history = new PushHistory();
            //普通消息类型
            history.setEntityType(PushModel.ENTITY_TYPE_MESSAGE);
            history.setEntity(entity);
            history.setReceiver(receiver);
            //接收者当前的设备推送Id
            history.setReceiverPushId(receiver.getPushId());
            histories.add(history);


            PushModel pushModel = new PushModel();
            pushModel.add(history.getEntityType(),history.getEntity());

            //添加到发送者的数据集中
            dispatcher.add(receiver,pushModel);
        }
    }

    /**
     * 通知新成员 被加入群
     * @param members 被加入群的成员
     */
    public static void pushJoinGroup(Set<GroupMember> members) {
        //发送者
        PushDispatcher dispatcher = new PushDispatcher();

        //历史记录表
        List<PushHistory> histories = new ArrayList<>();

        for(GroupMember member:members){
            //无需通过Id再找用户
            User receiver = member.getUser();
            if(receiver==null)
                return;

            //每个成员的信息卡片
            GroupMemberCard memberCard = new GroupMemberCard(member);
            String entity = TextUtil.toJson(memberCard);

            //历史记录字段表建立
            PushHistory history = new PushHistory();
            //被加入群类型
            history.setEntityType(PushModel.ENTITY_TYPE_ADD_GROUP);
            history.setEntity(entity);
            history.setReceiver(receiver);
            //接收者当前的设备推送Id
            history.setReceiverPushId(receiver.getPushId());
            histories.add(history);

            //构建消息model
            PushModel pushModel = new PushModel()
                    .add(history.getEntityType(),history.getEntity());

            //添加到发送者的数据集中
            dispatcher.add(receiver,pushModel);
            histories.add(history);
        }

        //保存到数据库的操作
        Hib.queryOnly(session -> {
            for (PushHistory history : histories) {
                session.saveOrUpdate(history);
            }
        });

        //提交发送
        dispatcher.submit();

    }

    /**
     * 通知老成员有新成员加入群
     * @param oldMembers 老的成员
     * @param insertCards 新的成员
     */
    public static void pushGroupMemberAdd(Set<GroupMember> oldMembers, Set<GroupMember> insertCards) {

        //发送者
        PushDispatcher dispatcher = new PushDispatcher();

        //历史记录表
        List<PushHistory> histories = new ArrayList<>();

        //当前新增的用户集合Json字符串
        String entity = TextUtil.toJson(insertCards);

        //进行循环添加，给oldMembers每一个老用户构建一个消息，消息的内容为新增的用户的集合
        //通知的类型是 新增群成员类型
        addGroupMembersPushModel(dispatcher,histories,oldMembers,
                entity,PushModel.ENTITY_TYPE_ADD_GROUP_MEMBERS);

        //保存到数据库的操作
        Hib.queryOnly(session -> {
            for (PushHistory history : histories) {
                session.saveOrUpdate(history);
            }
        });

        //提交发送
        dispatcher.submit();
    }

    /**
     * 推送账户退出消息
     * @param receiver 接收者
     * @param pushId 此时接收者的设备Id
     */
    public static void pushLogout(User receiver, String pushId) {
        //历史记录字段表建立
        PushHistory history = new PushHistory();
        //被加入群类型
        history.setEntityType(PushModel.ENTITY_TYPE_LOGOUT);
        history.setEntity("Account logout!!!");
        history.setReceiver(receiver);
        //接收者当前的设备推送Id
        history.setReceiverPushId(pushId);
        //保存到历史记录表
        Hib.queryOnly(session -> session.save(history));

        //发送者
        PushDispatcher dispatcher = new PushDispatcher();
        PushModel pushModel = new PushModel()
                .add(history.getEntityType(),history.getEntity());
        //添加并提交到第三方推送
        dispatcher.add(receiver,pushModel);
        dispatcher.submit();
    }

    /**
     * 给关注对象推送我的信息
     * 类型：我关注了TA
     * @param receiver 接收者
     * @param userCard 我的卡片信息
     */
    public static void pushFollow(User receiver, UserCard userCard) {
        //一定是相互关注了
        userCard.setFollow(true);
        String entity = TextUtil.toJson(userCard);

        //历史记录字段表建立
        PushHistory history = new PushHistory();
        //被加入群类型
        history.setEntityType(PushModel.ENTITY_TYPE_ADD_FRIEND);
        history.setEntity(entity);
        history.setReceiver(receiver);
        //接收者当前的设备推送Id
        history.setReceiverPushId(receiver.getPushId());
        //保存到历史记录表
        Hib.queryOnly(session -> session.save(history));

        //推送
        PushDispatcher dispatcher = new PushDispatcher();
        PushModel pushModel = new PushModel()
                .add(history.getEntityType(),history.getEntity());
        //添加并提交到第三方推送
        dispatcher.add(receiver,pushModel);
        dispatcher.submit();
    }
}
