package ball.upnp.ssdp;
/*-
 * ##########################################################################
 * UPnP/SSDP Implementation Classes
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2013 - 2021 Allen D. Ball
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
import org.apache.http.message.BasicHttpRequest;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.math.NumberUtils.toInt;
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
     * {@link SSDPRequest} enumerated {@link Method}s.
     */
    public enum Method {
        NOTIFY(null), MSEARCH("M-SEARCH");

        private final String string;

        Method(String string) { this.string = string; }

        @Override
        public String toString() { return (string != null) ? string : name(); }

        /**
         * Test if the argument method matches {@link.this}.
         *
         * @param       method          The method to test.
         *
         * @return      {@code true} is match; {@code false} otherwise.
         */
        public boolean is(String method) {
            return Objects.equals(toString(), method);
        }
    }

    /**
     * Method to parse a {@link SSDPRequest} from a {@link DatagramPacket}.
     *
     * @param   packet          The {@link DatagramPacket}.
     *
     * @return  A new {@link SSDPRequest}.
     */
    public static SSDPRequest from(DatagramPacket packet) {
        return new SSDPRequest(packet);
    }

    private SocketAddress address = null;
    private long timestamp = System.currentTimeMillis();
    private Long expiration = null;

    /**
     * Sole non-private constructor.
     *
     * @param   method          The {@link SSDPRequest} {@link Method}.
     */
    protected SSDPRequest(Method method) {
        super(method.toString(), "*", HttpVersion.HTTP_1_1);
    }

    private SSDPRequest(DatagramPacket packet) {
        this(packet, SSDPMessage.parse(packet));
    }

    private SSDPRequest(DatagramPacket packet, List<String> lines) {
        super(parseRequestLine(lines.remove(0), INSTANCE));

        lines.stream().forEach(t -> addHeader(parseHeader(t, INSTANCE)));

        address = packet.getSocketAddress();
    }

    /**
     * Method to get the {@link org.apache.http.RequestLine} method.
     *
     * @return  The method specified on the request line.
     */
    public String getMethod() { return getRequestLine().getMethod(); }

    /**
     * Method to get the {@link SocketAddress} from the
     * {@link DatagramPacket} if {@link.this} {@link SSDPRequest} was
     * parsed from a packet.
     *
     * @return  The {@link SocketAddress}.
     */
    public SocketAddress getSocketAddress() { return address; }

    /**
     * Method to get the {@code MX} header value as an {@code int}.  Returns
     * {@code 120} if the header is not specified or the if the value is not
     * in the range of {@code 1 <= mx <= 120}.
     *
     * @return  The {@code MX} value.
     */
    public int getMX() {
        return getHeaderValue(t -> min(max(toInt(t, 120), 1), 120), MX);
    }

    /**
     * {@link String} fluent header setter.
     *
     * @param   name            The header name.
     * @param   value           The header value.
     *
     * @return  {@link.this}
     */
    public SSDPRequest header(String name, String value) {
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
    public SSDPRequest header(String name, SocketAddress value) {
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
    public SSDPRequest header(String name, InetSocketAddress value) {
        return header(name,
                      t -> String.format("%s:%d",
                                         value.getAddress().getHostAddress(),
                                         value.getPort()),
                      value);
    }

    /**
     * {@link Number} fluent header setter.
     *
     * @param   name            The header name.
     * @param   value           The header value.
     *
     * @return  {@link.this}
     */
    public SSDPRequest header(String name, Number value) {
        return header(name, Number::toString, value);
    }

    /**
     * {@link URI} fluent header setter.
     *
     * @param   name            The header name.
     * @param   value           The header value.
     *
     * @return  {@link.this}
     */
    public SSDPRequest header(String name, URI value) {
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
    public <T> SSDPRequest header(String name,
                                  Function<T,String> function, T value) {
        setHeader(name, (value != null) ? function.apply(value) : null);

        return this;
    }

    @Override
    public long getExpiration() {
        if (expiration == null) {
            expiration = SSDPMessage.getExpiration(this, timestamp);
        }

        return expiration;
    }

    @Override
    public String toString() {
        String string =
            Stream.concat(Stream.of(getRequestLine()),
                          Stream.of(getAllHeaders()))
            .filter(Objects::nonNull)
            .map(Objects::toString)
            .collect(joining(EOL, EMPTY, EOM));

        return string;
    }
}
