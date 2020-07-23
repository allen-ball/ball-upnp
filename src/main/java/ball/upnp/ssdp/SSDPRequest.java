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
import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
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
    private SocketAddress address = null;

    /**
     * Sole public constructor.
     *
     * @param   packet          The {@link DatagramPacket} containing the
     *                          {@link SSDPRequest}.
     */
    public SSDPRequest(DatagramPacket packet) {
        this(packet, SSDPMessage.parse(packet));
    }

    private SSDPRequest(DatagramPacket packet, List<String> list) {
        super(parseRequestLine(list.remove(0), INSTANCE));

        list.stream().forEach(t -> addHeader(parseHeader(t, INSTANCE)));

        address = packet.getSocketAddress();
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
     * Method to get the {@link SocketAddress} from the
     * {@link DatagramPacket} if {@link.this} {@link SSDPRequest} was
     * parsed rom a packet.
     *
     * @return  The {@link SocketAddress}.
     */
    public SocketAddress getSocketAddress() { return address; }

    @Override
    public String toString() {
        String string =
            Stream.concat(Stream.of(getRequestLine()),
                          Stream.of(getAllHeaders()))
            .filter(Objects::nonNull)
            .map(Objects::toString)
            .collect(joining(CRLF, EMPTY, CRLF + CRLF));

        return string;
    }
}
