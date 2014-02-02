package com.elyxor.starter;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;

import com.elyxor.common.user.MongoUserProvider;
import com.elyxor.common.document.User;

@Path("/simple")
public class SimpleServlet {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(SimpleServlet.class);

	private static MongoUserProvider _userProvider;

	static {

		initializeUserProvider();
	}

	private static void initializeUserProvider() {
		
		if (null == _userProvider) {

			try {
				log.info("Instantiating User Provider");
//				String dbAuthProviderClassName = "com.elyxor.common.db.auth.PreferencesDBAuthProvider";
//				_userProvider = new MongoUserProvider(dbAuthProviderClassName, log);
                _userProvider = new MongoUserProvider();
			} catch (Exception e) {
				log.warn("Failed to create bubble provider. Will try again on next request.", e);
			}
		}
	}
	
	public SimpleServlet() {
		initializeUserProvider();
	}
	
	/**
	 * Processes HTTP get requests
	 * that have no parameters
	 * for example /do
	 * 
	 * @return "Elyxor Simple Servlet"
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getMessage() {

		return "Elyxor Simple Servlet";
	}

	@GET
	@Path("/user/{twitterName}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getUser(@Context HttpHeaders hd, @PathParam("twitterName") String twitterName) {

		User user = _userProvider.getByTwitterName(twitterName);
        if (null != user) {
            return userToString(user);
        }
        
        return "User not found: " + twitterName;
	}

    @GET
	@Path("/users")
	@Produces(MediaType.TEXT_PLAIN)
	public String getUsers(@Context HttpHeaders hd, 
                            @DefaultValue("0") @QueryParam(value = "offset") final String offset, 
                            @DefaultValue("-1") @QueryParam(value = "limit") final String limit) {
        
		List<User> users = _userProvider.getUsers(Integer.parseInt(limit), Integer.parseInt(offset));
        String str = "";
        for (int i = 0; i < users.size(); ++i) {
            str += userToString(users.get(i)) + "\r\n";
        }
        
        return str;
	}

	@GET
	@Path("/user/add/{twitterName}/{firstName}/{lastName}")
	@Produces(MediaType.TEXT_PLAIN)
	public String addUser(@Context HttpHeaders hd, @PathParam("twitterName") String twitterName,
                                                            @PathParam("firstName") String firstName,
                                                            @PathParam("lastName") String lastName) {
        _userProvider.add(firstName, lastName, twitterName);

        return "Added: " + twitterName;
	}

    @GET
	@Path("/user/delete/{twitterName}")
	@Produces(MediaType.TEXT_PLAIN)
	public String addUser(@Context HttpHeaders hd, @PathParam("twitterName") String twitterName) {
        _userProvider.remove(twitterName);

        return "OK";
	}

    private String userToString(User user) {
        return "Twitter Name: " + user.getTwitterName() + 
                ", First Name: " + user.getFirstName() + 
                ", Last Name: " + user.getLastName();
    }
}
