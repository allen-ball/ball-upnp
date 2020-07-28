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
    private Map<URI,URI> map = null;

    @Synchronized
    @Override
    public Map<URI,URI> getUSNs() {
        if (map == null) {
            map = new LinkedHashMap<>();

            if (this instanceof RootDevice) {
                map.put(RootDevice.NT, usn(getUDN(), RootDevice.NT));
            }

            map.put(getUDN(), getUDN());
            map.put(getDeviceType(), usn(getUDN(), getDeviceType()));

            if (this instanceof RootDevice) {
                getServiceList().stream()
                    .forEach(t -> map.put(t.getServiceType(),
                                          usn(getUDN(), t.getServiceType())));
                getDeviceList().stream()
                    .forEach(t -> map.putAll(t.getUSNs()));
            }
        }

        return map;
    }

    private URI usn(URI left, URI right) {
        return URI.create(left.toASCIIString() + "::" + right.toASCIIString());
    }

    @Override
    public String toString() { return getDeviceType().toString(); }
}
