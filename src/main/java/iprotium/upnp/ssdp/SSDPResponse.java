/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp.ssdp;

import java.net.DatagramPacket;
import java.net.URI;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineParser;

/**
 * SSDP {@link org.apache.http.HttpResponse} implementation.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class SSDPResponse extends BasicHttpResponse implements SSDPMessage {
    private static final BasicLineParser PARSER = BasicLineParser.INSTANCE;

    /**
     * Sole public constructor.
     *
     * @param   packet          The {@link DatagramPacket} containing the
     *                          {@link SSDPResponse}.
     */
    public SSDPResponse(DatagramPacket packet) {
        this(packet.getData(), packet.getOffset(), packet.getLength());
    }

    private SSDPResponse(byte[] data, int offset, int length) {
        this(new String(data, offset, length, CHARSET)
             .split(Pattern.quote(CRLF)));
    }

    private SSDPResponse(String[] lines) {
        super(PARSER.parseStatusLine(lines[0], PARSER));

        for (int i = 1; i < lines.length; i += 1) {
            addHeader(PARSER.parseHeader(lines[i], PARSER));
        }
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

        buffer.append(getStatusLine()).append(CRLF);

        for (Header header : getAllHeaders()) {
            buffer.append(header.toString()).append(CRLF);
        }

        buffer.append(CRLF);

        return buffer.toString();
    }
}
