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
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
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

    /**
     * Method to parse a {@link SSDPResponse} from a
     * {@link DatagramPacket}.
     *
     * @param   packet          The {@link DatagramPacket}.
     *
     * @return  A new {@link SSDPResponse}.
     */
    public static SSDPResponse from(DatagramPacket packet) {
        return new SSDPResponse(packet);
    }

    /**
     * {@code M-SEARCH} {@link SSDPResponse}.
     *
     * @param   maxAge          The {@code MAX-AGE} header parameter value.
     * @param   location        The {@code LOCATION} header value.
     * @param   st              The {@code ST} header value.
     * @param   usn             The {@code USN} header value.
     */
    public static SSDPResponse msearch(int maxAge,
                                       URI location, URI st, URI usn) {
        SSDPResponse response =
            new SSDPResponse(SC_OK, "OK")
            .header(CACHE_CONTROL, MAX_AGE + "=" + String.valueOf(maxAge))
            .header(DATE, GENERATOR.getCurrentDate())
            .header(EXT, (String) null)
            .header(LOCATION, location)
            .header(SERVER, "UPnP/1.0")
            .header(ST, st)
            .header(USN, usn);

        return response;
    }

    private SocketAddress address = null;

    private SSDPResponse(int code, String reason) {
        super(HttpVersion.HTTP_1_1, code, reason);
    }

    private SSDPResponse(DatagramPacket packet) {
        this(packet, SSDPMessage.parse(packet));
    }

    private SSDPResponse(DatagramPacket packet, List<String> lines) {
        super(parseStatusLine(lines.remove(0), INSTANCE));

        lines.stream().forEach(t -> addHeader(parseHeader(t, INSTANCE)));

        address = packet.getSocketAddress();
    }

    /**
     * Method to get the {@link SocketAddress} from the
     * {@link DatagramPacket} if {@link.this} {@link SSDPResponse} was
     * parsed from a packet.
     *
     * @return  The {@link SocketAddress}.
     */
    public SocketAddress getSocketAddress() { return address; }

    /**
     * {@link String} fluent header setter.
     *
     * @param   name            The header name.
     * @param   value           The header value.
     *
     * @return  {@link.this}
     */
    public SSDPResponse header(String name, String value) {
        setHeader(name, value);

        return this;
    }

    /**
     * {@link SocketAddress} fluent header setter.
     *
     * @param   name            The header name.
     * @param   value           The header value.
     *
     * @return  {@link.this}
     */
    public SSDPResponse header(String name, SocketAddress value) {
        return header(name, (InetSocketAddress) value);
    }

    /**
     * {@link InetSocketAddress} fluent header setter.
     *
     * @param   name            The header name.
     * @param   value           The header value.
     *
     * @return  {@link.this}
     */
    public SSDPResponse header(String name, InetSocketAddress value) {
        return header(name,
                      t -> String.format("%s:%d",
                                         value.getAddress().getHostAddress(),
                                         value.getPort()),
                      value);
    }

    /**
     * {@link URI} fluent header setter.
     *
     * @param   name            The header name.
     * @param   value           The header value.
     *
     * @return  {@link.this}
     */
    public SSDPResponse header(String name, URI value) {
        return header(name, URI::toASCIIString, value);
    }

    /**
     * Fluent header setter.
     *
     * @param   <T>             The target type.
     * @param   name            The header name.
     * @param   value           The header value.
     *
     * @return  {@link.this}
     */
    public <T> SSDPResponse header(String name,
                                   Function<T,String> function, T value) {
        setHeader(name, (value != null) ? function.apply(value) : null);

        return this;
    }

    @Override
    public String toString() {
        String string =
            Stream.concat(Stream.of(getStatusLine()),
                          Stream.of(getAllHeaders()))
            .filter(Objects::nonNull)
            .map(Objects::toString)
            .collect(joining(EOL, EMPTY, EOM));

        return string;
    }
}
