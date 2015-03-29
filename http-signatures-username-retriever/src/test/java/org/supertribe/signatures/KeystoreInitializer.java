package org.supertribe.signatures;

import com.tomitribe.tribestream.security.signatures.store.StoreManager;
import org.apache.openejb.loader.SystemInstance;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.File;

@Singleton
@Startup
public class KeystoreInitializer {

    public static final String SECRET = "this is supposed to be the shared secret between client and server. " +
            "Not supposed to be in a constant.";

    public static final String KEY_ALIAS = "support";
    private static final String PWD = "this is sensible ;-)";
    public static final String ALGO = "HmacSHA256";
    private static File KS;

    @PostConstruct
    public void init() throws Exception {
        // init and generate a key
        final File conf = SystemInstance.get().getBase().getDirectory("conf");
        KS = new File(conf, "test.jks");
        StoreManager.get(KS.getAbsolutePath(), PWD.toCharArray(), true);
        StoreManager.get(KS.getAbsolutePath(), PWD.toCharArray()).addSecretKey(KEY_ALIAS, PWD.toCharArray(), new SecretKeySpec(SECRET.getBytes(), ALGO));
    }
}
