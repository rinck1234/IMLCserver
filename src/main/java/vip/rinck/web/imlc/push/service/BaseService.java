package vip.rinck.web.imlc.push.service;

import vip.rinck.web.imlc.push.bean.db.User;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

public class BaseService {
    //添加一个上下文注解，该注解会给securityContext赋值
    //具体的值为拦截器返回的SecurityContext
    @Context
    protected SecurityContext securityContext;

    /**
     * 从上下文直接获取自己的信息
     * @return
     */
    protected User getSelf(){
        return (User) securityContext.getUserPrincipal();
    }
}
