package vip.rinck.web.imlc.push.factory;

import com.google.common.base.Strings;
import vip.rinck.web.imlc.push.bean.api.group.GroupCreateModel;
import vip.rinck.web.imlc.push.bean.db.Group;
import vip.rinck.web.imlc.push.bean.db.GroupMember;
import vip.rinck.web.imlc.push.bean.db.User;
import vip.rinck.web.imlc.push.utils.Hib;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 群数据库处理
 */
public class GroupFactory {
    //通过Id拿到群Model
    public static Group findById(String groupId) {
        return Hib.query(session -> session.get(Group.class, groupId));
    }

    public static Group findById(User user, String groupId) {
        GroupMember member = getMember(user.getId(), groupId);
        if (member != null) {
            return member.getGroup();
        }
        return null;

    }

    //通过名字查找群
    public static Group findByName(String name) {
        return Hib.query(session -> (Group) session.createQuery("from Group where lower(groupname)=:name ")
                .setParameter("name", name.toLowerCase())
                .uniqueResult());
    }


    //获取群所有成员
    public static Set<GroupMember> getMembers(Group group) {
        return Hib.query(session -> {
            @SuppressWarnings("unchecked")
            List<GroupMember> members = session.createQuery("from GroupMember where group=:group")
                    .setParameter("group", group)
                    .list();

            return new HashSet<>(members);
        });
    }

    //获取一个人加入的所有群
    public static Set<GroupMember> getMembers(User user) {
        return Hib.query(session -> {
            @SuppressWarnings("unchecked")
            List<GroupMember> members = session.createQuery("from GroupMember where userId=:userId")
                    .setParameter("userId", user.getId())
                    .list();

            return new HashSet<>(members);
        });
    }

    //创建群
    public static Group create(User creator, GroupCreateModel model, List<User> users) {
        return Hib.query(session -> {
            Group group = new Group(creator, model);
            session.save(group);

            GroupMember ownerMember = new GroupMember(creator, group);
            //设置群主
            ownerMember.setPermissionType(GroupMember.PERMISSION_TYPE_ADMIN_SU);
            //保存，并没有提交数据库
            session.save(ownerMember);

            for (User user : users) {
                GroupMember member = new GroupMember(user, group);
                session.save(member);
            }

            //session.flush();
            //session.load(group,group.getId());
            return group;


        });

    }

    //获取一个群成员
    public static GroupMember getMember(String userId, String groupId) {
        return Hib.query(session -> (GroupMember) session.createQuery("from GroupMember where userId=:userId and groupId=:groupId")
                .setParameter("userId", userId)
                .setParameter("groupId", groupId)
                .setMaxResults(1)
                .uniqueResult()
        );
    }

    //查询
    @SuppressWarnings("unchecked")
    public static List<Group> search(String name) {
        if(Strings.isNullOrEmpty(name))
            name = "";//保证不为null的情况，减少后面判断和额外的错误
        final String searchName = "%"+name+"%";

        return Hib.query(session -> {
            //查询条件; username忽略大小写，并且使用模糊查询
            //用户信息必需完善
            return (List<Group>)session.createQuery("from Group where lower(groupname) like :name")
                    .setParameter("name",searchName)
                    .setMaxResults(20)//至多20条
                    .list();


        });
    }

    //给群添加用户
    public static Set<GroupMember> addMembers(Group group, List<User> insertUsers) {
        return Hib.query(session -> {

            Set<GroupMember> members = new HashSet<>();

            for (User user : insertUsers) {
                GroupMember member = new GroupMember(user,group);
                session.save(member);
                members.add(member);
            }
            //进行数据刷新
            /*
            for (GroupMember member : members) {
                //进行刷新 会进行关联查询 循环中消耗较高
                session.refresh(member);
            }
            */
            return members;
        });
    }
}
