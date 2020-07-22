package ball.upnp.ssdp;
/*-
 * ##########################################################################
 * UPnP/SSDP Implementation Classes
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2013 - 2020 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.message.BasicLineParser.INSTANCE;
import static org.apache.http.message.BasicLineParser.parseHeader;
import static org.apache.http.message.BasicLineParser.parseStatusLine;

/**
 * SSDP {@link org.apache.http.HttpResponse} implementation.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPResponse extends BasicHttpResponse implements SSDPMessage {
    private final InetAddress from;

    /**
     * Sole public constructor.
     *
     * @param   packet          The {@link DatagramPacket} containing the
     *                          {@link SSDPResponse}.
     */
    public SSDPResponse(DatagramPacket packet) {
        this(packet, toString(packet).split(Pattern.quote(CRLF)));
    }

    private SSDPResponse(DatagramPacket packet, String[] lines) {
        super(parseStatusLine(lines[0], INSTANCE));

        for (int i = 1; i < lines.length; i += 1) {
            addHeader(parseHeader(lines[i], INSTANCE));
        }

        from = packet.getAddress();
    }

    private static String toString(DatagramPacket packet) {
        return new String(packet.getData(),
                          packet.getOffset(), packet.getLength(), UTF_8);
    }

    /**
     * Protected constructor for subclasses.
     *
     * @param   code            The response status code.
     * @param   reason          The response status reason.
     */
    protected SSDPResponse(int code, String reason) {
        super(HttpVersion.HTTP_1_1, code, reason);

        from = null;
    }

    /**
     * Method to get the sender {@link InetAddress}.
     *
     * @return  The sender {@link InetAddress} (may be {@code null}).
     */
    public InetAddress getInetAddress() { return from; }

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
