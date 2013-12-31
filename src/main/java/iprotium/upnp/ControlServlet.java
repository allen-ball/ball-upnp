/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp;

import javax.servlet.http.HttpServlet;

/**
 * {@link HttpServlet} implementation to handle {@link Service} control
 * requests.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class ControlServlet extends HttpServlet {
    private static final long serialVersionUID = 800025004176271470L;

    private final Service service;

    /**
     * Sole constructor.
     *
     * @param   service         The {@link Service}.
     */
    public ControlServlet(Service service) {
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
