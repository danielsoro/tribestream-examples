package org.supertribe.signatures;

import com.tomitribe.tribestream.security.signatures.Base64;
import com.tomitribe.tribestream.security.signatures.Signature;
import com.tomitribe.tribestream.security.signatures.Signatures;
import com.tomitribe.tribestream.security.signatures.Signer;
import org.apache.openejb.cipher.StaticDESPasswordCipher;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DummyTest {

    @Test
    public void cipher() {
        System.out.println(new ReversePasswordCipher().encrypt("this is sensible ;-)"));
        System.out.println(new StaticDESPasswordCipher().encrypt("this is sensible ;-)"));
    }

    @Test
    public void calcDigest() throws Exception {
        final Signature signature = new Signature("kids", "hmac-sha256", null, "(request-target)", "date");
        final SecretKey key = new SecretKeySpec("PennyLunaLuka".getBytes(), "HmacSHA256");
        final Signer signer = new Signer(key, signature);
        final String method = "GET";
        final String uri = "/signature-prototype-1.0-SNAPSHOT/api/colors/preferred";
        final Map<String, String> headers = new HashMap<>();
        headers.put("Date", "Tue, 07 Jun 2014 20:51:35 GMT");
        final Signature result = signer.sign(method, uri, headers);
        System.out.println(result);

        final String signingString = Signatures.createSigningString(Arrays.asList("(request-target) date".split(" ")), method, uri, headers);
        System.out.println(signingString);
        System.out.println(new String(Base64.encodeBase64(signingString.getBytes()), "UTF-8"));

        final String expected = "KHJlcXVlc3QtdGFyZ2V0KTogZ2V0IC9zaWduYXR1cmUtcHJvdG90eXBlLTEuMC1TTkFQU0hPVC9hcGkvY29sb3JzL3ByZWZlcnJlZApkYXRlOiBUdWUsIDA3IEp1biAyMDE0IDIwOjUxOjM1IEdNVA==";
        System.out.println(expected);
        System.out.println(new String(Base64.decodeBase64(expected.getBytes()), "UTF-8"));

        System.out.println(expected.equals(new String(Base64.encodeBase64(signingString.getBytes()), "UTF-8")));

        System.out.println(new String(Base64.decodeBase64("KHJlcXVlc3QtdGFyZ2V0KTogZ2V0IC9zaWduYXR1cmUtcHJvdG90eXBlLTEuMC1TTkFQU0hPVC9hcGkvY29sb3JzL3ByZWZlcnJlZFxuZGF0ZTogVHVlLCAwNyBKdW4gMjAxNCAyMDo1MTozNSBHTVQ=".getBytes()), "UTF-8"));
    }

}
