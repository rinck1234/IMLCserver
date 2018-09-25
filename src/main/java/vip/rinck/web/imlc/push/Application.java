package vip.rinck.web.imlc.push;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import vip.rinck.web.imlc.push.service.AccountService;

import java.util.logging.Logger;

/**
 * @author RINCK
 */
public class Application extends ResourceConfig {
    public Application(){
        packages(AccountService.class.getPackage().getName());
        register(JacksonJsonProvider.class);
        register(Logger.class);
    }
}
