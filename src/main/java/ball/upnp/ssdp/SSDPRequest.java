/*
 * $Id$
 *
 * Copyright 2013 - 2019 Allen D. Ball.  All rights reserved.
 */
package ball.upnp.ssdp;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicLineParser;

/**
 * SSDP {@link org.apache.http.HttpRequest} implementation.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
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
        super(BasicLineParser.parseRequestLine(lines[0], PARSER));

        for (int i = 1; i < lines.length; i += 1) {
            addHeader(BasicLineParser.parseHeader(lines[i], PARSER));
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
