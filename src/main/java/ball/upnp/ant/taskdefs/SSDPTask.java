package ball.upnp.ant.taskdefs;
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
import ball.swing.table.ArrayListTableModel;
import ball.upnp.ssdp.SSDPDiscoveryCache;
import ball.upnp.ssdp.SSDPDiscoveryService;
import ball.upnp.ssdp.SSDPMessage;
import ball.upnp.ssdp.SSDPRequest;
import ball.util.ant.taskdefs.AnnotatedAntTask;
import ball.util.ant.taskdefs.AntTask;
import ball.util.ant.taskdefs.ClasspathDelegateAntTask;
import ball.util.ant.taskdefs.ConfigurableAntTask;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Synchronized;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.http.HttpHeaders;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.ClasspathUtils;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract {@link.uri http://ant.apache.org/ Ant} {@link Task} base class
 * for SSDP tasks.
 *
 * {@ant.task}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class SSDPTask extends Task
                               implements AnnotatedAntTask,
                                          ClasspathDelegateAntTask,
                                          ConfigurableAntTask,
                                          SSDPDiscoveryService.Listener {
    @Getter @Setter @Accessors(chain = true, fluent = true)
    private ClasspathUtils.Delegate delegate = null;

    @Override
    public void init() throws BuildException {
        super.init();
        ClasspathDelegateAntTask.super.init();
        ConfigurableAntTask.super.init();
    }

    @Override
    public void execute() throws BuildException {
        super.execute();
        AnnotatedAntTask.super.execute();
    }

    @Override
    public void register(SSDPDiscoveryService service) { }

    @Override
    public void unregister(SSDPDiscoveryService service) { }

    @Synchronized
    @Override
    public void sendEvent(SSDPDiscoveryService service,
                          DatagramSocket socket, SSDPMessage message) {
        // log(toString(socket) + " --> " /* + message.getSocketAddress() */);
        log("--- Outgoing ---");
        log(String.valueOf(message));
    }

    @Synchronized
    @Override
    public void receiveEvent(SSDPDiscoveryService service,
                             DatagramSocket socket, SSDPMessage message) {
        // log(toString(socket) + " <-- " /* + message.getSocketAddress() */);
        log("--- Incoming ---");
        log(String.valueOf(message));
    }

    private String toString(DatagramSocket socket) {
        return toString(socket.getLocalSocketAddress());
    }

    private String toString(SocketAddress address) {
        return toString((InetSocketAddress) address);
    }

    private String toString(InetSocketAddress address) {
        return String.format("%s:%d",
                             address.getAddress().getHostAddress(),
                             address.getPort());
    }

    /**
     * {@link.uri http://ant.apache.org/ Ant} {@link Task} to run SSDP
     * discovery.
     *
     * {@ant.task}
     */
    @AntTask("ssdp-discover")
    @NoArgsConstructor @ToString
    public static class Discover extends SSDPTask {
        @Getter @Setter
        private int timeout = 180;

        @Override
        public void execute() throws BuildException {
            super.execute();

            ClassLoader loader =
                Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());

                SSDPDiscoveryCache cache = new SSDPDiscoveryCache();
                SSDPDiscoveryService service =
                    new SSDPDiscoveryService()
                    .addListener(this)
                    .addListener(cache);

                service.awaitTermination(getTimeout(), TimeUnit.SECONDS);

                log(new TableModelImpl(cache));
            } catch (BuildException exception) {
                throw exception;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                throw new BuildException(throwable);
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
        }

        private class TableModelImpl extends ArrayListTableModel<SSDPDiscoveryCache.Value> {
            private static final long serialVersionUID = -7515042633146116666L;

            public TableModelImpl(SSDPDiscoveryCache cache) {
                super(cache.values(),
                      SSDPMessage.USN,
                      HttpHeaders.EXPIRES, HttpHeaders.LOCATION);
            }

            @Override
            protected Object getValueAt(SSDPDiscoveryCache.Value row, int x) {
                Object object = null;

                switch (x) {
                default:
                case 0:
                    object = row.getSSDPMessage().getUSN();
                    break;

                case 1:
                    object =
                        Duration.ofMillis(row.getExpiration()
                                          - System.currentTimeMillis())
                        .toString();
                    break;

                case 2:
                    object = row.getSSDPMessage().getLocation();
                    break;
                }

                return object;
            }
        }
    }

    /**
     * {@link.uri http://ant.apache.org/ Ant} {@link Task} listen on the
     * SSDP UDP port.
     *
     * {@ant.task}
     */
    @AntTask("ssdp-listen")
    @NoArgsConstructor @ToString
    public static class Listen extends SSDPTask {
        @Override
        public void execute() throws BuildException {
            super.execute();

            ClassLoader loader =
                Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());

                SSDPDiscoveryService service =
                    new SSDPDiscoveryService()
                    .addListener(this);

                service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            } catch (BuildException exception) {
                throw exception;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                throw new BuildException(throwable);
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
        }
    }

    /**
     * {@link.uri http://ant.apache.org/ Ant} {@link Task} to send an
     * {@code M-SEARCH} command and then listen on the SSDP UDP port.
     *
     * {@ant.task}
     */
    @AntTask("ssdp-m-search")
    @NoArgsConstructor @ToString
    public static class MSearch extends SSDPTask {
        @Getter @Setter
        private int mx = 5;
        @Getter @Setter
        private URI st = SSDPMessage.SSDP_ALL;

        @Override
        public void execute() throws BuildException {
            super.execute();

            ClassLoader loader =
                Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());

                SSDPDiscoveryService service =
                    new SSDPDiscoveryService()
                    .addListener(this);

                service.multicast(0, SSDPRequest.msearch(getMx(), getSt()));
                service.awaitTermination(getMx(), TimeUnit.SECONDS);
            } catch (BuildException exception) {
                throw exception;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                throw new BuildException(throwable);
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
        }
    }
}
