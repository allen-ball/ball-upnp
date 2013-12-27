/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp.ssdp;

import iprotium.io.IOUtil;
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
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class SSDPDiscoveryThread extends Thread {
    private final int interval;
    private final DatagramSocket socket;
    private final CopyOnWriteArrayList<Listener> list =
        new CopyOnWriteArrayList<Listener>();
    private final ConcurrentLinkedQueue<SSDPMessage> queue =
        new ConcurrentLinkedQueue<SSDPMessage>();
    private final SSDPDiscoveryRequest request = new SSDPDiscoveryRequest();

    /**
     * Sole constructor.
     *
     * @param   interval        The minimum interval (in seconds) between
     *                          broadcast messages.
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

    @Override
    public void start() {
        new Thread() {
            { setDaemon(true); }

            @Override
            public void run() {
                for (;;) {
                    queue(request);

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
            IOUtil.close(socket);
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
