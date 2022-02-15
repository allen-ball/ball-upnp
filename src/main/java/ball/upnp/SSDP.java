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
import java.util.function.Predicate;

import static ball.upnp.ssdp.SSDPMessage.SSDP_ALL;

/**
 * SSDP "discoverable" marker interface.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public interface SSDP {

    /**
     * Method to provide the {@link Map} of {@code USN} to {@code NT}
     * ({@code ST}) permutations required for {@code NOTIFY("ssdp:alive")}
     * and {@code NOTIFY("ssdp:byebye")} requests and
     * {@code M-SEARCH("ssdp:all")} responses for {@link.this}
     * {@link Device} or {@link Service}.
     *
     * @return  {@link Map} of {@code USN}/{@code NT} permutations.
     */
    public Map<URI,Set<URI>> getUSNMap();

    /**
     * Method to get the {@code USN} for this {@link Device}
     * {@code UDN}.
     *
     * @param   urn             The {@code URN} {@link URI}.
     *
     * @return  The {@code USN} {@link URI}.
     */
    public URI getUSN(URI urn);

    /**
     * Method to test if a {@code NT} satisfies an {@code ST}.
     *
     * @param   st              The {@code ST} header value.
     * @param   nt              The {@code NT} header value.
     *
     * @return  {@code true} if {@code nt} satisfies {@code st};
     *          {@code false} otherwise.
     */
    public static boolean matches(URI st, URI nt) {
        boolean matches = SSDP_ALL.equals(st) || st.toString().equalsIgnoreCase(nt.toString());

        if (! matches) {
            try {
                if (! matches) {
                    if (st.getScheme().equalsIgnoreCase("uuid")) {
                        matches |= st.toString().equalsIgnoreCase(nt.toString());
                    }
                }

                if (! matches) {
                    if (st.getScheme().equalsIgnoreCase("urn")) {
                        /*
                         * TBD: Create a real SSDP URN parser.
                         */
                        String string = st.toString().toUpperCase();
                        int index = string.lastIndexOf(":");

                        if (index != -1) {
                            index += 1;

                            String prefix = string.substring(0, index);
                            String stVersion = string.replace(prefix, "");
                            String ntVersion = nt.toString().toUpperCase().replace(prefix, "");

                            if (stVersion.matches("[0-9]+") && ntVersion.matches("[0-9]+")) {
                                matches |= Integer.decode(stVersion) <= Integer.decode(ntVersion);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
            }
        }

        return matches;
    }

    /**
     * Method to provide a {@link Predicate} that implements
     * {@link #matches(URI,URI)}
     *
     * @param   st              The {@code ST} header value.
     *
     * @return  {@link Predicate} to test if {@code nt} against {@code st}.
     */
    public static Predicate<URI> matches(URI st) {
        return t -> matches(st, t);
    }
}
