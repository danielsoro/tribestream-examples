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

import com.tomitribe.tribestream.security.digest.encoder.MD5Encoder;
import com.tomitribe.tribestream.security.signatures.Signature;
import com.tomitribe.tribestream.security.signatures.Signer;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    private Date today = new Date(); // default window is 1 hour
    private String stringToday = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).format(today);

    @Test
    public void success() throws Exception {

         final String actual = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("preferred")
                .header("Authorization", sign("GET", "/colors/api/colors/preferred"))
                .header("Date", stringToday)
                .get(String.class);

        assertEquals("orange", actual);
    }

    @Test
    public void wrongDate() throws Exception {
        final Date oneHourAgo = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2));
        final Response actual = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("preferred")
                .header("Authorization", sign("GET", "/colors/api/colors/preferred"))
                .header("Date", new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).format(oneHourAgo))
                .get();

        assertEquals(412, actual.getStatus());
    }

    @Test
    public void successPost() throws Exception {
        final String actual = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("preferred")
                .header("Authorization", sign("POST", "/colors/api/colors/preferred"))
                .header("Date", stringToday)
                .post("Hello", String.class);

        assertEquals("Hello", actual);
    }

    @Test
    public void successPut() throws Exception {
        final String actual = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("preferred")
                .header("Authorization", sign("PUT", "/colors/api/colors/preferred"))
                .header("Date", stringToday)
                .put("World", String.class);

        assertEquals("World", actual);
    }

    @Test
    public void fail() throws Exception {
        final Response response = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("refused")
                .header("Authorization", sign("GET", "/colors/api/colors/refused"))
                .header("Date", stringToday)
                .get();
        assertEquals(403, response.getStatus());
    }

    @Test
    public void authorized() throws Exception {
        final Response response = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("authorized")
                .header("Authorization", sign("GET", "/colors/api/colors/authorized"))
                .header("Date", stringToday)
                .get();
        assertEquals("you rock guys", IO.slurp(InputStream.class.cast(response.getEntity())));
    }

    @Test
    public void preferredWantDigest() throws Exception {
        final Response response = WebClient.create(webapp.toExternalForm())
                .path("api/colors")
                .path("preferred")
                .header("Authorization", sign("GET", "/colors/api/colors/preferred"))
                .header("Date", stringToday)
                .header("Want-Digest", "SHA")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("orange", IO.slurp(InputStream.class.cast(response.getEntity())));

    }

    @Test
    public void preferred() throws IOException {
        final String content = "message";
        final HttpClient httpClient = HttpClients.createDefault();
        final HttpPost request = new HttpPost(webapp.toExternalForm() + "touch");
        request.setEntity(new StringEntity(content));

        {
            request.setHeader("Digest", "SHA=invalid,MD5=" + new String(MD5Encoder.INSTANCE.encode(content.getBytes())));
            request.setHeader("Want-Digest", "SHA;q=0.2,MD5;q=0.21");
            final HttpResponse response = httpClient.execute(request);
            assertEquals(new StringBuilder(content).reverse().toString(), EntityUtils.toString(response.getEntity()));
            assertEquals(200, response.getStatusLine().getStatusCode());
            final Header[] digestHeaders = response.getHeaders(com.tomitribe.tribestream.security.digest.Headers.DIGEST);
            assertEquals(1, digestHeaders.length);
            assertEquals(2, response.getHeaders("digest")[0].getElements().length); // sha and md5
            assertEquals(0, response.getHeaders(com.tomitribe.tribestream.security.digest.Headers.CONTENT_MD5).length);
        }
        {
            request.setHeader("Digest", "SHA=invalid,MD5=" + new String(MD5Encoder.INSTANCE.encode(content.getBytes())));
            request.setHeader("Want-Digest", "SHA;q=0.4,MD5;q=0.21,bla;q=1");
            final HttpResponse response = httpClient.execute(request);
            assertEquals(new StringBuilder(content).reverse().toString(), EntityUtils.toString(response.getEntity()));
            assertEquals(200, response.getStatusLine().getStatusCode());
            final Header[] digestHeaders = response.getHeaders(com.tomitribe.tribestream.security.digest.Headers.DIGEST);
            assertEquals(1, digestHeaders.length);
            assertEquals(2, response.getHeaders("digest")[0].getElements().length); // sha and md5
            assertEquals(0, response.getHeaders(com.tomitribe.tribestream.security.digest.Headers.CONTENT_MD5).length);
        }
    }

    @Test
    public void supplier() throws Exception {
        final WebClient webClient = WebClient.create(webapp.toExternalForm());
        WebClient.getConfig(webClient).getHttpConduit().setAuthSupplier(
                new SignatureAuthSupplier(new SecretKeySpec(KeystoreInitializer.SECRET.getBytes(), KeystoreInitializer.ALGO),
                        KeystoreInitializer.KEY_ALIAS, "hmac-sha256", "(request-target)", "date"));

        assertEquals("you rock guys", webClient
                .path("api/colors")
                .path("authorized")
                .header("Authorization", sign("GET", "/colors/api/colors/authorized")) // already supplied
                .header("Date", stringToday)
                .get(String.class));

        // not supplied
        assertEquals("you rock guys", webClient.reset()
                .path("api/colors")
                .path("authorized")
                .header("Date", stringToday)
                .get(String.class));
    }

    private Signature sign(final String method, final String uri) throws Exception {
        final Signature signature = new Signature(KeystoreInitializer.KEY_ALIAS, "hmac-sha256", null, "(request-target)", "date");

        final Key key = new SecretKeySpec(KeystoreInitializer.SECRET.getBytes(), KeystoreInitializer.ALGO);
        final Signer signer = new Signer(key, signature);
        final Map<String, String> headers = new HashMap<>();
        headers.put("Date", stringToday);
        return signer.sign(method, uri, headers);
    }

    private static class SignatureAuthSupplier implements HttpAuthSupplier {
        private final Key key;
        private final String keyAlias;
        private final String algorithm;
        private final String[] consideredHeaders;

        public SignatureAuthSupplier(final Key key, final String keyAlias, final String algorithm, final String... headers) {
            this.key = key;
            this.keyAlias = keyAlias;
            this.algorithm = algorithm;
            this.consideredHeaders = headers;
        }

        private String sign(final String method, final String uri, final Map<String, List<String>> headers) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
            final Signature signature = new Signature(keyAlias, algorithm, null, consideredHeaders);
            final Signer signer = new Signer(key, signature);
            final Map<String, String> h = new HashMap<>(headers != null ? headers.size() : 0);
            if (headers != null) {
                for (final Map.Entry<String, List<String>> e : headers.entrySet()) {
                    final List<String> value = e.getValue();
                    if (value != null && !value.isEmpty()) {
                        h.put(e.getKey(), value.iterator().next());
                    }
                }
            }
            return signer.sign(method, uri, h).toString();
        }

        @Override
        public boolean requiresRequestCaching() {
            return false;
        }

        @Override
        public String getAuthorization(final AuthorizationPolicy authPolicy, final URL url, final Message message, final String fullHeader) {
            final Map<String, List<String>> headers = Headers.getSetProtocolHeaders(message);
            final List<String> existing = headers.get("authorization");
            if (existing != null && !existing.isEmpty()) {
                return existing.iterator().next();
            }
            try {
                return sign(
                        message.get("org.apache.cxf.request.method").toString(),
                        url.getPath(),
                        Headers.getSetProtocolHeaders(message));
            } catch (final NoSuchAlgorithmException | InvalidKeyException | IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
