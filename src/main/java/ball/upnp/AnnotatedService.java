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
import ball.upnp.annotation.ServiceId;
import ball.upnp.annotation.ServiceType;
import java.net.URI;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * {@link Service} support for {@link ServiceType} and related annotations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface AnnotatedService extends Service {
    @Override
    default URI getServiceType() {
        ServiceType annotation =
            AnnotationUtils.findAnnotation(getClass(), ServiceType.class);

        return (annotation != null) ? URI.create(annotation.value()) : null;
    }

    @Override
    default URI getServiceId() {
        ServiceId annotation =
            AnnotationUtils.findAnnotation(getClass(), ServiceId.class);

        return (annotation != null) ? URI.create(annotation.value()) : getServiceType();
    }
}
