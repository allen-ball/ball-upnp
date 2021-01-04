package ball.upnp;
/*-
 * ##########################################################################
 * UPnP/SSDP Implementation Classes
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2013 - 2021 Allen D. Ball
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
import org.springframework.core.annotation.AnnotationUtils;

/**
 * {@link Description} support for {@link XmlNs} annotation.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface Description {

    /**
     * Method to get the {@link XmlNs}'s name.
     *
     * @return  The name.
     */
    default String getXmlns() {
        XmlNs annotation =
            AnnotationUtils.findAnnotation(getClass(), XmlNs.class);

        return (annotation != null) ? annotation.value() : null;
    }
}
