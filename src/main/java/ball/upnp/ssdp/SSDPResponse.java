package ball.upnp.ssdp;
/*-
 * ##########################################################################
 * UPnP/SSDP Implementation Classes
 * %%
 * Copyright (C) 2013 - 2022 Allen D. Ball
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
import java.util.stream.Stream;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.message.BasicLineParser;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.util.CharArrayBuffer;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * SSDP {@link org.apache.hc.core5.http.HttpResponse} implementation.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public class SSDPResponse extends BasicHttpResponse implements SSDPMessage {
    private static final long serialVersionUID = -5163694432470542459L;

    /**
     * Method to parse a {@link SSDPResponse} from a {@link DatagramPacket}.
     *
     * @param   packet          The {@link DatagramPacket}.
     *
     * @return  A new {@link SSDPResponse}.
     *
     * @throws  ParseException  If the {@link DatagramPacket} cannot be
     *                          parsed.
     */
    public static SSDPResponse from(DatagramPacket packet) throws ParseException {
        List<CharArrayBuffer> list = SSDPMessage.parse(packet);
        StatusLine line = BasicLineParser.INSTANCE.parseStatusLine(list.remove(0));
        SSDPResponse response = new SSDPResponse(line.getStatusCode(), line.getReasonPhrase());

        for (CharArrayBuffer buffer : list) {
            response.addHeader(BasicLineParser.INSTANCE.parseHeader(buffer));
        }

        return response;
    }

    /** @serial */ private SocketAddress address = null;
    /** @serial */ private long timestamp = System.currentTimeMillis();
    /** @serial */ private Long expiration = null;

    /**
     * Sole constructor.
     *
     * @param   code            The {@link SSDPRequest} {@code code}.
     * @param   reason          The {@link SSDPRequest} reason.
     */
    protected SSDPResponse(int code, String reason) {
        super(code, reason);
    }

    /**
     * Method to get the {@link SocketAddress} from the
     * {@link DatagramPacket} if {@link.this} {@link SSDPResponse} was
     * parsed from a packet.
     *
     * @return  The {@link SocketAddress}.
     */
    public SocketAddress getSocketAddress() { return address; }

    public String getStatusLine() {
        String string =
            Stream.of(getVersion(), getCode(), getReasonPhrase())
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(joining(SPACE));

        return string;
    }

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
        return header(name, t -> String.format("%s:%d", value.getAddress().getHostAddress(), value.getPort()), value);
    }

    /**
     * {@link Number} fluent header setter.
     *
     * @param   name            The header name.
     * @param   value           The header value.
     *
     * @return  {@link.this}
     */
    public SSDPResponse header(String name, Number value) {
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
    public <T> SSDPResponse header(String name, Function<T,String> function, T value) {
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
            Stream.concat(Stream.of(getStatusLine()), Stream.of(getHeaders()))
            .filter(Objects::nonNull)
            .map(Objects::toString)
            .collect(joining(EOL, EMPTY, EOM));

        return string;
    }
}
