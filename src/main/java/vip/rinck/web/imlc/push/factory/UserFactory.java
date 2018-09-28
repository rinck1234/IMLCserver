package vip.rinck.web.imlc.push.factory;

import com.google.common.base.Strings;
import org.hibernate.Session;
import vip.rinck.web.imlc.push.bean.db.User;
import vip.rinck.web.imlc.push.bean.db.UserFollow;
import vip.rinck.web.imlc.push.utils.Hib;
import vip.rinck.web.imlc.push.utils.TextUtil;

import javax.xml.soap.Text;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserFactory {

    //通过Token查询用户信息
    //只能自己使用，查询本人信息
    public static User findByToken(String token){
        return Hib.query(session -> (User)session
                .createQuery("from User where token=:token")
                .setParameter("token",token)
                .uniqueResult());
    }

    //通过phone找到user
    public static User findByPhone(String phone){
        return Hib.query(session -> (User)session
                .createQuery("from User where phone=:inphone")
                .setParameter("inphone",phone)
                .uniqueResult());
    }

    //通过username找到user
    public static User findByUsername(String username){
        return Hib.query(session -> (User)session
                .createQuery("from User where phone=:inusername")
                .setParameter("inusername",username)
                .uniqueResult());
    }

    //通过Id找到User
    public static User findById(String id){
        //通过Id查询
        return Hib.query(session -> session.get(User.class,id));
    }

    /**
     * 更新用户信息到数据库
     * @param user
     * @return
     */
    public static User update(User user){
        return Hib.query(session -> {
            session.saveOrUpdate(user);
            return user;
        });
    }

    /**
     * 给当前的账户绑定PushId
     * @param user 自己的User
     * @param pushId
     * @return
     */
    public static User bindPushId(User user,String pushId){
        if(Strings.isNullOrEmpty(pushId)){
            return null;
        }
        //查询是否有其他账户绑定这个设备
        //取消绑定，避免推送混乱
        //查询的列表不能包括自己
        Hib.queryOnly(session -> {
            @SuppressWarnings("unchecked")
            List<User> userList = (List<User>) session
                    .createQuery("from User where lower(pushId)=:pushId and id!=:userId")
                    .setParameter("pushId", pushId.toLowerCase())
                    .setParameter("userId", user.getId())
                    .list();

            for (User u : userList) {
                // 更新为null
                u.setPushId(null);
                session.saveOrUpdate(u);
            }
        });

        if(pushId.equalsIgnoreCase(user.getPushId())){
            //如果当前需要绑定的设备ID，已经绑定过
            //不需要绑定
            return user;
        }else{
            //如果存在
            if(Strings.isNullOrEmpty(user.getPushId())){
                //TODO 推送一条退出消息
            }

            //更新新的设备Id
            user.setPushId(pushId);
            return update(user);
        }
    }

    /**
     * 使用账户和密码登录
     * @param account
     * @param password
     * @return
     */
    public static User login(String account,String password){
        String accountStr = account.trim();
        String encodePassword = encodePassword(password);
        User user = Hib.query(session -> (User) session
                 .createQuery("from User where phone=:phone and password=:password")
                 .setParameter("phone",accountStr)
                 .setParameter("password",encodePassword)
                 .uniqueResult());
        if(user!=null){
            //对User进行登录操作，更新Token
            user = login(user);
        }
        return user;
    }

    /**
     * 用户注册
     * 注册操作需写入数据库，并返回数据库中的User信息
     * @param account
     * @param password
     * @param username
     * @return
     */
    public static User register(String account,String password,String username){
        //去除首尾空格
        account = account.trim();
        //加密密码
        password = encodePassword(password);
        User user = createUser(account,password,username);
        if(user!=null){
            user = login(user);
        }
        return user;
    }

    /**
     * 注册部分新建用户逻辑
     * @param account 手机号
     * @param password 加密后的密码
     * @param username 用户名
     * @return 返回一个用户
     */
    private static User createUser(String account,String password,String username){
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(account);

        //数据库存储
       return Hib.query(session -> {
           session.save(user);
           return user;
       });

    }

    /**
     * 把一个User进行登录操作
     * 本质上是对Token操作
     * @param user
     * @return
     */
    private static User login(User user){
        //使用一个随机的UUID值充当Token
        String newToken = UUID.randomUUID().toString();
        //进行一次Base64格式化
        newToken = TextUtil.encodeBase64(newToken);
        user.setToken(newToken);
        return update(user);
    }

    /**
     * 对密码进行加密操作
     * @param password 原文
     * @return 密文
     */
    private static String encodePassword(String password){
        //去除首尾空格
        password = password.trim();
        //进行MD5非对称加密
        password = TextUtil.getMD5(password);
        //再进行一次Base64对称加密
        return TextUtil.encodeBase64(password);
    }

    /**
     * 获取我的联系人的列表
     * @param self User
     * @return
     */
    public static List<User> contacts(User self){
        return Hib.query(session -> {
            //重新加载一次 和当前session绑定
            session.load(self,self.getId());
            //获取我关注的人
            Set<UserFollow> follows = self.getFollowing();
            //使用简写方式
            return follows.stream()
                    .map(UserFollow::getTarget)
                    .collect(Collectors.toList());
        });
    }

    /**
     * 关注人的操作
     * @param origin 发起者
     * @param target 被关注的人
     * @param alias 备注名
     * @return 被关注人的信息
     */
    public static User follow(final User origin,final User target,final String alias){
        UserFollow follow = getUserFollow(origin,target);
        if(follow!=null){
            //已关注，直接返回
            return follow.getTarget();
        }
        return Hib.query(session -> {
            //想要操作懒加载的数据，需要重新load
            session.load(origin,origin.getId());
            session.load(target,target.getId());

            //我关注人的时候，同时他也关注我，
            //所有需要添加两条UserFollow数据
            UserFollow originFollow = new UserFollow();
            originFollow.setOrigin(origin);
            originFollow.setTarget(target);
            originFollow.setAlias(alias);

            //发起者是他，我是被关注的人
            UserFollow targetFollow = new UserFollow();
            targetFollow.setOrigin(target);
            targetFollow.setTarget(origin);

            //保存数据库
            session.save(originFollow);
            session.save(targetFollow);

            return target;

        });
    }

    /**
     * 查询两个人是否已经关注
     * @param origin 发起者
     * @param target 被关注人
     * @return 返回中间UserFollow
     */
    public static UserFollow getUserFollow(final User origin,final User target){
        return Hib.query(session -> (UserFollow)session.createQuery("from UserFollow where originId = :originId and targetId = :targetId")
                .setParameter("originId",origin.getId())
                .setParameter("targetId",target.getId())
                .setMaxResults(1)
                //唯一查询返回
                .uniqueResult());
    }

    /**
     * 搜索联系人的实现
     * @param username 查询的username，允许为空
     * @return 查询到的用户集合，如果name为空，则返回最近的用户
     */
    @SuppressWarnings("unchecked")
    public static List<User> search(String username) {
        if(Strings.isNullOrEmpty(username))
            username = "";//保证不为null的情况，减少后面判断和额外的错误
        final String searchName = "%"+username+"%";

        return Hib.query(session -> {
            //查询条件; username忽略大小写，并且使用模糊查询
            //用户信息必需完善
           return (List<User>)session.createQuery("from User where lower(username) like :username and portrait is not null and description is not null ")
                   .setParameter("username",searchName)
                   .setMaxResults(20)//至多20条
                   .list();


        });

    }
}
