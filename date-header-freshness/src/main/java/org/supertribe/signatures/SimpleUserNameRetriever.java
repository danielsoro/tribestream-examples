/*
 * Tomitribe Confidential
 *
 * Copyright(c) Tomitribe Corporation. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package org.supertribe.signatures;

import com.tomitribe.tribestream.security.signatures.Signature;
import com.tomitribe.tribestream.security.signatures.SignatureUsernameRetriever;

public class SimpleUserNameRetriever implements SignatureUsernameRetriever {
    @Override
    public String getUsername(final Signature signature) {
        return "support";
    }
}
