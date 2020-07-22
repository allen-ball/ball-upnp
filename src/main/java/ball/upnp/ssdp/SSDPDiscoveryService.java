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
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.http.ParseException;

import static ball.upnp.ssdp.SSDPMessage.MULTICAST_SOCKET_ADDRESS;
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

    private final MulticastSocket socket;
    private final CopyOnWriteArrayList<Listener> list =
        new CopyOnWriteArrayList<>();

    /**
     * Sole constructor.
     *
     * @throws  IOException     If the underlying {@link MulticastSocket}
     *                          cannot be conditioned.
     */
    public SSDPDiscoveryService() throws IOException {
        super(SIZE);

        socket = new MulticastSocket(MULTICAST_SOCKET_ADDRESS.getPort());
        socket.setLoopbackMode(false);
        socket.setTimeToLive(255);
        socket.joinGroup(MULTICAST_SOCKET_ADDRESS.getAddress());

        submit(() -> receive());
    }

    /**
     * Method to add a {@link Listener}.
     *
     * @param   listener        The {@link Listener}.
     *
     * @return  {@link.this}
     */
    public SSDPDiscoveryService addListener(Listener listener) {
        list.add(listener);

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
        list.remove(listener);

        return this;
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
            scheduleAtFixedRate(() -> send(new SSDPDiscoveryRequest()),
                                0, interval, TimeUnit.SECONDS);
        }

        return this;
    }

    /**
     * Method to send an {@link SSDPMessage} on the
     * {@link SSDPMessage#MULTICAST_SOCKET_ADDRESS}.
     *
     * @param   message         The {@link SSDPMessage} to send.
     */
    public void send(SSDPMessage message) {
        try {
            list.stream()
                .forEach(t -> t.sendEvent(this, message));
            socket.send(message.toDatagramPacket());
        } catch (IOException exception) {
        }
    }

    private void receive() {
        try {
            for (;;) {
                byte[] bytes = new byte[8 * 1024];
                DatagramPacket packet =
                    new DatagramPacket(bytes, bytes.length);

                try {
                    socket.receive(packet);

                    SSDPMessage message = parse(packet);

                    if (message != null) {
                        list.stream()
                            .forEach(t -> t.receiveEvent(this, message));
                    }
                } catch (SocketTimeoutException exception) {
                }
            }
        } catch (IOException exception) {
        } finally {
            if (socket != null) {
                socket.close();
            }
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
         * @param       message         The {@link SSDPMessage}.
         */
        public void sendEvent(SSDPDiscoveryService service,
                              SSDPMessage message);

        /**
         * Callback made after receiving a {@link SSDPMessage}.
         *
         * @param       service         The {@link SSDPDiscoveryService}.
         * @param       message         The {@link SSDPMessage}.
         */
        public void receiveEvent(SSDPDiscoveryService service,
                                 SSDPMessage message);
    }
}
