/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp.ssdp;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;

/**
 * SSDP {@link HttpMessage} interface definition.
 *
 * @author <a href="mailto:ball@iprotium.com">Allen D. Ball</a>
 * @version $Revision$
 */
public interface SSDPMessage extends HttpMessage {

    /**
     * SSDP IPv4 broadcast address
     */
    public static final InetSocketAddress ADDRESS =
        new InetSocketAddress("239.255.255.250", 1900);

    /**
     * {@link SSDPMessage} {@link Charset}
     */
    public static final Charset CHARSET = Charset.forName("UTF-8");

    /**
     * SSDP message header name
     */
    public static final String
        MAN = "MAN",
        MX = "MX",
        ST = "ST",
        USN = "USN";

    /**
     * {@link SSDPMessage} end-of-line sequence.
     */
    public static final String CRLF = "\r\n";

    /**
     * {@link Helper} class instance to help implement
     * {@link #getLocation()}, {@link #getST()}, and {@link #getUSN()}
     * methods.
     */
    public static final Helper HELPER = new Helper();

    /**
     * Method to get the service location.
     *
     * @return  The service location {@link URI}.
     */
    public URI getLocation();

    /**
     * Method to get the service type ({@value #ST}).
     *
     * @return  The service type {@link URI}.
     */
    public URI getST();

    /**
     * Method to get the {@value #USN} {@link URI}.
     *
     * @return  The {@value #USN} {@link URI}.
     */
    public URI getUSN();

    /**
     * {@link Helper} class to help implement {@link #getLocation()},
     * {@link #getST()}, and {@link #getUSN()} methods.
     */
    public class Helper {
        private Helper() { }

        /**
         * Method to help implement {@link SSDPMessage#getLocation()}.
         *
         * @param       message         The {@link SSDPMessage}.
         *
         * @return      The {@link URI}.
         */
        public URI getLocation(SSDPMessage message) {
            Header header = message.getFirstHeader(HttpHeaders.LOCATION);
            String value = (header != null) ? header.getValue() : null;

            return (value != null) ? URI.create(value) : null;
        }

        /**
         * Method to help implement {@link SSDPMessage#getST()}.
         *
         * @param       message         The {@link SSDPMessage}.
         *
         * @return      The {@link URI}.
         */
        public URI getST(SSDPMessage message) {
            Header header = message.getFirstHeader(ST);
            String value = (header != null) ? header.getValue() : null;

            return (value != null) ? URI.create(value) : null;
        }

        /**
         * Method to help implement {@link SSDPMessage#getUSN()}.
         *
         * @param       message         The {@link SSDPMessage}.
         *
         * @return      The {@link URI}.
         */
        public URI getUSN(SSDPMessage message) {
            Header header = message.getFirstHeader(USN);
            String value = (header != null) ? header.getValue() : null;

            return (value != null) ? URI.create(value) : null;
        }

        @Override
        public String toString() { return getClass().getName(); }
    }
}
