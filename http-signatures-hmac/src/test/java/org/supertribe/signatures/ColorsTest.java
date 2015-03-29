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
import org.apache.ziplock.IO;
import org.apache.ziplock.maven.Mvn;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URL;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class ColorsTest {

    @Deployment(testable = false)
    public static WebArchive war() throws Exception {
        return new Mvn.Builder()
                .name("colors.war")
                .build(WebArchive.class)
                .addClass(KeystoreInitializer.class);
    }

    @ArquillianResource
    private URL webapp;

    @Test
    public void success() throws Exception {

         final String actual = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("preferred")
                .header("Authorization", sign("GET", "/colors/api/colors/preferred"))
                .get(String.class);

        assertEquals("orange", actual);
    }

    @Test
    public void successPost() throws Exception {
        final String actual = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("preferred")
                .header("Authorization", sign("POST", "/colors/api/colors/preferred"))
                .post("Hello", String.class);

        assertEquals("Hello", actual);
    }

    @Test
    public void successPut() throws Exception {
        final String actual = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("preferred")
                .header("Authorization", sign("PUT", "/colors/api/colors/preferred"))
                .put("World", String.class);

        assertEquals("World", actual);
    }

    @Test
    public void fail() throws Exception {
        final Response response = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("refused")
                .header("Authorization", sign("GET", "/colors/api/colors/refused"))
                .get();
        assertEquals(403, response.getStatus());
    }

    @Test
    public void authorized() throws Exception {
        final Response response = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("authorized")
                .header("Authorization", sign("GET", "/colors/api/colors/authorized"))
                .get();
        assertEquals("you rock guys", IO.slurp(InputStream.class.cast(response.getEntity())));
    }

    private Signature sign(final String method, final String uri) throws Exception {
        final Signature signature = new Signature(KeystoreInitializer.KEY_ALIAS, "hmac-sha256", null, "(request-target)");

        final Key key = new SecretKeySpec(KeystoreInitializer.SECRET.getBytes(), KeystoreInitializer.ALGO);
        final Signer signer = new Signer(key, signature);
        final Map<String, String> headers = new HashMap<>();
        return signer.sign(method, uri, headers);
    }
}
