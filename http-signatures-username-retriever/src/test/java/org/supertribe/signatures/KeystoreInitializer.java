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

    /**
     * Initializes the keystore at ${TRIBESTREAM_HOME}/conf/test.jks, creating a keystore (with this is sensible ;-) as
     * the keystore password), with a secret key of "this is supposed to be the shared secret between client and server.
     * Not supposed to be in a constant." with an alias of "support".
     *
     * This is stored in the keystore with using the HmacSHA256 algorithm.
     *
     * This method is a @PostConstruct on a @Singleton @Startup EJB so the keystore is created as soon as the
     * application starts up. This works great for a test, but is not the recommended approach in production.
     *
     * @throws Exception if an error occurs.
     */
    @PostConstruct
    public void init() throws Exception {
        // init and generate a key
        final File conf = SystemInstance.get().getBase().getDirectory("conf");
        KS = new File(conf, "test.jks");
        StoreManager.get(KS.getAbsolutePath(), PWD.toCharArray(), true);
        StoreManager.get(KS.getAbsolutePath(), PWD.toCharArray()).addSecretKey(KEY_ALIAS, PWD.toCharArray(), new SecretKeySpec(SECRET.getBytes(), ALGO));
    }
}
