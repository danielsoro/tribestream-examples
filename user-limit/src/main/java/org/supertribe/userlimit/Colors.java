/*
 * Tomitribe Confidential
 *
 * Copyright(c) Tomitribe Corporation. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package org.supertribe.userlimit;

import com.tomitribe.tribestream.governance.api.Concurrent;
import com.tomitribe.tribestream.governance.api.GovernanceUnit;
import com.tomitribe.tribestream.governance.api.Rate;
import com.tomitribe.tribestream.governance.api.UserLimit;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("colors")
@Singleton
@Lock(LockType.READ)
@RolesAllowed("tribe")
public class Colors {

    /**
     * A simple GET resource with an user-specific rate limit. This allows each user 5 calls within a 10 second period.
     * Any subsequent calls within the 10 second window will fail with a HTTP 429 (rate exceeded) status.
     *
     * @return the static hardcoded string "orange".
     */
    @GET
    @Path("preferred")
    @UserLimit(rate = @Rate(window = 10, unit = GovernanceUnit.SECONDS, limit = 5))
    public String preferred() {
        return "orange";
    }

    /**
     * A simple POST resource with an user-specific concurrency limit. This allows 2 simultaneous calls for each user.
     * Any more concurrent calls will return with a HTTP 423 (resource locked) status code.
     * <p/>
     * This method has had a sleep() call deliberately added to it to slow it down, this ensuring the multithreaded test
     * invokes this method 8 times concurrently.
     * <p/>
     * This simple method returns the same string as it is given, effectively echoing the payload to the response.
     *
     * @param payload The payload POSTed, as a String
     * @return the payload from the POST, echoed back out
     */
    @POST
    @Path("preferred")
    @UserLimit(concurrent = @Concurrent(limit = 2))
    public String preferredPost(final String payload) {

        // simulate a method that takes slightly longer to execute so we can be sure the test calls the method
        // with 8 threads concurrently
        try {
            Thread.sleep(500);
        } catch (final InterruptedException e) {
            // interupted, ignore
        }

        return payload;
    }

}
