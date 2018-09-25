package vip.rinck.web.imlc.push.factory;

import com.google.common.base.Strings;
import org.hibernate.Session;
import vip.rinck.web.imlc.push.bean.db.User;
import vip.rinck.web.imlc.push.utils.Hib;
import vip.rinck.web.imlc.push.utils.TextUtil;

import javax.xml.soap.Text;

import java.util.List;
import java.util.UUID;

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
       return Hib.query(session -> (User)session.save(user));
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
}
