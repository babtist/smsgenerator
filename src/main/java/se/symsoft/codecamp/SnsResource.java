/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.symsoft.codecamp;

import se.symsoft.cc2016.logutil.Logged;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.net.URISyntaxException;

@Path("sns")
public class SnsResource {

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_HTML)
    @Logged
    public void receive(@Suspended final AsyncResponse asyncResponse, final String data) throws URISyntaxException {
        System.out.println(data);
        asyncResponse.resume("OK");
    }

}
