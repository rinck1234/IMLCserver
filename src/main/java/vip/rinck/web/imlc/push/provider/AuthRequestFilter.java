package vip.rinck.web.imlc.push.provider;


import com.google.common.base.Strings;
import org.glassfish.jersey.server.ContainerRequest;
import vip.rinck.web.imlc.push.bean.api.base.ResponseModel;
import vip.rinck.web.imlc.push.bean.db.User;
import vip.rinck.web.imlc.push.factory.UserFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;

/**
 * 用于所有的请求接口过滤和拦截
 */
@Provider
public class AuthRequestFilter implements ContainerRequestFilter {



    //继承实现接口的过滤方法
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        //检测是否是登录注册接口
        String relationPath = ((ContainerRequest)requestContext).getPath(false);
        if(relationPath.startsWith("account/login")||relationPath.startsWith("account/register")){
            return;
        }

        //从Headers中去找到第一个token节点
        String token = requestContext.getHeaders().getFirst("token");
        if(!Strings.isNullOrEmpty(token)){
            //查询自己的信息
            final User self = UserFactory.findByToken(token);
            if(self!=null){
                //给当前请求添加一个上下文
                requestContext.setSecurityContext(new SecurityContext() {
                    //主体部分
                    @Override
                    public Principal getUserPrincipal() {
                        //User实现Principal接口
                        return self;
                    }

                    @Override
                    public boolean isUserInRole(String role) {
                        //可以在这里写入用户的权限，role是权限名
                        return true;
                    }

                    @Override
                    public boolean isSecure() {
                        //检查HTTPS 默认false
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return null;
                    }
                });
                //写入上下文就返回
                return;
            }
        }

        //直接返回一个账户需要登录的model
        ResponseModel model = ResponseModel.buildAccountError();
        //构建一个返回
        Response response = Response.status(Response.Status.OK)
                .entity(model)
                .build();
        //停止一个请求的继续下发，直接返回请求
        requestContext.abortWith(response);
    }
}
