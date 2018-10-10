package vip.rinck.web.imlc.push.service;

import com.google.common.base.Strings;
import vip.rinck.web.imlc.push.bean.api.base.ResponseModel;
import vip.rinck.web.imlc.push.bean.api.group.GroupCreateModel;
import vip.rinck.web.imlc.push.bean.api.group.GroupMemberAddModel;
import vip.rinck.web.imlc.push.bean.api.group.GroupMemberUpdateModel;
import vip.rinck.web.imlc.push.bean.card.ApplyCard;
import vip.rinck.web.imlc.push.bean.card.GroupCard;
import vip.rinck.web.imlc.push.bean.card.GroupMemberCard;
import vip.rinck.web.imlc.push.bean.db.Group;
import vip.rinck.web.imlc.push.bean.db.GroupMember;
import vip.rinck.web.imlc.push.bean.db.User;
import vip.rinck.web.imlc.push.factory.GroupFactory;
import vip.rinck.web.imlc.push.factory.PushFactory;
import vip.rinck.web.imlc.push.factory.UserFactory;
import vip.rinck.web.imlc.push.provider.LocalDateTimeConverter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 群组的接口的入口
 */
@Path("/group")
public class GroupService extends BaseService {
    /**
     * 创建群
     *
     * @param model 基本参数
     * @return 群信息
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupCard> create(GroupCreateModel model) {
        if (!GroupCreateModel.check(model))
            return ResponseModel.buildParameterError();
        //创建者
        User creator = getSelf();
        //创建者移除列表中
        model.getUsers().remove(creator.getId());
        if (model.getUsers().size() == 0)
            return ResponseModel.buildParameterError();

        //检查是否已有
        if (GroupFactory.findByName(model.getName()) != null)
            return ResponseModel.buildHaveNameError();

        List<User> users = new ArrayList<>();
        for (String s : model.getUsers()) {
            User user = UserFactory.findById(s);
            if (user == null)
                continue;
            users.add(user);
        }
        //没有一个成员
        if (users.size() == 0)
            return ResponseModel.buildParameterError();

        Group group = GroupFactory.create(creator, model, users);
        if (group == null)
            //服务器异常
            return ResponseModel.buildServiceError();

        //获取群管理员信息(创建者自己的信息)
        GroupMember creatorMember = GroupFactory.getMember(creator.getId(), group.getId());
        if (creatorMember == null)
            //服务器异常
            return ResponseModel.buildServiceError();

        //获取群成员，并推送被添加的信息
        Set<GroupMember> members = GroupFactory.getMembers(group);
        if (members == null)
            //服务器异常
            return ResponseModel.buildServiceError();
        members = members.stream()
                .filter(groupMember -> groupMember.getId().equalsIgnoreCase(creatorMember.getId()))
                .collect(Collectors.toSet());
        //开始发起推送
        PushFactory.pushJoinGroup(members);
        return ResponseModel.buildOk(new GroupCard(creatorMember));
    }

    /**
     * 查找群，没有传递参数就是搜索最近所有的群
     *
     * @param name 搜索的参数
     * @return 群信息列表
     */
    @GET
    @Path("/search/{name:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupCard>> search(@PathParam("name") @DefaultValue("") String name) {
        User self = getSelf();
        List<Group> groups = GroupFactory.search(name);
        if (groups != null && groups.size() > 0) {
            List<GroupCard> groupCards = groups.stream()
                    .map(group -> {
                        GroupMember member = GroupFactory.getMember(self.getId(), group.getId());
                        return new GroupCard(group, member);
                    }).collect(Collectors.toList());
            return ResponseModel.buildOk(groupCards);

        }
        return ResponseModel.buildOk();
    }

    /**
     * 拉取自己当前的群的列表
     *
     * @param dateStr 时间字段，
     * @return
     */
    @GET
    @Path("/list/{date:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupCard>> list(@DefaultValue("") @PathParam("date") String dateStr) {
        User self = getSelf();

        LocalDateTime dateTime = null;
        if (!Strings.isNullOrEmpty(dateStr)) {
            try {
                dateTime = LocalDateTime.parse(dateStr, LocalDateTimeConverter.FORMATTER);
            } catch (Exception e) {
                dateTime = null;
            }
        }

        Set<GroupMember> members = GroupFactory.getMembers(self);
        if(members==null||members.size()==0)
            return ResponseModel.buildOk();

        final LocalDateTime finalDateTime = dateTime;
        List<GroupCard> groupCards = members.stream()
                .filter(groupMember -> finalDateTime==null//时间为null 不做限制
                        ||groupMember.getUpdateAt().isAfter(finalDateTime))//时间不为null 需要在该时间之后
                .map(GroupCard::new)//转换操作
                .collect(Collectors.toList());
        return ResponseModel.buildOk(groupCards);
    }


    /**
     * 获取一个群的信息
     * 必需是群成员
     * @param id 群的Id
     * @return 群信息
     */
    @GET
    @Path("/{groupId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupCard> getGroup(@PathParam("groupId") String id) {
        if(Strings.isNullOrEmpty(id))
            return ResponseModel.buildParameterError();

        User self = getSelf();
        GroupMember member = GroupFactory.getMember(self.getId(),id);
        if(member==null)
            return ResponseModel.buildNotFoundGroupError(null);
        return ResponseModel.buildOk(new GroupCard(member));
    }

    /**
     * 拉取一个群的所有成员,必需是成员之一
     *
     * @param groupId 群Id
     * @return 成员列表
     */
    @GET
    @Path("/{groupId}/members")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupMemberCard>> members(@PathParam("groupId") String groupId) {
        User self = getSelf();

        Group group = GroupFactory.findById(groupId);
        //没有这个群
        if(group==null)
            return ResponseModel.buildNotFoundGroupError(null);

        //检查权限
        GroupMember selfMember = GroupFactory.getMember(self.getId(),groupId);
        if(selfMember==null)
            return ResponseModel.buildNoPermissionError();

        //所有的成员
        Set<GroupMember> members = GroupFactory.getMembers(group);
        if(members==null)
            return ResponseModel.buildServiceError();

        List<GroupMemberCard> memberCards = members
                .stream()
                .map(GroupMemberCard::new)
                .collect(Collectors.toList());

        //返回
        return ResponseModel.buildOk(memberCards);
    }

    /**
     * 给群添加成员的接口
     *
     * @param groupId 群Id，必需是群的管理组
     * @param model   添加成员的model
     * @return 添加成员的列表
     */
    @POST
    @Path("/{groupId}/{date:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupMemberCard>> memberAdd(@PathParam("groupId") String groupId, GroupMemberAddModel model) {
        if(Strings.isNullOrEmpty(groupId)||!GroupMemberAddModel.check(model))
            return ResponseModel.buildParameterError();
        //获取自己的信息
        User self = getSelf();

        //移除自己再判断数量
        model.getUsers().remove(self.getId());
        if(model.getUsers().size()==0)
            return ResponseModel.buildParameterError();

        //没有这个群
        Group group = GroupFactory.findById(groupId);
        if(group==null)
            return ResponseModel.buildNotFoundGroupError(null);

        //必需是成员，同时是管理组及其以上
        GroupMember selfMember = GroupFactory.getMember(self.getId(),groupId);
        if(selfMember==null||selfMember.getPermissionType()==GroupMember.NOTIFY_LEVEL_NONE)
            return ResponseModel.buildNoPermissionError();

        //已有的成员
        Set<GroupMember> oldMembers = GroupFactory.getMembers(group);
        Set<String> oldMemberUserIds = oldMembers.stream()
                .map(GroupMember::getUserId)
                .collect(Collectors.toSet());
        List<User> insertUsers = new ArrayList<>();
        for (String s : model.getUsers()) {
            User user = UserFactory.findById(s);
            if(user==null)
                continue;
            if(oldMemberUserIds.contains(user.getId()))
                continue;
            insertUsers.add(user);
        }

        //没有一个新增的成员
        if(insertUsers.size()==0){
            return ResponseModel.buildParameterError();
        }

        //进行添加操作
        Set<GroupMember> insertMembers = GroupFactory.addMembers(group,insertUsers);
        if(insertMembers==null)
            return ResponseModel.buildServiceError();

        //转换
        List<GroupMemberCard> insertCards = insertMembers.stream()
                .map(GroupMemberCard::new)
                .collect(Collectors.toList());
        //通知
        //1.通知新增的成员 你被加入了群
        PushFactory.pushJoinGroup(insertMembers);

        //2.通知老成员 某人被加入了群
        PushFactory.pushGroupMemberAdd(oldMembers,insertMembers);
        return ResponseModel.buildOk(insertCards);
    }

    /**
     * 更改成员信息 请求的人为管理员或用户自己
     *
     * @param memberId 成员Id，可以查询对应的群和人
     * @param model    修改的Model
     * @return 当前成员的信息
     */
    @PUT
    @Path("/member/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupMemberCard> modifyMember(@PathParam("memberId") String memberId, GroupMemberUpdateModel model) {
        return null;
    }

    /**
     * 申请加入一个群
     * 此时会创建一个加入的申请，并写入表，会给管理员发送消息
     * 同意，则调用添加成员接口
     *
     * @param groupId 群Id
     * @return 申请的信息
     */
    @POST
    @Path("/applyJoin/{groupId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<ApplyCard> join(@PathParam("groupId") String groupId) {
        return null;
    }
}
