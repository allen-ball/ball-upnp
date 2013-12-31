/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp;

import javax.servlet.http.HttpServlet;

/**
 * {@link HttpServlet} implementation to serve {@link Service} Control
 * Protocol Descriptions.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class SCPDServlet extends HttpServlet {
    private static final long serialVersionUID = -1595387597799529203L;

    private final Service service;

    /**
     * Sole constructor.
     *
     * @param   service         The {@link Service}.
     */
    public SCPDServlet(Service service) {
        super();

        if (service != null) {
            this.service = service;
        } else {
            throw new NullPointerException("service");
        }
    }

    @Override
    public String toString() {
        return (getClass().getSimpleName()
                + "[" + service.getUSN().toString() + "]");
    }
}
