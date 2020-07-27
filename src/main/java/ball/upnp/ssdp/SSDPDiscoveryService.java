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
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.apache.http.ParseException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * SSDP discovery {@link ScheduledThreadPoolExecutor} implementation.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPDiscoveryService extends ScheduledThreadPoolExecutor {
    private static final String MULTICAST_ADDRESS = "239.255.255.250";
    private static final int MULTICAST_PORT = 1900;

    /**
     * SSDP IPv4 multicast {@link SocketAddress}.
     */
    public static final InetSocketAddress MULTICAST_SOCKET_ADDRESS =
        new InetSocketAddress(MULTICAST_ADDRESS, MULTICAST_PORT);

    private final MulticastSocket multicast;
    private final DatagramSocket socket;
    private final CopyOnWriteArrayList<Listener> listeners =
        new CopyOnWriteArrayList<>();

    /**
     * Sole constructor.
     *
     * @throws  IOException     If the underlying {@link MulticastSocket}
     *                          cannot be conditioned.
     */
    public SSDPDiscoveryService() throws IOException {
        super(8);

        multicast = new MulticastSocket(MULTICAST_SOCKET_ADDRESS.getPort());
        multicast.setReuseAddress(true);
        multicast.setLoopbackMode(false);
        multicast.setTimeToLive(4);
        multicast.joinGroup(MULTICAST_SOCKET_ADDRESS.getAddress());

        socket = new DatagramSocket();

        submit(() -> receive(multicast));
        submit(() -> receive(socket));
    }

    /**
     * Method to add a {@link Listener}.
     *
     * @param   listener        The {@link Listener}.
     *
     * @return  {@link.this}
     */
    public SSDPDiscoveryService addListener(Listener listener) {
        if ((! listeners.contains(listener)) && listeners.add(listener)) {
            listener.register(this);
        }

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
        if (listeners.remove(listener)) {
            listener.register(this);
        }

        return this;
    }

    private void fireSendEvent(DatagramSocket socket, SSDPMessage message) {
        listeners.stream().forEach(t -> t.sendEvent(this, socket, message));
    }

    private void fireReceiveEvent(DatagramSocket socket, SSDPMessage message) {
        listeners.stream().forEach(t -> t.receiveEvent(this, socket, message));
    }

    /**
     * Method to queue an {@link SSDPMessage} multicast.
     *
     * @param   delay           Time to delay (in milliseconds) before
     *                          sending.
     * @param   message         The {@link SSDPMessage} to send.
     */
    public void multicast(long delay, SSDPMessage message) {
        send(delay, MULTICAST_SOCKET_ADDRESS, message);
    }

    /**
     * Method to queue an {@link SSDPMessage} for sending.
     *
     * @param   delay           Time to delay (in milliseconds) before
     *                          sending.
     * @param   address         The destination {@link SocketAddress}.
     * @param   message         The {@link SSDPMessage} to send.
     */
    public void send(long delay, SocketAddress address, SSDPMessage message) {
        byte[] bytes = message.toString().getBytes(UTF_8);
        DatagramPacket packet =
            new DatagramPacket(bytes, 0, bytes.length, address);

        schedule(() -> task(message, packet), delay, MILLISECONDS);
    }

    private void task(SSDPMessage message, DatagramPacket packet) {
        try {
            fireSendEvent(socket, message);
            socket.send(packet);
        } catch (IOException exception) {
        }
    }

    private void receive(DatagramSocket socket) {
        try {
            socket.setSoTimeout((int) MILLISECONDS.convert(15, SECONDS));

            for (;;) {
                try {
                    byte[] bytes = new byte[8 * 1024];
                    DatagramPacket packet =
                        new DatagramPacket(bytes, bytes.length);

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
                message = SSDPResponse.from(packet);
            } catch (ParseException exception) {
            }
        }

        if (message == null) {
            try {
                message = SSDPRequest.from(packet);
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
         * Callback when a {@link Listener} is added to a
         * {@link SSDPDiscoveryService}.
         *
         * @param       service         The {@link SSDPDiscoveryService}.
         */
        public void register(SSDPDiscoveryService service);

        /**
         * Callback when a {@link Listener} is removed from a
         * {@link SSDPDiscoveryService}.
         *
         * @param       service         The {@link SSDPDiscoveryService}.
         */
        public void unregister(SSDPDiscoveryService service);

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

    /**
     * {@link SSDPDiscoveryService} {@link SSDPRequest} handler.
     */
    public static abstract class RequestHandler implements Listener {
        private final SSDPRequest.Method method;

        /**
         * Sole constructor.
         *
         * @param       method          The {@link SSDPRequest.Method} to
         *                              handle.
         */
        protected RequestHandler(SSDPRequest.Method method) {
            this.method = Objects.requireNonNull(method);
        }

        public abstract void run(SSDPDiscoveryService service,
                                 DatagramSocket socket, SSDPRequest request);

        @Override
        public void register(SSDPDiscoveryService service) { }

        @Override
        public void unregister(SSDPDiscoveryService service) { }

        @Override
        public void receiveEvent(SSDPDiscoveryService service,
                                 DatagramSocket socket, SSDPMessage message) {
            if (message instanceof SSDPRequest) {
                SSDPRequest request = (SSDPRequest) message;

                if (method.is(request.getMethod())) {
                    service.submit(() -> run(service, socket, request));
                }
            }
        }

        @Override
        public void sendEvent(SSDPDiscoveryService service,
                              DatagramSocket socket, SSDPMessage message) {
        }
    }

    /**
     * {@link SSDPDiscoveryService} {@link SSDPResponse} handler.
     */
    public static abstract class ResponseHandler implements Listener {

        /**
         * Sole constructor.
         */
        protected ResponseHandler() { }

        public abstract void run(SSDPDiscoveryService service,
                                 DatagramSocket socket, SSDPResponse request);

        @Override
        public void register(SSDPDiscoveryService service) { }

        @Override
        public void unregister(SSDPDiscoveryService service) { }

        @Override
        public void receiveEvent(SSDPDiscoveryService service,
                                 DatagramSocket socket, SSDPMessage message) {
            if (message instanceof SSDPResponse) {
                service.submit(() -> run(service, socket, (SSDPResponse) message));
            }
        }

        @Override
        public void sendEvent(SSDPDiscoveryService service,
                              DatagramSocket socket, SSDPMessage message) {
        }
    }
}
