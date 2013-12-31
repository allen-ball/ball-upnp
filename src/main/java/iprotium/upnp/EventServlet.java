/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp;

import javax.servlet.http.HttpServlet;

/**
 * {@link HttpServlet} implementation to handle {@link Service} events.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class EventServlet extends HttpServlet {
    private static final long serialVersionUID = -9219469886349601815L;

    private final Service service;

    /**
     * Sole constructor.
     *
     * @param   service         The {@link Service}.
     */
    public EventServlet(Service service) {
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
