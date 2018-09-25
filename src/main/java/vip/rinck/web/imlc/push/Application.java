package vip.rinck.web.imlc.push;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import vip.rinck.web.imlc.push.provider.AuthRequestFilter;
import vip.rinck.web.imlc.push.provider.GsonProvider;
import vip.rinck.web.imlc.push.service.AccountService;

import java.util.logging.Logger;

/**
 * @author RINCK
 */
public class Application extends ResourceConfig {
    public Application(){
        //注册逻辑处理的包名
        packages(AccountService.class.getPackage().getName());

        //注册全局请求拦截器
        register(AuthRequestFilter.class);

        //注册Json解析器
        //register(JacksonJsonProvider.class);
        //替换解析器为Gson
        register(GsonProvider.class);

        //注册日志打印输出
        register(Logger.class);
    }
}
