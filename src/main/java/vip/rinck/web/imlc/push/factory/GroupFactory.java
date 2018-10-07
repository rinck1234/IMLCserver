package vip.rinck.web.imlc.push.factory;

import vip.rinck.web.imlc.push.bean.db.Group;
import vip.rinck.web.imlc.push.bean.db.GroupMember;
import vip.rinck.web.imlc.push.bean.db.User;

import java.util.Set;

/**
 * 群数据库处理
 */
public class GroupFactory {
    public static Group findById(String groupId) {
        //TODO 查询一个群
        return null;
    }

    public static Set<GroupMember> getMembers(Group group) {
        //TODO 查询一个群的成员
        return null;
    }

    public static Group findById(User sender, String receiverId) {
        //TODO 查询一个群 该用户必须是群成员
        return null;
    }
}
