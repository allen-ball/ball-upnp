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
import org.apache.http.HttpHeaders;

/**
 * SSDP discovery ({@value #METHOD}) {@link SSDPRequest}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPDiscoveryRequest extends SSDPRequest {

    /**
     * {@link SSDPRequest} method name ({@value #METHOD})
     */
    public static final String METHOD = "M-SEARCH";

    /**
     * Sole constructor.
     */
    public SSDPDiscoveryRequest() {
        super(METHOD);

        addHeader(HttpHeaders.HOST.toUpperCase(), toString(ADDRESS));
        addHeader(MAN, "\"ssdp:discover\"");
        addHeader(MX, String.valueOf(120));
        addHeader(ST, "ssdp:all");
    }
}
