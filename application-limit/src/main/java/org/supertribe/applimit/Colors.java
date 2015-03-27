/*
 * Tomitribe Confidential
 *
 * Copyright(c) Tomitribe Corporation. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package org.supertribe.applimit;

import com.tomitribe.tribestream.governance.api.ApplicationLimit;
import com.tomitribe.tribestream.governance.api.Concurrent;
import com.tomitribe.tribestream.governance.api.GovernanceUnit;
import com.tomitribe.tribestream.governance.api.Rate;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("colors")
@Singleton
@Lock(LockType.READ)
public class Colors {

    /**
     * A simple GET resource with an application-wide rate limit. This allows 10 calls within a 10 second period.
     * The 11th and subsequent calls within the 10 second window will fail with a HTTP 429 (rate exceeded) status.
     *
     * @return the static hardcoded string "orange".
     */
    @GET
    @Path("preferred")
    @ApplicationLimit(rate = @Rate(window = 10, unit = GovernanceUnit.SECONDS, limit = 10))
    public String preferred() {
        return "orange";
    }

    /**
     * A simple POST resource with an application-wide concurrency limit. This allows 2 simultaneous calls. Any more
     * concurrent calls will return with a HTTP 423 (resource locked) status code.
     *
     * This method has had a sleep() call deliberately added to it to slow it down, this ensuring the multithreaded test
     * invokes this method 4 times concurrently.
     *
     * This simple method returns the same string as it is given, effectively echoing the payload to the response.
     * @param payload The payload POSTed, as a String
     * @return the payload from the POST, echoed back out
     */
    @POST
    @Path("preferred")
    @ApplicationLimit(concurrent = @Concurrent(limit = 2))
    public String preferredPost(final String payload) {

        // simulate a method that takes slightly longer to execute so we can be sure the test calls the method
        // with 4 threads concurrently
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // interupted, ignore
        }

        return payload;
    }

}
