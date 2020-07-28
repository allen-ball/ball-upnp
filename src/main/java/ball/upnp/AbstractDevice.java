package ball.upnp;
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
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP}
 * {@link Device}s.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class AbstractDevice implements AnnotatedDevice {
    private Map<URI,URI> notifications = null;

    /**
     * Method to provide {@link Map} of {@code NT} ({@code ST}) to
     * {@code USN} permutations required for {@code NOTIFY("ssdp:alive")}
     * and {@code NOTIFY("ssdp:byebye")} and {@code M-SEARCH("ssdp:all")}
     * responses for {@link.this} {@link Device}.
     *
     * @return  {@link Map} of {@code NT}/{@code USN} permutations.
     */
    @Synchronized
    public Map<URI,URI> notifications() {
        if (notifications == null) {
            notifications = new LinkedHashMap<>();

            if (this instanceof RootDevice) {
                notifications.put(RootDevice.NT, usn(getUDN(), RootDevice.NT));
            }

            notifications.put(getUDN(), getUDN());
            notifications.put(getDeviceType(), usn(getUDN(), getDeviceType()));
        }

        return notifications;
    }

    private URI usn(URI left, URI right) {
        return URI.create(left.toASCIIString() + "::" + right.toASCIIString());
    }

    @Override
    public String toString() { return getDeviceType().toString(); }
}
