package vip.rinck.web.imlc.push.service;

import com.google.common.base.Strings;
import vip.rinck.web.imlc.push.bean.api.account.AccountRspModel;
import vip.rinck.web.imlc.push.bean.api.account.LoginModel;
import vip.rinck.web.imlc.push.bean.api.account.RegisterModel;
import vip.rinck.web.imlc.push.bean.api.base.ResponseModel;
import vip.rinck.web.imlc.push.bean.card.UserCard;
import vip.rinck.web.imlc.push.bean.db.User;
import vip.rinck.web.imlc.push.factory.UserFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.awt.*;

/**
 * @author RINCK
 */
@Path("/account")
public class AccountService extends BaseService {

    //登录
    @POST
    @Path("/login")
    //指定请求与返回的响应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> login(LoginModel model) {
        if (!LoginModel.check(model)) {
            return ResponseModel.buildParameterError();
        }

        User user = UserFactory.login(model.getAccount(), model.getPassword());
        if (user != null) {
            //如果有携带pushId
            if (!Strings.isNullOrEmpty(model.getPushId())) {
                return bind(user, model.getPushId());
            }
            //返回当前账户
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        } else {
            return ResponseModel.buildLoginError();
        }
    }

    //注册
    @POST
    @Path("/register")
    //指定请求与返回的响应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> register(RegisterModel model) {
        if (!RegisterModel.check(model)) {
            //返回参数异常
            return ResponseModel.buildParameterError();
        }
        User user = UserFactory.findByPhone(model.getAccount().trim());
        if (user != null) {
            //手机号已存在
            return ResponseModel.buildHaveAccountError();
        }
        user = UserFactory.findByUsername(model.getUsername().trim());
        if (user != null) {
            //用户名已存在
            return ResponseModel.buildHaveNameError();
        }
        //开始注册逻辑
        user = UserFactory.register(model.getAccount(), model.getPassword(), model.getUsername());
        if (user != null) {
            //如果携带有pushId
            if (!Strings.isNullOrEmpty(model.getPushId())) {
                return bind(user, model.getPushId());
            }
            //返回当前账户
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        } else {
            //注册异常
            return ResponseModel.buildRegisterError();
        }

    }

    //绑定
    @POST
    @Path("/bind/{pushId}")
    //指定请求与返回的响应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    //从请求头中获取token字段
    //pushId从url中获取
    public ResponseModel<AccountRspModel> bind(@HeaderParam("token") String token,
                                               @PathParam("pushId") String pushId) {
        if (Strings.isNullOrEmpty(token) || Strings.isNullOrEmpty(pushId)) {
            return ResponseModel.buildParameterError();
        }
        //拿到自己的信息
        //User user = UserFactory.findByToken(token);
        User self = getSelf();
        return bind(self, pushId);

    }

    /**
     * 绑定的操作
     *
     * @param self
     * @param pushId
     * @return
     */
    private ResponseModel<AccountRspModel> bind(User self, String pushId) {
        User user = UserFactory.bindPushId(self, pushId);
        if (user == null) {
            //绑定失败 服务器异常
            return ResponseModel.buildServiceError();
        }
        //返回当前账户,并且已经绑定了
        AccountRspModel rspModel = new AccountRspModel(user, true);
        return ResponseModel.buildOk(rspModel);
    }

}
