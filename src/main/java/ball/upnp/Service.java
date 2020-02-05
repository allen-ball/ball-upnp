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
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP} services.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@XmlRootElement(name = "scpd", namespace = "urn:schemas-upnp-org:service-1-0")
@RequiredArgsConstructor(access = PROTECTED)
public abstract class Service implements AnnotatedService, XmlDocument {
    @NonNull @Getter
    private final Device device;
    @Getter
    private final List<Action> actionList = new LinkedList<>();
    @Getter
    private final List<StateVariable> serviceStateTable = new LinkedList<>();

    /**
     * Method to get the USN {@link URI}.  The {@link URI} is calculated by
     * combining the {@link Device#getUDN()} and {@link #getServiceId()}.
     *
     * @return  The USN {@link URI}.
     */
    public URI getUSN() {
        return URI.create(getDevice().getUDN() + "::" + getServiceId());
    }

    @Override
    public String toString() { return getServiceType().toString(); }
}
