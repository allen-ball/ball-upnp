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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.http.ParseException;

/**
 * SSDP discovery {@link Thread} implementation.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPDiscoveryThread extends Thread {
    protected final int interval;
    private final DatagramSocket socket;
    private final CopyOnWriteArrayList<Listener> list =
        new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedQueue<SSDPMessage> queue =
        new ConcurrentLinkedQueue<>();

    /**
     * Sole constructor.
     *
     * @param   interval        The minimum interval (in seconds) between
     *                          broadcast messages.
     *
     * @throws  SocketException
     *                          If the underlying {@link DatagramSocket}
     *                          cannot be conditioned.
     */
    public SSDPDiscoveryThread(int interval) throws SocketException {
        super();

        if (interval > 0) {
            this.interval = interval;
        } else {
            throw new IllegalArgumentException("interval=" + interval);
        }

        setDaemon(true);
        setName(getClass().getName());

        socket = new DatagramSocket();
        socket.setBroadcast(true);
        socket.setReuseAddress(true);
        socket.setSoTimeout((interval * 1000) / 2);
    }

    /**
     * Method to add a {@link Listener}.
     *
     * @param  listener         The {@link Listener}.
     */
    public void addListener(Listener listener) { list.add(listener); }

    /**
     * Method to remove a {@link Listener}.
     *
     * @param  listener         The {@link Listener}.
     */
    public void removeListener(Listener listener) { list.remove(listener); }

    /**
     * Method to queue a {@link SSDPMessage} for transmission.
     *
     * @param  message          The {@link SSDPMessage}.
     */
    public void queue(SSDPMessage message) {
        if (message != null && (! queue.contains(message))) {
            queue.add(message);
        }
    }

    /**
     * Callback periodically made to {@link #queue(SSDPMessage)} a
     * {@link SSDPDiscoveryRequest} available to be intercepted (overridden)
     * by subclass implementations.
     */
    protected void ping() { queue(new SSDPDiscoveryRequest()); }

    @Override
    public void start() {
        new Thread() {
            { setDaemon(true); }

            @Override
            public void run() {
                for (;;) {
                    ping();

                    try {
                        sleep(interval * 1000);
                    } catch (InterruptedException exception) {
                    }
                }
            }
        }.start();

        super.start();
    }

    @Override
    public void run() {
        try {
            byte[] bytes = new byte[8 * 1024];
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);

            for (;;) {
                SSDPMessage message = null;

                while ((message = queue.poll()) != null) {
                    for (Listener listener : list) {
                        listener.sendEvent(this, message);
                    }

                    socket.send(message.toDatagramPacket());
                }

                try {
                    packet.setData(bytes);
                    socket.receive(packet);

                    message = parse(packet);

                    if (message != null) {
                        for (Listener listener : list) {
                            listener.receiveEvent(this, message);
                        }
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
     * {@link SSDPDiscoveryThread} listener interface definition.
     */
    public interface Listener {

        /**
         * Callback made just before sending a {@link SSDPMessage}.
         *
         * @param       thread          The {@link SSDPDiscoveryThread}.
         * @param       message         The {@link SSDPMessage}.
         */
        public void sendEvent(SSDPDiscoveryThread thread, SSDPMessage message);

        /**
         * Callback made after receiving a {@link SSDPMessage}.
         *
         * @param       thread          The {@link SSDPDiscoveryThread}.
         * @param       message         The {@link SSDPMessage}.
         */
        public void receiveEvent(SSDPDiscoveryThread thread,
                                 SSDPMessage message);
    }
}
