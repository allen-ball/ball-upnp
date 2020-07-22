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
import java.net.InetSocketAddress;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.message.BasicLineParser.INSTANCE;
import static org.apache.http.message.BasicLineParser.parseHeader;
import static org.apache.http.message.BasicLineParser.parseRequestLine;

/**
 * SSDP {@link org.apache.http.HttpRequest} implementation.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPRequest extends BasicHttpRequest implements SSDPMessage {

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
        this(new String(data, offset, length, UTF_8)
             .split(Pattern.quote(CRLF)));
    }

    private SSDPRequest(String[] lines) {
        super(parseRequestLine(lines[0], INSTANCE));

        for (int i = 1; i < lines.length; i += 1) {
            addHeader(parseHeader(lines[i], INSTANCE));
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
