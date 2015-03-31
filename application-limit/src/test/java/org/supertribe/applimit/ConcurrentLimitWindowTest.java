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

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.ziplock.maven.Mvn;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.util.IO;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(Arquillian.class)
public class ConcurrentLimitWindowTest {

    /**
     * Builds the .war file for Arquillian to deploy to Tribestream to test
     *
     * @return A .war file build in the same way as if a Maven build had been run on the project.
     * @throws Exception if an error occurs building or deploying the .war archive
     */
    @Deployment(testable = false)
    public static WebArchive war() throws Exception {
        return new Mvn.Builder()
                .name("colors.war")
                .build(WebArchive.class);
    }

    /**
     * Arquillian will boot an instance of Tribestream with a random port. The URL with the random port is injected
     * into this field.
     */
    @ArquillianResource
    private URL webapp;

    /**
     * Calls the /api/colors/preferred endpoint, and checks the HTTP status code and (optionally) response body match the
     * expected response code and string.
     *
     * @param payload            The payload to send in the POST to the endpoint
     * @param expectedStatusCode The status code that the endpoint is expected to return. Typically this will be 200 (OK)
     *                           for calls within the limit, and 429 (limit exceeded) once the limit has been exceeded.
     * @param expectedBody       The expected response body. This can be null. If null is specified, the response body is not
     *                           checked.
     * @return true if the call succeeds, false otherwise
     * @throws Exception when an error occurs
     */
    private boolean call(final String payload, final int expectedStatusCode, final String expectedBody) throws Exception {

        final Response response = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("preferred")
                .post(payload);

        if (expectedStatusCode != response.getStatus()) {
            return false;
        }

        if (expectedBody != null) {
            try (final InputStream is = InputStream.class.cast(response.getEntity())) {
                final String body = IO.slurp(is);
                if (! expectedBody.equals(body)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * This test checks the concurrency limit on the endpoint by calling it 4 times simultaneously. 2 calls should
     * succeed, and 2 calls should fail. Once these calls are complete, the endpoint should be immediately available
     * again
     *
     * @throws Exception when an error occurs or the test fails.
     */
    @Test
    public void testConcurrentLimit() throws Exception {

        final AtomicInteger callsSucceeded = new AtomicInteger(0);
        final AtomicInteger callsMade = new AtomicInteger(0);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (call("hello", 200, "hello")) {
                        callsSucceeded.incrementAndGet();
                    }
                } catch (Exception e) {
                    // call failed - don't increment the counter
                }

                callsMade.incrementAndGet();
            }
        };

        final ExecutorService threadPool = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            threadPool.submit(runnable);
        }
        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        // two out of the four calls should have succeeded
        Assert.assertEquals(2, callsSucceeded.intValue());
        Assert.assertEquals(4, callsMade.intValue());

        // the resource should be available again
        Assert.assertTrue(call("hello", 200, "hello"));
    }
}
