/*
 * $Id$
 *
 * Copyright 2013, 2014 Allen D. Ball.  All rights reserved.
 */
package ball.upnp.ant.taskdefs;

import ball.annotation.AntTask;
import ball.text.ArrayListTableModel;
import ball.text.TextTable;
import ball.upnp.ssdp.SSDPDiscoveryCache;
import ball.upnp.ssdp.SSDPDiscoveryRequest;
import ball.upnp.ssdp.SSDPDiscoveryThread;
import ball.upnp.ssdp.SSDPMessage;
import ball.util.ant.taskdefs.AbstractClasspathTask;
import org.apache.http.HttpHeaders;
import org.apache.tools.ant.BuildException;

/**
 * {@link.uri http://ant.apache.org/ Ant} {@link org.apache.tools.ant.Task}
 * to run SSDP discovery.
 *
 * {@bean-info}
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
@AntTask("ssdp-discover")
public class SSDPDiscoverTask extends AbstractClasspathTask
                              implements SSDPDiscoveryThread.Listener {
    private int timeout = 60;

    /**
     * Sole constructor.
     */
    public SSDPDiscoverTask() { super(); }

    protected int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    @Override
    public void execute() throws BuildException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(getClassLoader());

            SSDPDiscoveryCache cache = new SSDPDiscoveryCache();
            SSDPDiscoveryThread thread = new SSDPDiscoveryThread(getTimeout());

            thread.addListener(this);
            thread.addListener(cache);
            thread.start();

            Thread.sleep(getTimeout() * 1000);

            log(new TextTable(new TableModelImpl(cache.values())));
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
