package ball.upnp;
/*-
 * ##########################################################################
 * UPnP/SSDP Implementation Classes
 * %%
 * Copyright (C) 2013 - 2022 Allen D. Ball
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
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * {@link.uri http://www.upnp.org/ UPnP} {@link RootDevice} interface.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public interface RootDevice extends Device {

    /**
     * {@code upnp:rootdevice}
     */
    public static final URI NT = URI.create("upnp:rootdevice");

    /**
     * {@code CONFIGID.UPNP.ORG}
     *
     * @return  {@code configID}
     */
    default int getConfigId() { return 1; }

    /**
     * {@code CACHE-CONTROL: MAX-AGE}
     *
     * @return  {@code MAX-AGE}
     */
    default int getMaxAge() { return 1800; }

    /**
     * {@code LOCATION}
     *
     * @return  Description {@link URI}
     */
    public URI getLocation();

    /**
     * Method to get {@link.this} {@link RootDevice}'s presentation
     * {@code URL} (as an {@link URI}).
     *
     * @return  The presentation {@link URI}.
     */
    public URI getPresentationURL();

    /**
     * Method to invoke {@link BiConsumer consumer} for every {@link URI NT}
     * / {@link URI USN} combinations representing the {@link RootDevice}
     * with embedded {@link Service}s and {@link Device}s.
     *
     * @param   consumer        The {@link BiConsumer}.
     */
    default void notify(BiConsumer<URI,URI> consumer) {
        getUSNMap().forEach((usn, v) -> v.forEach(nt -> consumer.accept(nt, usn)));
    }
}
