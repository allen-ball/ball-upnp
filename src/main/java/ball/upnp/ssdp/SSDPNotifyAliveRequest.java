package ball.upnp.ssdp;
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

/**
 * Alive {@link SSDPNotifyRequest}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPNotifyAliveRequest extends SSDPNotifyRequest {

    /**
     * Sole constructor.
     *
     * @param   nt              The {@code NT} header value.
     * @param   usn             The {@code USN} header value.
     * @param   location        The {@code LOCATION} header value.
     */
    public SSDPNotifyAliveRequest(URI nt, URI usn, URI location) {
        super();

        addHeader(NT, nt.toASCIIString());
        addHeader(NTS, SSDP_ALIVE);
        addHeader(USN, usn.toASCIIString());
        addHeader(LOCATION, location.toASCIIString());
        addHeader(CACHE_CONTROL, MAX_AGE + "=" + String.valueOf(1800));
    }
}
