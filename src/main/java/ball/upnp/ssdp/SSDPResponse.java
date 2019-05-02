/*
 * $Id$
 *
 * Copyright 2013 - 2019 Allen D. Ball.  All rights reserved.
 */
package ball.upnp.ssdp;

import java.net.DatagramPacket;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineParser;

/**
 * SSDP {@link org.apache.http.HttpResponse} implementation.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
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
        super(BasicLineParser.parseStatusLine(lines[0], PARSER));

        for (int i = 1; i < lines.length; i += 1) {
            addHeader(BasicLineParser.parseHeader(lines[i], PARSER));
        }
    }

    /**
     * Protected constructor for subclasses.
     *
     * @param   code            The response status code.
     * @param   reason          The response status reason.
     */
    protected SSDPResponse(int code, String reason) {
        super(HttpVersion.HTTP_1_1, code, reason);
    }

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
