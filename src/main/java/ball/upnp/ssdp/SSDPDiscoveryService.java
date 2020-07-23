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
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.http.ParseException;

/**
 * SSDP discovery {@link ScheduledThreadPoolExecutor} implementation.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPDiscoveryService extends ScheduledThreadPoolExecutor {
    private static final int SIZE = 4;

    /**
     * SSDP IPv4 multicast socket address.
     */
    public static final InetSocketAddress MULTICAST_SOCKET_ADDRESS =
        new InetSocketAddress("239.255.255.250", 1900);

    private final MulticastSocket multicast;
    private final CopyOnWriteArrayList<Listener> listeners =
        new CopyOnWriteArrayList<>();

    /**
     * Sole constructor.
     *
     * @throws  IOException     If the underlying {@link MulticastSocket}
     *                          cannot be conditioned.
     */
    public SSDPDiscoveryService() throws IOException {
        super(SIZE);

        multicast = new MulticastSocket(MULTICAST_SOCKET_ADDRESS.getPort());
        multicast.setLoopbackMode(false);
        multicast.setTimeToLive(255);
        multicast.joinGroup(MULTICAST_SOCKET_ADDRESS.getAddress());

        submit(() -> receive(multicast));
    }

    /**
     * Method to add a {@link Listener}.
     *
     * @param   listener        The {@link Listener}.
     *
     * @return  {@link.this}
     */
    public SSDPDiscoveryService addListener(Listener listener) {
        listeners.add(listener);

        return this;
    }

    /**
     * Method to remove a {@link Listener}.
     *
     * @param   listener        The {@link Listener}.
     *
     * @return  {@link.this}
     */
    public SSDPDiscoveryService removeListener(Listener listener) {
        listeners.remove(listener);

        return this;
    }

    private void fireSendEvent(DatagramSocket socket, SSDPMessage message) {
        listeners.stream().forEach(t -> t.sendEvent(this, socket, message));
    }

    private void fireReceiveEvent(DatagramSocket socket, SSDPMessage message) {
        listeners.stream().forEach(t -> t.receiveEvent(this, socket, message));
    }

    /**
     * Send {@link SSDPDiscoveryRequest}s every {@code interval} seconds.
     *
     * @param   interval        The minimum interval (in seconds) between
     *                          broadcast messages.
     *
     * @return  {@link.this}
     */
    public SSDPDiscoveryService discover(int interval) {
        if (interval > 0) {
            scheduleAtFixedRate(() -> multicast(new SSDPDiscoveryRequest(MULTICAST_SOCKET_ADDRESS)),
                                0, interval, TimeUnit.SECONDS);
        }

        return this;
    }

    /**
     * Method queue an {@link SSDPMessage} for multicast.
     *
     * @param   message         The {@link SSDPMessage} to send.
     */
    public void multicast(SSDPMessage message) {
        send(multicast, MULTICAST_SOCKET_ADDRESS, message);
    }

    /**
     * Method to queue an {@link SSDPMessage} for sending on a
     * {@link DatagramSocket}.
     *
     * @param   socket          The {@link DatagramSocket}.
     * @param   address         The destination {@link InetSocketAddress}.
     * @param   message         The {@link SSDPMessage} to send.
     */
    public void send(DatagramSocket socket,
                     InetSocketAddress address, SSDPMessage message) {
        submit(() -> task(socket, address, message));
    }

    private void task(DatagramSocket socket,
                      InetSocketAddress address, SSDPMessage message) {
        try {
            fireSendEvent(socket, message);
            socket.send(message.toDatagramPacket(address));
        } catch (IOException exception) {
        }
    }

    private void receive(DatagramSocket socket) {
        try {
            for (;;) {
                byte[] bytes = new byte[8 * 1024];
                DatagramPacket packet =
                    new DatagramPacket(bytes, bytes.length);

                try {
                    socket.receive(packet);

                    SSDPMessage message = parse(packet);

                    if (message != null) {
                        fireReceiveEvent(socket, message);
                    }
                } catch (SocketTimeoutException exception) {
                }
            }
        } catch (IOException exception) {
        }
    }

    private SSDPMessage parse(DatagramPacket packet) {
        SSDPMessage message = null;

        if (message == null) {
            try {
                message = new SSDPResponse(packet);
            } catch (ParseException exception) {
            }
        }

        if (message == null) {
            try {
                message = new SSDPRequest(packet);
            } catch (ParseException exception) {
            }
        }

        return message;
    }

    /**
     * {@link SSDPDiscoveryService} listener interface definition.
     */
    public interface Listener {

        /**
         * Callback made just before sending a {@link SSDPMessage}.
         *
         * @param       service         The {@link SSDPDiscoveryService}.
         * @param       socket          The {@link DatagramSocket}.
         * @param       message         The {@link SSDPMessage}.
         */
        public void sendEvent(SSDPDiscoveryService service,
                              DatagramSocket socket,
                              SSDPMessage message);

        /**
         * Callback made after receiving a {@link SSDPMessage}.
         *
         * @param       service         The {@link SSDPDiscoveryService}.
         * @param       socket          The {@link DatagramSocket}.
         * @param       message         The {@link SSDPMessage}.
         */
        public void receiveEvent(SSDPDiscoveryService service,
                                 DatagramSocket socket,
                                 SSDPMessage message);
    }
}
