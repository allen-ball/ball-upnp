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
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.Charset;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;
import org.apache.http.protocol.HttpDateGenerator;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * SSDP {@link HttpMessage} interface definition.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface SSDPMessage extends HttpMessage {

    /**
     * SSDP {@link Charset}.
     */
    public static final Charset CHARSET = UTF_8;

    /**
     * SSDP IPv4 broadcast address.
     */
    public static final InetSocketAddress ADDRESS =
        new InetSocketAddress("239.255.255.250", 1900);

    /**
     * {@link HttpDateGenerator} instance.
     */
    public static final HttpDateGenerator GENERATOR = new HttpDateGenerator();

    /**
     * SSDP message header name.
     */
    public static final String
        AL = "AL",
        EXT = "EXT",
        MAN = "MAN",
        MX = "MX",
        NT = "NT",
        NTS = "NTS",
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
     * Method to get the service location.
     *
     * @return  The service location {@link URI}.
     */
    public default URI getLocation() {
        Header header = getFirstHeader(HttpHeaders.LOCATION);

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
    public default URI getST() {
        Header header = getFirstHeader(ST);
        String value = (header != null) ? header.getValue() : null;

        return (value != null) ? URI.create(value) : null;
    }

    /**
     * Method to get the {@value #USN} {@link URI}.
     *
     * @return  The {@value #USN} {@link URI}.
     */
    public default URI getUSN() {
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
     *
     * @see #ADDRESS
     */
    public default DatagramPacket toDatagramPacket(InetSocketAddress address) throws SocketException {
        byte[] bytes = toString().getBytes(CHARSET);

        return new DatagramPacket(bytes, 0, bytes.length, address);
    }

    /**
     * Method to encode {@link.this} {@link SSDPMessage} to a
     * {@link DatagramPacket}.
     *
     * @return  The {@link DatagramPacket}.
     *
     * @throws  SocketException
     *                          If this {@link SSDPMessage} cannot be
     *                          converted to a {@link DatagramPacket}.
     *
     * @see #ADDRESS
     */
    public default DatagramPacket toDatagramPacket() throws SocketException {
        return toDatagramPacket(ADDRESS);
    }
}
