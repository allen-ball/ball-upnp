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
import java.net.SocketException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpMessage;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpDateGenerator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * SSDP {@link HttpMessage} interface definition.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface SSDPMessage extends HttpMessage, HttpStatus {

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
     * SSDP {@link #ST} value.
     */
    public static final URI SSDP_ALL = URI.create("ssdp:all");

    /**
     * HTTP cache control key.
     */
    public static final String
        MAX_AGE = "max-age";

    /**
     * {@link SSDPMessage} end-of-line sequence.
     */
    public static final String EOL = "\r\n";

    /**
     * {@link SSDPMessage} end-of-message sequence.
     */
    public static final String EOM = EOL + EOL;

    /**
     * Static method to parse a {@link DatagramPacket} to a {@link List} of
     * lines ({@link String}s).
     *
     * @param   packet          The {@link DatagramPacket}.
     *
     * @return  The {@link List} of parsed lines.
     */
    public static List<String> parse(DatagramPacket packet) {
        String string =
            new String(packet.getData(),
                       packet.getOffset(), packet.getLength(),
                       UTF_8);

        return Pattern.compile(EOL).splitAsStream(string).collect(toList());
    }

    /**
     * Method to find the first {@link Header} matching {@code names} and
     * return that value.
     *
     * @param   names           The candidate {@link Header} names.
     *
     * @return  The value or {@code null} if no header found.
     */
    default String getHeaderValue(String... names) {
        String string =
            Stream.of(names)
            .map(this::getFirstHeader)
            .filter(Objects::nonNull)
            .map(Header::getValue)
            .findFirst().orElse(null);

        return string;
    }

    /**
     * Method to find the first {@link Header} matching {@code names} and
     * return the value converted with {@code function}.
     *
     * @param   <T>             The target type.
     * @param   function        The conversion {@code Function}.
     * @param   names           The candidate {@link Header} names.
     *
     * @return  The converted value or {@code null} if no header found.
     */
    default <T> T getHeaderValue(Function<String,T> function,
                                 String... names) {
        String string = getHeaderValue(names);

        return (string != null) ? function.apply(string) : null;
    }

    /**
     * Method to find the first {@link Header} matching {@code name} with a
     * parameter matching {@code parameter} and return that parameter value.
     *
     * @param   name            The target {@link Header} name.
     * @param   parameter       The target parameter name.
     *
     * @return  The value or {@code null} if no header/parameter combination
     *          is found.
     */
    default String getHeaderParameterValue(String name, String parameter) {
        String value =
            Stream.of(getHeaders(name))
            .map(t -> t.getElements())
            .filter(Objects::nonNull)
            .flatMap(t -> Stream.of(t))
            .filter(t -> parameter.equals(t.getName()))
            .map(t -> t.getValue())
            .filter(Objects::nonNull)
            .findFirst().orElse(null);

        return value;
    }

    /**
     * Method to find the first {@link Header} matching {@code name} with a
     * parameter matching {@code parameter} and return that parameter value
     * converted with {@code function}.
     *
     * @param   <T>             The target type.
     * @param   name            The target {@link Header} name.
     * @param   parameter       The target parameter name.
     *
     * @return  The value or {@code null} if no header/parameter combination
     *          is found.
     */
    default <T> T getHeaderParameterValue(Function<String,T> function,
                                          String name, String parameter) {
        String string = getHeaderParameterValue(name, parameter);

        return (string != null) ? function.apply(string) : null;
    }

    /**
     * Method to get the {@value #NT} {@link URI}.
     *
     * @return  The {@value #NT} {@link URI}.
     */
    default URI getNT() { return getHeaderValue(URI::create, NT); }

    /**
     * Method to get the {@value #ST} {@link URI}.
     *
     * @return  The {@value #ST} {@link URI}.
     */
    default URI getST() { return getHeaderValue(URI::create, ST); }

    /**
     * Method to get the {@value #USN} {@link URI}.
     *
     * @return  The {@value #USN} {@link URI}.
     */
    default URI getUSN() { return getHeaderValue(URI::create, USN); }

    /**
     * Method to get the location {@link URI}.
     *
     * @return  The service location {@link URI}.
     */
    default URI getLocation() {
        return getHeaderValue(URI::create, LOCATION, AL);
    }
}
