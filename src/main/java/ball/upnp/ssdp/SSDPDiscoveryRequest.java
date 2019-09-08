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
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPDiscoveryRequest extends SSDPRequest {

    /**
     * {@link SSDPRequest} method name ({@value #METHOD})
     */
    public static final String METHOD = "M-SEARCH";

    /**
     * Sole constructor.
     */
    public SSDPDiscoveryRequest() {
        super(METHOD);

        addHeader(HttpHeaders.HOST.toUpperCase(), toString(ADDRESS));
        addHeader(MAN, "\"ssdp:discover\"");
        addHeader(MX, String.valueOf(120));
        addHeader(ST, "ssdp:all");
    }
}
