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
import java.util.List;
import java.util.UUID;

/**
 * {@link.uri http://www.upnp.org/ UPnP} device interface.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface Device extends Templated {

    /**
     * Method to get the URN ({@link URI}) describing {@link.this}
     * {@link Device}'s device type.
     *
     * @return  The service type.
     */
    public URI getDeviceType();

    /**
     * Method to get {@link.this} {@link Device}'s {@link UUID}.
     *
     * @return  The {@link UUID}.
     */
    public UUID getUUID();

    /**
     * Method to get {@link.this} {@link Device}'s {@link Service}s.
     *
     * @return  The {@link List} of {@link Service}s.
     */
    public List<Service> getServiceList();

    /**
     * Method to get {@link.this} {@link Device}'s {@link Device}s.
     *
     * @return  The {@link List} of {@link Device}s.
     */
    public List<Device> getDeviceList();

    /**
     * Method to get {@link.this} {@link Device}'s {@link Icon}s.
     *
     * @return  The {@link List} of {@link Icon}s.
     */
    public List<Icon> getIconList();

    /**
     * Method to get {@link.this} {@link Device}'s UDN.
     *
     * @return  The UDN {@link URI}.
     */
    default URI getUDN() {
        return URI.create("uuid:" + getUUID().toString().toUpperCase());
    }
}
