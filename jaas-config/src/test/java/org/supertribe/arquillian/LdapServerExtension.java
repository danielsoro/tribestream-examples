/*
 * Tomitribe Confidential
 *
 * Copyright(c) Tomitribe Corporation. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package org.supertribe.arquillian;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class LdapServerExtension implements LoadableExtension {

    /**
     * Add the LdapServerLifecycleExecuter as an event observer to the Arquillian ExtensionBuilder
     *
     * @param builder ExtensionBuilder
     */
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.observer(LdapServerLifecycleExecuter.class);
    }

}

