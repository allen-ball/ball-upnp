/*
 * $Id$
 *
 * Copyright 2013, 2014 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import ball.io.IOUtil;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.InputStream;
import javax.activation.DataSource;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServlet} implementation to serve a {@link DataSource}.
 *
 * {@bean-info}
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class DataSourceServlet extends HttpServlet {
    private static final long serialVersionUID = 4471660723759189225L;

    private DataSource ds = null;

    /**
     * Sole constructor.
     *
     * @param   ds              The {@link DataSource}.
     */
    @ConstructorProperties({ "dataSource" })
    public DataSourceServlet(DataSource ds) {
        super();

        setDataSource(ds);
    }

    public DataSource getDataSource() { return ds; }
    public void setDataSource(DataSource ds) { this.ds = ds; }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException,
                                                              ServletException {
        DataSource ds = getDataSource();

        if (ds != null) {
            InputStream in = null;
            ServletOutputStream out = null;

            try {
                if (ds.getContentType() != null) {
                    response.setContentType(ds.getContentType());
                }

                in = ds.getInputStream();
                out = response.getOutputStream();
                IOUtil.copy(in, out);
            } finally {
                IOUtil.close(in);
                IOUtil.close(out);
            }
        }
    }

    @Override
    public String toString() { return getClass().getSimpleName(); }
}