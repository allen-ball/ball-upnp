/*
 * $Id$
 *
 * Copyright 2013 - 2019 Allen D. Ball.  All rights reserved.
 */
package ball.upnp.ssdp;

import org.apache.http.HttpHeaders;

/**
 * SSDP discovery ({@value #METHOD}) {@link SSDPRequest}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPNotifyRequest extends SSDPRequest {

    /**
     * {@link SSDPRequest} method name ({@value #METHOD})
     */
    public static final String METHOD = "NOTIFY";

    /**
     * Sole constructor.
     */
    public SSDPNotifyRequest() {
        super(METHOD);

        addHeader(HttpHeaders.HOST.toUpperCase(), toString(ADDRESS));
    }
}
