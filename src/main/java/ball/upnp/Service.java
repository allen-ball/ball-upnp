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
import ball.upnp.annotation.XmlNs;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * {@link.uri http://www.upnp.org/ UPnP} service interface.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@XmlNs("urn:schemas-upnp-org:service-1-0")
public interface Service extends Description, SSDP {

    /**
     * Method to get the {@link Device} hosting {@link.this}
     * {@link Service}.
     *
     * @return  The {@link Device}.
     */
    public Device getDevice();

    /**
     * Method to get the URN ({@link URI}) describing {@link.this}
     * {@link Service}'s service type.
     *
     * @return  The service type.
     */
    public URI getServiceType();

    /**
     * Method to get the URN ({@link URI}) describing {@link.this}
     * {@link Service}'s service ID.
     *
     * @return  The service type.
     */
    public URI getServiceId();

    /**
     * Method to get {@link.this} {@link Service}'s description (SCPD)
     * {@code URL} (as an {@link URI}).
     *
     * @return  The presentation {@link URI}.
     */
    public URI getSCPDURL();

    /**
     * Method to get {@link.this} {@link Service}'s control {@code URL} (as
     * an {@link URI}).
     *
     * @return  The presentation {@link URI}.
     */
    public URI getControlURL();

    /**
     * Method to get {@link.this} {@link Service}'s event subscription
     * {@code URL} (as an {@link URI}).
     *
     * @return  The presentation {@link URI}.
     */
    public URI getEventSubURL();

    /**
     * Method to get {@link.this} {@link Service}'s {@link Action}s.
     *
     * @return  The {@link List} of {@link Action}s.
     */
    public List<? extends Action> getActionList();

    /**
     * Method to get {@link.this} {@link Service}'s {@link StateVariable}s.
     *
     * @return  The {@link List} of {@link StateVariable}s.
     */
    public List<? extends StateVariable> getServiceStateTable();

    @Override
    default Map<URI,Set<URI>> getUSNMap() {
        LinkedHashMap<URI,Set<URI>> map = new LinkedHashMap<>();
        Function<URI,Set<URI>> mapper = k -> new LinkedHashSet<>();

        map.computeIfAbsent(getUSN(getServiceType()), mapper)
            .add(getServiceType());

        return map;
    }

    @Override
    default URI getUSN(URI urn) {
        return getDevice().getUSN(Objects.requireNonNull(urn));
    }
}
