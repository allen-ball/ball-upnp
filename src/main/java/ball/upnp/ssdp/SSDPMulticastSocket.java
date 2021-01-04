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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import lombok.ToString;

/**
 * SSDP discovery {@link MulticastSocket} implementation.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ToString
public class SSDPMulticastSocket extends MulticastSocket {
    private static final String ADDRESS = "239.255.255.250";
    private static final int PORT = 1900;

    /**
     * SSDP IPv4 multicast {@link InetSocketAddress}.
     */
    public static final InetSocketAddress INET_SOCKET_ADDRESS =
        new InetSocketAddress(ADDRESS, PORT);

    /**
     * Sole constructor.
     */
    public SSDPMulticastSocket() throws IOException {
        super(PORT);

        setReuseAddress(true);
        setLoopbackMode(false);
        setTimeToLive(2);
        joinGroup(INET_SOCKET_ADDRESS.getAddress());
    }
}
