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
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicLineParser;

/**
 * SSDP {@link org.apache.http.HttpRequest} implementation.
 *
 * @author <a href="mailto:ball@iprotium.com">Allen D. Ball</a>
 * @version $Revision$
 */
public class SSDPRequest extends BasicHttpRequest implements SSDPMessage {
    private static final BasicLineParser PARSER = BasicLineParser.INSTANCE;

    /**
     * Sole public constructor.
     *
     * @param   packet          The {@link DatagramPacket} containing the
     *                          {@link SSDPRequest}.
     */
    public SSDPRequest(DatagramPacket packet) {
        this(packet.getData(), packet.getOffset(), packet.getLength());
    }

    private SSDPRequest(byte[] data, int offset, int length) {
        this(new String(data, offset, length, CHARSET)
             .split(Pattern.quote(CRLF)));
    }

    private SSDPRequest(String[] lines) {
        super(PARSER.parseRequestLine(lines[0], PARSER));

        for (int i = 1; i < lines.length; i += 1) {
            addHeader(PARSER.parseHeader(lines[i], PARSER));
        }
    }

    /**
     * Protected constructor for subclasses.
     *
     * @param   method          The name of the SSDP method.
     */
    protected SSDPRequest(String method) {
        super(method, "*", HttpVersion.HTTP_1_1);
    }

    /**
     * Method to encode {@code this} {@link SSDPRequest} to a
     * {@link DatagramPacket}.
     *
     * @return  The {@link DatagramPacket}.
     *
     * @see #ADDRESS
     */
    public DatagramPacket toDatagramPacket() throws SocketException {
        byte[] bytes = toString().getBytes(CHARSET);

        return new DatagramPacket(bytes, 0, bytes.length, ADDRESS);
    }

    /**
     * Convenience method to format a {@link InetSocketAddress} to its
     * {@link String} representation.
     *
     * @param   address         The {@link InetSocketAddress}.
     *
     * @return  The {@link String} representation.
     */
    protected String toString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    @Override
    public URI getLocation() { return HELPER.getLocation(this); }

    @Override
    public URI getST() { return HELPER.getST(this); }

    @Override
    public URI getUSN() { return HELPER.getUSN(this); }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append(getRequestLine()).append(CRLF);

        for (Header header : getAllHeaders()) {
            buffer.append(header.toString()).append(CRLF);
        }

        buffer.append(CRLF);

        return buffer.toString();
    }
}
