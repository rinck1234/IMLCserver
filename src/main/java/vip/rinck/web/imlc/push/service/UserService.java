package vip.rinck.web.imlc.push.service;


import com.google.common.base.Strings;
import vip.rinck.web.imlc.push.bean.api.account.AccountRspModel;
import vip.rinck.web.imlc.push.bean.api.base.ResponseModel;
import vip.rinck.web.imlc.push.bean.api.user.UpdateInfoModel;
import vip.rinck.web.imlc.push.bean.card.UserCard;
import vip.rinck.web.imlc.push.bean.db.User;
import vip.rinck.web.imlc.push.factory.UserFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

/**
 * 用户信息处理
 */
@Path("/user")
public class UserService extends BaseService {


    //用户信息修改接口
    //返回自己的个人信息
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> update(UpdateInfoModel model) {
        if (!UpdateInfoModel.check(model)) {
            return ResponseModel.buildParameterError();
        }
        //拿到自己的信息
        User self = getSelf();
        //更新用户信息
        self = model.updateToUser(self);
        self = UserFactory.update(self);
        //构建自己的用户信息
        UserCard userCard = new UserCard(self, true);
        //返回
        return ResponseModel.buildOk(userCard);

    }

}
