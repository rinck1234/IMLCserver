package vip.rinck.web.imlc.push.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author RINCK
 */
@Path("/account")
public class AccountService {
    @GET
    @Path("/login")
    public String get(){
        return "You get the login.";
    }
}
