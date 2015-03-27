/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.supertribe.userlimit;

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
     * @param username           The username to use for the call
     * @param password           The password to use for the call
     * @param payload            The payload to send in the POST to the endpoint
     * @param expectedStatusCode The status code that the endpoint is expected to return. Typically this will be 200 (OK)
     *                           for calls within the limit, and 429 (limit exceeded) once the limit has been exceeded.
     * @param expectedBody       The expected response body. This can be null. If null is specified, the response body is not
     *                           checked.
     * @return true if the call succeeds, false otherwise
     * @throws Exception when an error occurs
     */
    private boolean call(final String username, final String password, final String payload, final int expectedStatusCode, final String expectedBody) throws Exception {

        final Response response = WebClient.create(webapp.toExternalForm(), username, password, null)
                .path("api/colors")
                .path("preferred")
                .post(payload);

        if (expectedStatusCode != response.getStatus()) {
            return false;
        }

        if (expectedBody != null) {
            try (final InputStream is = InputStream.class.cast(response.getEntity())) {
                final String body = IO.slurp(is);
                if (!expectedBody.equals(body)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * This test checks the concurrency limit on the endpoint by calling it 4 times simultaneously for each user.
     * 2 calls for each user should succeed, and 2 calls should fail. Once these calls are complete, the endpoint
     * should be immediately available again
     *
     * @throws Exception when an error occurs or the test fails.
     */
    @Test
    public void testConcurrentLimit() throws Exception {

        final AtomicInteger user1CallsSucceeded = new AtomicInteger(0);
        final AtomicInteger user1CallsMade = new AtomicInteger(0);
        final AtomicInteger user2CallsSucceeded = new AtomicInteger(0);
        final AtomicInteger user2CallsMade = new AtomicInteger(0);

        Runnable user1Runnable = getRunnable("user1", "user1", user1CallsSucceeded, user1CallsMade);
        Runnable user2Runnable = getRunnable("user2", "user2", user2CallsSucceeded, user2CallsMade);

        final ExecutorService threadPool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 4; i++) {
            threadPool.submit(user1Runnable);
        }
        for (int i = 0; i < 4; i++) {
            threadPool.submit(user2Runnable);
        }
        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        // two out of the four calls should have succeeded
        Assert.assertEquals(2, user1CallsSucceeded.intValue());
        Assert.assertEquals(4, user1CallsMade.intValue());

        // two out of the four calls should have succeeded
        Assert.assertEquals(2, user2CallsSucceeded.intValue());
        Assert.assertEquals(4, user2CallsMade.intValue());

        // the resource should be available again
        Assert.assertTrue(call("user1", "user1", "hello", 200, "hello"));
        Assert.assertTrue(call("user2", "user2", "hello", 200, "hello"));
    }

    /**
     * Convenience method to create a task to call the endpoint
     * @param username Username to use to call the endpoint
     * @param password Password to use to call the endpoint
     * @param callsSucceeded counter for successful calls
     * @param callsMade counter for all calls
     * @return a runnable that can be executed by the threadpool
     */
    private Runnable getRunnable(final String username, final String password, final AtomicInteger callsSucceeded, final AtomicInteger callsMade) {
        return new Runnable() {
                @Override
                public void run() {
                    try {
                        if (call(username, password, "hello", 200, "hello")) {
                            callsSucceeded.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // call failed - don't increment the counter
                    }

                    callsMade.incrementAndGet();
                }
            };
    }
}
