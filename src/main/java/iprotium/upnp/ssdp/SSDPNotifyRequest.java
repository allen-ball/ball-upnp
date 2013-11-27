/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp.ssdp;

import org.apache.http.HttpHeaders;

/**
 * SSDP discovery ({@value #METHOD}) {@link SSDPRequest}.
 *
 * @author <a href="mailto:ball@iprotium.com">Allen D. Ball</a>
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

        addHeader(HttpHeaders.HOST, toString(ADDRESS));
    }
}
