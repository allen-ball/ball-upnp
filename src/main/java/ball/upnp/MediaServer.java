/*
 * $Id$
 *
 * Copyright 2013 - 2016 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import ball.tomcat.EmbeddedTomcat;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.plist.XMLPropertyListConfiguration;

import static ball.io.Directory.USER_HOME;

/**
 * {@link.uri http://www.upnp.org/ UPnP} {@link MediaServer}
 * {@link Device} which implements {@link ContentDirectory} and
 * {@link ConnectionManager} {@link Service}s.
 *
 * <table>
 *   <tr><td>{@link #TYPE}:</td><td>{@value #TYPE}</td></tr>
 * </table>
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class MediaServer extends Device {
    public static final String TYPE =
        "urn:schemas-upnp-org:device:MediaServer:4";

    private final RootDevice root;
    private final ContentDirectory directory;
    private final ConnectionManager manager;
    private final List<? extends Service> list;

    /**
     * Sole constructor.
     */
    public MediaServer() {
        super(URI.create(TYPE));

        root = new RootDevice(this);
        directory = new ContentDirectory(this);
        manager = new ConnectionManager(this);

        list = Arrays.asList(root, directory, manager);
    }

    @Override
    public List<? extends Service> getServiceList() { return list; }

    private static Class<?> MAIN_CLASS = MediaServer.class;
    private static Options OPTIONS = new Options();

    /**
     * Static {@code main} entry point for a {@link MediaServer}.
     *
     * @param   argv            Array of command arguments.
     */
    public static void main(String[] argv) {
        try {
            CommandLine line = new DefaultParser().parse(OPTIONS, argv);

            if (! line.getArgList().isEmpty()) {
                throw new IllegalArgumentException(String.valueOf(line.getArgList()));
            }

            File file =
                USER_HOME.getChildFile("Library", "Preferences",
                                       MAIN_CLASS.getName() + ".plist");
            XMLPropertyListConfiguration plist =
                new XMLPropertyListConfiguration(file);
            CompositeConfiguration composite = new CompositeConfiguration();

            composite.addConfiguration(new SystemConfiguration());
            composite.addConfiguration(plist);

            EmbeddedTomcat tomcat = new EmbeddedTomcat();

            tomcat.setSilent(false);
            tomcat.getServer().setParentClassLoader(MAIN_CLASS.getClassLoader());
            tomcat.setPort(8080);

            new SSDP().configure(tomcat);
            new MediaServer().configure(tomcat);

            tomcat.start();
            tomcat.getServer().await();
            System.exit(0);
        } catch (Throwable throwable) {
            new HelpFormatter().printHelp(MAIN_CLASS.getName(), OPTIONS);
            throwable.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
