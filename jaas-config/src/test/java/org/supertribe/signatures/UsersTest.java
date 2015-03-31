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

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.ziplock.maven.Mvn;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class UsersTest {

    /**
     * Build the web archive to test. This adds in the KeystoreInitializer class from the test sources,
     * which would otherwise be excluded.
     * @return The archive to deploy for the test
     * @throws Exception
     */
    @Deployment(testable = false)
    public static WebArchive war() throws Exception {
        return new Mvn.Builder()
                .name("users.war")
                .build(WebArchive.class);
    }

    /**
     * Arquillian will boot an instance of Tribestream with a random port. The URL with the random port is injected
     * into this field.
     */
    @ArquillianResource
    private URL webapp;

    /**
     * Test logging in and calling the service as an LDAP user
     * @throws Exception when an error occurs or the test fails
     */
    @Test
    public void successJBloggs() throws Exception {
         final String actual = WebClient.create(webapp.toExternalForm(), "jbloggs", "test", null)
                .path("api/users")
                .path("whoami")
                .get(String.class);

        assertEquals("jbloggs", actual);
    }

    /**
     * Test logging in with an LDAP user with the wrong password. This should return a 401.
     * @throws Exception when an error occurs or the test fails
     */
    @Test
    public void failJBloggs() throws Exception {
         final Response response = WebClient.create(webapp.toExternalForm(), "jbloggs", "badpassword", null)
                .path("api/users")
                .path("whoami")
                .get();

        assertEquals(401, response.getStatus());
    }

    /**
     * Test logging in and calling the service as a Tomcat user
     * @throws Exception when an error occurs or the test fails
     */
    @Test
    public void successSupport() throws Exception {
         final String actual = WebClient.create(webapp.toExternalForm(), "support", "support", null)
                .path("api/users")
                .path("whoami")
                .get(String.class);

        assertEquals("support", actual);
    }

    /**
     * Test logging in with a Tomcat user with the wrong password. This should return a 401.
     * @throws Exception when an error occurs or the test fails
     */
    @Test
    public void failSupport() throws Exception {
         final Response response = WebClient.create(webapp.toExternalForm(), "support", "badpassword", null)
                .path("api/users")
                .path("whoami")
                .get();

        assertEquals(401, response.getStatus());
    }
}
