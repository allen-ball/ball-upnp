/*
 * $Id$
 *
 * Copyright 2013, 2014 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServlet} implementation for redirects.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class RedirectServlet extends HttpServlet {
    private static final long serialVersionUID = 2685222049618705078L;

    private final String path;

    /**
     * Sole constructor.
     *
     * @param   path            The path to redirect requests to.
     */
    public RedirectServlet(String path) {
        super();

        if (path != null) {
            this.path = path;
        } else {
            throw new NullPointerException("path");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException,
                                                              ServletException {
        response.sendRedirect(path);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + path;
    }
}