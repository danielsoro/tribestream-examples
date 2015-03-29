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

import com.tomitribe.tribestream.security.signatures.Signature;
import com.tomitribe.tribestream.security.signatures.Signer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.ziplock.maven.Mvn;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class UsersTest {

    @Deployment(testable = false)
    public static WebArchive war() throws Exception {
        return new Mvn.Builder()
                .name("users.war")
                .build(WebArchive.class)
                .addClass(KeystoreInitializer.class);
    }

    @ArquillianResource
    private URL webapp;

    @Test
    public void success() throws Exception {

         final String actual = WebClient.create(webapp.toExternalForm())
                .path("api/users")
                .path("whoami")
                .header("Authorization", sign("GET", "/users/api/users/whoami"))
                .get(String.class);

        assertEquals("user", actual);
    }

    private Signature sign(final String method, final String uri) throws Exception {
        final Signature signature = new Signature(KeystoreInitializer.KEY_ALIAS, "hmac-sha256", null, "(request-target)");

        final Key key = new SecretKeySpec(KeystoreInitializer.SECRET.getBytes(), KeystoreInitializer.ALGO);
        final Signer signer = new Signer(key, signature);
        final Map<String, String> headers = new HashMap<>();
        return signer.sign(method, uri, headers);
    }
}
