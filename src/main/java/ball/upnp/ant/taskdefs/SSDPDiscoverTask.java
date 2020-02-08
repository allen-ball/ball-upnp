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
import ball.upnp.ssdp.SSDPDiscoveryRequest;
import ball.upnp.ssdp.SSDPDiscoveryThread;
import ball.upnp.ssdp.SSDPMessage;
import ball.util.ant.taskdefs.AnnotatedAntTask;
import ball.util.ant.taskdefs.AntTask;
import ball.util.ant.taskdefs.ClasspathDelegateAntTask;
import ball.util.ant.taskdefs.ConfigurableAntTask;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.http.HttpHeaders;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.ClasspathUtils;

/**
 * {@link.uri http://ant.apache.org/ Ant} {@link Task} to run SSDP
 * discovery.
 *
 * {@ant.task}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@AntTask("ssdp-discover")
@NoArgsConstructor @ToString
public class SSDPDiscoverTask extends Task
                              implements AnnotatedAntTask,
                                         ClasspathDelegateAntTask,
                                         ConfigurableAntTask,
                                         SSDPDiscoveryThread.Listener {
    @Getter @Setter @Accessors(chain = true, fluent = true)
    private ClasspathUtils.Delegate delegate = null;
    @Getter @Setter
    private int timeout = 60;

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

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(getClassLoader());

            SSDPDiscoveryCache cache = new SSDPDiscoveryCache();
            SSDPDiscoveryThread thread = new SSDPDiscoveryThread(getTimeout());

            thread.addListener(this);
            thread.addListener(cache);
            thread.start();

            Thread.sleep(getTimeout() * 1000);

            log(new TableModelImpl(cache.values()));
        } catch (BuildException exception) {
            throw exception;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new BuildException(throwable);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    @Override
    public void sendEvent(SSDPDiscoveryThread thread, SSDPMessage message) {
        log(String.valueOf(message));
    }

    @Override
    public void receiveEvent(SSDPDiscoveryThread thread, SSDPMessage message) {
        log(String.valueOf(message));
    }

    private class TableModelImpl
                  extends ArrayListTableModel<SSDPDiscoveryCache.Value> {
        private static final long serialVersionUID = 5540353564214745627L;

        public TableModelImpl(Iterable<SSDPDiscoveryCache.Value> iterable) {
            super(iterable,
                  SSDPMessage.USN, SSDPMessage.ST,
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
                object = row.getSSDPMessage().getST();
                break;

            case 2:
                object = row.getExpiration();
                break;

            case 3:
                object = row.getSSDPMessage().getLocation();
                break;
            }

            return object;
        }
    }
}
