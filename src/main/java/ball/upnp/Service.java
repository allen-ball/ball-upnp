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

/**
 * {@link.uri http://www.upnp.org/ UPnP} service interface.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface Service extends Templated {

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

    public URI getSCPDURL();
    public URI getControlURL();
    public URI getEventSubURL();

    /**
     * Method to get {@link.this} {@link Service}'s {@link Action}s.
     *
     * @return  The {@link List} of {@link Action}s.
     */
    public List<Action> getActionList();

    /**
     * Method to get {@link.this} {@link Service}'s {@link StateVariable}s.
     *
     * @return  The {@link List} of {@link StateVariable}s.
     */
    public List<StateVariable> getServiceStateTable();

    /**
     * Method to get the USN {@link URI}.  The {@link URI} is calculated by
     * combining the {@link Device#getUDN()} and {@link #getServiceId()}.
     *
     * @return  The USN {@link URI}.
     */
    default URI getUSN() {
        return URI.create(getDevice().getUDN() + "::" + getServiceId());
    }
}
