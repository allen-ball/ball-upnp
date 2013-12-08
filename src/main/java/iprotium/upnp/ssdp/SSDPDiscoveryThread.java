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
import java.util.ArrayList;
import org.apache.http.ParseException;

/**
 * SSDP discovery {@link Thread} implementation.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class SSDPDiscoveryThread extends Thread {
    private final ArrayList<Listener> list = new ArrayList<Listener>();
    private final int interval;
    private final DatagramSocket socket;

    /**
     * Sole constructor.
     *
     * @param   interval
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
        socket.setSoTimeout(interval * 1000);
    }

    /**
     * Method to add a {@link Listener}.
     *
     * @param  listener         The {@link Listener}.
     */
    public void addListener(Listener listener) {
        synchronized (this) {
            list.add(listener);
        }
    }

    /**
     * Method to add a {@link Listener}.
     *
     * @param  listener         The {@link Listener}.
     */
    public void removeListener(Listener listener) {
        synchronized (this) {
            list.remove(listener);
        }
    }

    @Override
    public void run() {
        try {
            byte[] bytes = new byte[8 * 1024];
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);

            for (;;) {
                SSDPDiscoveryRequest request = new SSDPDiscoveryRequest();

                for (Listener listener : list) {
                    listener.sendEvent(request);
                }

                socket.send(request.toDatagramPacket());

                for (;;) {
                    try {
                        packet.setData(bytes);

                        socket.receive(packet);

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

                        if (message != null) {
                            for (Listener listener : list) {
                                listener.receiveEvent(message);
                            }
                        }
                    } catch (SocketTimeoutException exception) {
                        break;
                    }
                }
            }
        } catch (IOException exception) {
        } finally {
            IOUtil.close(socket);
        }
    }

    /**
     * {@link SSDPDiscoveryThread} listener interface definition.
     */
    public interface Listener {

        /**
         * Callback made just before sending a {@link SSDPDiscoveryRequest}.
         *
         * @param       message         The {@link SSDPMessage}.
         */
        public void sendEvent(SSDPMessage message);

        /**
         * Callback made after receiving a {@link SSDPMessage}.
         *
         * @param       message         The {@link SSDPMessage}.
         */
        public void receiveEvent(SSDPMessage message);
    }
}
