/*
 * Tomitribe Confidential
 *
 * Copyright(c) Tomitribe Corporation. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package org.supertribe.signatures;

import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("users")
@Singleton
@Lock(LockType.READ)
public class Users {

    @Resource
    private SessionContext ctx;

    @GET
    @Path("whoami")
    public String whoami() {
        return ctx.getCallerPrincipal().getName();
    }

}
