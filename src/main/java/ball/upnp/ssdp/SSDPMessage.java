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
import java.util.List;
import java.net.SocketException;
import java.net.URI;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;
import org.apache.http.protocol.HttpDateGenerator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * SSDP {@link HttpMessage} interface definition.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface SSDPMessage extends HttpMessage {

    /**
     * {@link HttpDateGenerator} instance.
     */
    public static final HttpDateGenerator GENERATOR = new HttpDateGenerator();

    /**
     * SSDP message header name.
     */
    public static final String
        AL = "AL",
        CACHE_CONTROL = HttpHeaders.CACHE_CONTROL.toUpperCase(),
        DATE = HttpHeaders.DATE,
        EXPIRES = HttpHeaders.EXPIRES.toUpperCase(),
        EXT = "EXT",
        HOST = HttpHeaders.HOST.toUpperCase(),
        LOCATION = HttpHeaders.LOCATION.toUpperCase(),
        MAN = "MAN",
        MX = "MX",
        NT = "NT",
        NTS = "NTS",
        SERVER = HttpHeaders.SERVER.toUpperCase(),
        ST = "ST",
        USN = "USN";

    /**
     * SSDP {@link #NTS} value.
     */
    public static final String
        SSDP_ALIVE = "ssdp:alive",
        SSDP_BYEBYE = "ssdp:byebye";

    /**
     * HTTP cache control key.
     */
    public static final String
        MAX_AGE = "max-age";

    /**
     * {@link SSDPMessage} end-of-line sequence.
     */
    public static final String CRLF = "\r\n";

    /**
     * See {@link #addHeader(String,String)}.
     *
     * @param   name            The header name.
     * @param   name            The header value.
     */
    default void addHeader(String name, SocketAddress value) {
        InetSocketAddress address = (InetSocketAddress) value;

        addHeader(name, String.format("%s:%d",
                                      address.getAddress().getHostAddress(),
                                      address.getPort()));
    }

    /**
     * See {@link #setHeader(String,String)}.
     *
     * @param   name            The header name.
     * @param   name            The header value.
     */
    default void setHeader(String name, SocketAddress value) {
        InetSocketAddress address = (InetSocketAddress) value;

        addHeader(name, String.format("%s:%d",
                                      address.getAddress().getHostAddress(),
                                      address.getPort()));
    }

    /**
     * See {@link #addHeader(String,String)}.
     *
     * @param   name            The header name.
     * @param   name            The header value.
     */
    default void addHeader(String name, URI value) {
        addHeader(name, (value != null) ? value.toASCIIString() : null);
    }

    /**
     * See {@link #setHeader(String,String)}.
     *
     * @param   name            The header name.
     * @param   name            The header value.
     */
    default void setHeader(String name, URI value) {
        setHeader(name, (value != null) ? value.toASCIIString() : null);
    }

    /**
     * Method to get the service location.
     *
     * @return  The service location {@link URI}.
     */
    default URI getLocation() {
        Header header = getFirstHeader(LOCATION);

        if (header == null) {
            header = getFirstHeader(AL);
        }

        String value = (header != null) ? header.getValue() : null;

        return (value != null) ? URI.create(value) : null;
    }

    /**
     * Method to get the service type ({@value #ST}).
     *
     * @return  The service type {@link URI}.
     */
    default URI getST() {
        Header header = getFirstHeader(ST);
        String value = (header != null) ? header.getValue() : null;

        return (value != null) ? URI.create(value) : null;
    }

    /**
     * Method to get the {@value #USN} {@link URI}.
     *
     * @return  The {@value #USN} {@link URI}.
     */
    default URI getUSN() {
        Header header = getFirstHeader(USN);
        String value = (header != null) ? header.getValue() : null;

        return (value != null) ? URI.create(value) : null;
    }

    /**
     * Method to encode {@link.this} {@link SSDPMessage} to a
     * {@link DatagramPacket}.
     *
     * @param   address         The {@link InetSocketAddress} to send the
     *                          packet.
     *
     * @return  The {@link DatagramPacket}.
     *
     * @throws  SocketException
     *                          If this {@link SSDPMessage} cannot be
     *                          converted to a {@link DatagramPacket}.
     */
    default DatagramPacket toDatagramPacket(InetSocketAddress address) throws SocketException {
        byte[] bytes = toString().getBytes(UTF_8);

        return new DatagramPacket(bytes, 0, bytes.length, address);
    }

    public static List<String> parse(DatagramPacket packet) {
        String string =
            new String(packet.getData(), packet.getOffset(), packet.getLength(), UTF_8);

        return Pattern.compile(CRLF).splitAsStream(string).collect(toList());
    }
}
