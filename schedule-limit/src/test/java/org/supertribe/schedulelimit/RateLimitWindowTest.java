/*
 * Tomitribe Confidential
 *
 * Copyright(c) Tomitribe Corporation. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package org.supertribe.schedulelimit;

import org.apache.ziplock.maven.Mvn;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;

@RunWith(Arquillian.class)
public class RateLimitWindowTest {

    /**
     * Builds the .war file for Arquillian to deploy to Tribestream to test
     * @return A .war file build in the same way as if a Maven build had been run on the project.
     * @throws Exception if an error occurs building or deploying the .war archive
     */
    @Deployment
    public static WebArchive war() throws Exception {
        return new Mvn.Builder()
                .name("work.war")
                .build(WebArchive.class);
    }

    @EJB
    private WorkRateLimit work;

    /**
     * Calls the @Timeout method 20 times via a timer. The method allows three invocations per minute, so we
     * check the number of calls that have been successful after 10 seconds.
     * @throws Exception on error or test failure
     */
    @Test
    public void testRateLimitWindow() throws Exception {
        work.start();
        Thread.sleep(10000);
        final int successfulCalls = work.stop();
        Assert.assertEquals(3, successfulCalls);
    }

}
