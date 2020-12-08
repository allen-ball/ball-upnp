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
import ball.upnp.RootDevice;
import ball.upnp.SSDP;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Stream;
import lombok.ToString;
import org.apache.http.ParseException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * SSDP discovery {@link ScheduledThreadPoolExecutor} implementation.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPDiscoveryService extends ScheduledThreadPoolExecutor {
    private static final String OS =
        Stream.of("os.name", "os.version")
        .map(System::getProperty)
        .map(t -> t.replaceAll("[\\p{Space}]+", EMPTY))
        .collect(joining("/"));
    private static final String UPNP = "UPnP/2.0";

    private final String server;
    private final int bootId = (int) (System.currentTimeMillis() / 1000);
    private final Random random = new Random();
    private final MulticastSocket multicast;
    private final DatagramSocket unicast;
    private final CopyOnWriteArrayList<Listener> listeners =
        new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<RootDevice,ScheduledFuture<?>> advertisers =
        new ConcurrentHashMap<>();

    /**
     * Sole constructor.
     *
     * @param   product         The {@code product/version} {@link String}
     *                          identifying this UPnP application.
     *
     * @throws  IOException     If the underlying {@link MulticastSocket}
     *                          cannot be conditioned.
     */
    public SSDPDiscoveryService(String product) throws IOException {
        super(8);

        server =
            Stream.of(OS, UPNP, product)
            .filter(Objects::nonNull)
            .collect(joining(SPACE));

        random.setSeed(System.currentTimeMillis());

        multicast = new SSDPMulticastSocket();
        /*
         * Bind to an {@link.rfc 4340} ephemeral port.
         */
        DatagramSocket socket = null;
        List<Integer> ports =
            random.ints(49152, 65536).limit(256).boxed().collect(toList());

        for (;;) {
            try {
                socket = new DatagramSocket(ports.remove(0));
                break;
            } catch (SocketException exception) {
                if (ports.isEmpty()) {
                    throw exception;
                } else {
                    continue;
                }
            }
        }

        unicast = socket;

        addListener(new MSEARCH());

        submit(() -> receive(multicast));
        submit(() -> receive(unicast));
    }

    /**
     * {@code SERVER} and {@code USER-AGENT}
     *
     * @return  {@code SERVER} and {@code USER-AGENT}
     */
    public String getUserAgent() { return server; }

    /**
     * {@code BOOTID.UPNP.ORG}
     *
     * @return  {@code bootId}
     */
    public int getBootId() { return bootId; }

    /**
     * {@code NEXTBOOTID.UPNP.ORG}
     *
     * @return  {@code nextBootId}
     */
    public int getNextBootId() { throw new UnsupportedOperationException(); }

    /**
     * {@code SEARCHPORT.UPNP.ORG}
     *
     * @return  Search port.
     */
    public int getSearchPort() { return unicast.getLocalPort(); }

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
     * Method to add a {@link RootDevice} to advertise.
     *
     * @param   device          The {@link RootDevice} to advertise.
     * @param   rate            The rate (in seconds) to repeat
     *                          advertisements.
     *
     * @return  {@link.this}
     */
    public SSDPDiscoveryService advertise(RootDevice device,
                                          int rate) {
        ScheduledFuture<?> future =
            scheduleAtFixedRate(() -> alive(device),
                                advertisers.size(), rate, SECONDS);

        future = advertisers.put(device, future);

        if (future != null) {
            future.cancel(true);
        }

        return this;
    }

    private void alive(RootDevice device) {
        device.notify((nt, usn) -> multicast(new Alive(nt, usn, device)));
    }

    private void byebye(RootDevice device) {
        device.notify((nt, usn) -> multicast(new ByeBye(nt, usn, device)));
    }

    /**
     * Send multicast {@code M-SEARCH} messsage.
     *
     * @param   mx              The {@code MX} header value.
     * @param   st              The {@code ST} header value.
     */
    public void msearch(int mx, URI st) { multicast(0, new MSearch(mx, st)); }

    /**
     * Method to queue an {@link SSDPMessage} for multicast without delay.
     *
     * @param   message         The {@link SSDPMessage} to send.
     */
    public void multicast(SSDPMessage message) {
        send(0, SSDPMulticastSocket.SOCKET_ADDRESS, message);
    }

    /**
     * Method to queue an {@link SSDPMessage} for multicast with delay.
     *
     * @param   delay           Time to delay (in milliseconds) before
     *                          sending.
     * @param   message         The {@link SSDPMessage} to send.
     */
    public void multicast(long delay, SSDPMessage message) {
        send(delay, SSDPMulticastSocket.SOCKET_ADDRESS, message);
    }

    /**
     * Method to queue an {@link SSDPMessage} for sending without delay.
     *
     * @param   address         The destination {@link SocketAddress}.
     * @param   message         The {@link SSDPMessage} to send.
     */
    public void send(SocketAddress address, SSDPMessage message) {
        send(0, address, message);
    }

    /**
     * Method to queue an {@link SSDPMessage} for sending with delay.
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
            fireSendEvent(unicast, message);
            unicast.send(packet);
        } catch (IOException exception) {
        }
    }

    /**
     * Method to queue a {@link List} of {@link SSDPMessage}s for sending
     * with a {@code MX} parameter.  Messages are sent in list order with
     * random delays none greater that {@code MX} seconds.
     *
     * @param   mx              Maximum delay (in seconds) before sending.
     * @param   address         The destination {@link SocketAddress}.
     * @param   messages        The {@link List} of {@link SSDPMessage}s to
     *                          send.
     */
    public void send(int mx,
                     SocketAddress address,
                     List<? extends SSDPMessage> messages) {
        List<Long> delays =
            messages.stream()
            .map(t -> random.nextInt((int) SECONDS.toMillis(mx)))
            .map(t -> SECONDS.toMillis(1) + t)
            .collect(toList());

        delays.sort(Comparator.naturalOrder());

        messages.stream().forEach(t -> send(delays.remove(0), address, t));
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

    @Override
    public void shutdown() {
        advertisers.values().stream().forEach(t -> t.cancel(true));
        advertisers.keySet().stream().forEach(t -> byebye(t));
        advertisers.clear();

        super.shutdown();
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

    @ToString
    private class MSEARCH extends RequestHandler {
        public MSEARCH() { super(SSDPRequest.Method.MSEARCH); }

        @Override
        public void run(SSDPDiscoveryService service,
                        DatagramSocket socket, SSDPRequest request) {
            try {
                if (isHeaderValue(request, SSDPMessage.MAN, "\"ssdp:discover\"")) {
                    int mx = request.getMX();
                    SocketAddress address = request.getSocketAddress();
                    List<SSDPMessage> list = new LinkedList<>();
                    URI st = request.getST();
                    boolean all = SSDPMessage.SSDP_ALL.equals(st);

                    advertisers.keySet()
                        .stream()
                        .forEach(device -> device.notify((nt, usn) -> {
                                    if (SSDP.matches(st, nt)) {
                                        list.add(new MSearch(service, all ? nt : st, usn, device));
                                    }
                                }));

                    service.send(mx, address, list);
                }
            } catch (Exception exception) {
                /* log.error("{}", exception.getMessage(), exception); */
            }
        }

        private boolean isHeaderValue(SSDPRequest request,
                                      String header, String value) {
            return Objects.equals(request.getHeaderValue(header), value);
        }

        private class MSearch extends SSDPResponse {
            public MSearch(SSDPDiscoveryService service,
                           URI st, URI usn, RootDevice device) {
                super(SC_OK, "OK");

                header(CACHE_CONTROL, MAX_AGE + "=" + device.getMaxAge());
                header(DATE, GENERATOR.getCurrentDate());
                header(EXT, (String) null);
                header(LOCATION, device.getLocation());
                header(SERVER, service.getUserAgent());
                header(ST, st);
                header(USN, usn);
                header(BOOTID_UPNP_ORG, service.getBootId());
                header(CONFIGID_UPNP_ORG, device.getConfigId());
                header(SEARCHPORT_UPNP_ORG, service.getSearchPort());
            }
        }
    }

    private class MSearch extends SSDPRequest {
        public MSearch(int mx, URI st) {
            super(Method.MSEARCH);

            header(HOST, SSDPMulticastSocket.SOCKET_ADDRESS);
            header(MAN, "\"ssdp:discover\"");
            header(MX, mx);
            header(ST, st);
            header(USER_AGENT, getUserAgent());
        }
    }

    private class Alive extends SSDPRequest {
        public Alive(URI nt, URI usn, RootDevice device) {
            super(Method.NOTIFY);

            header(HOST, SSDPMulticastSocket.SOCKET_ADDRESS);
            header(CACHE_CONTROL, MAX_AGE + "=" + device.getMaxAge());
            header(NT, nt);
            header(NTS, SSDP_ALIVE);
            header(SERVER, getUserAgent());
            header(USN, usn);
            header(LOCATION, device.getLocation());
            header(BOOTID_UPNP_ORG, getBootId());
            header(CONFIGID_UPNP_ORG, device.getConfigId());
            header(SEARCHPORT_UPNP_ORG, getSearchPort());
        }
    }

    private class ByeBye extends SSDPRequest {
        public ByeBye(URI nt, URI usn, RootDevice device) {
            super(Method.NOTIFY);

            header(HOST, SSDPMulticastSocket.SOCKET_ADDRESS);
            header(NT, nt);
            header(NTS, SSDP_BYEBYE);
            header(USN, usn);
            header(BOOTID_UPNP_ORG, getBootId());
            header(CONFIGID_UPNP_ORG, device.getConfigId());
        }
    }

    private class Update extends SSDPRequest {
        public Update(URI nt, URI usn, RootDevice device) {
            super(Method.NOTIFY);

            header(HOST, SSDPMulticastSocket.SOCKET_ADDRESS);
            header(LOCATION, device.getLocation());
            header(NT, nt);
            header(NTS, SSDP_UPDATE);
            header(USN, usn);
            header(BOOTID_UPNP_ORG, getBootId());
            header(CONFIGID_UPNP_ORG, device.getConfigId());
            header(NEXTBOOTID_UPNP_ORG, getNextBootId());
            header(SEARCHPORT_UPNP_ORG, getSearchPort());
        }
    }
}
