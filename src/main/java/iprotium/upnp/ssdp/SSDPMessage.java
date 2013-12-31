/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp.ssdp;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.Charset;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;

/**
 * SSDP {@link HttpMessage} interface definition.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
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
        AL = "AL",
        EXT = "EXT",
        MAN = "MAN",
        MX = "MX",
        NT = "NT",
        NTS = "NTS",
        ST = "ST",
        USN = "USN";

    /**
     * {@link SSDPMessage} end-of-line sequence.
     */
    public static final String CRLF = "\r\n";

    /**
     * {@link Impl} instance to help implement {@link #getLocation()},
     * {@link #getST()}, {@link #getUSN()}, and {@link #toDatagramPacket()}
     * methods.
     */
    public static final Impl IMPL = new Impl();

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
     * Method to encode {@code this} {@link SSDPMessage} to a
     * {@link DatagramPacket}.
     *
     * @return  The {@link DatagramPacket}.
     *
     * @see #ADDRESS
     */
    public DatagramPacket toDatagramPacket() throws SocketException;

    /**
     * Class to help implement {@link #getLocation()}, {@link #getST()},
     * {@link #getUSN()}, and {@link #toDatagramPacket()} methods.
     */
    public class Impl {
        private Impl() { }

        /**
         * Method to help implement {@link SSDPMessage#getLocation()}.
         *
         * @param       message         The {@link SSDPMessage}.
         *
         * @return      The {@link URI}.
         */
        public URI getLocation(SSDPMessage message) {
            Header header = message.getFirstHeader(HttpHeaders.LOCATION);

            if (header == null) {
                header = message.getFirstHeader(AL);
            }

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

        /**
         * Method to encode {@code this} {@link SSDPRequest} to a
         * {@link DatagramPacket}.
         *
         * @return      The {@link DatagramPacket}.
         */
        public DatagramPacket toDatagramPacket(SSDPMessage message)
                                                throws SocketException {
            byte[] bytes = message.toString().getBytes(CHARSET);

            return new DatagramPacket(bytes, 0, bytes.length, ADDRESS);
        }

        @Override
        public String toString() { return getClass().getName(); }
    }
}
