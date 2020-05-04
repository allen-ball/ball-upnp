package ball.upnp.annotation.processing;
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
import ball.annotation.ServiceProviderFor;
import ball.annotation.processing.AnnotatedProcessor;
import ball.annotation.processing.For;
import ball.upnp.AnnotatedDevice;
import ball.upnp.annotation.DeviceType;
import java.net.URI;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static javax.tools.Diagnostic.Kind.ERROR;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * {@link DeviceType} annotation {@link Processor}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Processor.class })
@For({ DeviceType.class })
@NoArgsConstructor @ToString
public class DeviceTypeProcessor extends AnnotatedProcessor {
    @Override
    protected void process(RoundEnvironment roundEnv,
                           TypeElement annotation, Element element) {
        super.process(roundEnv, annotation, element);

        String string = element.getAnnotation(DeviceType.class).value();

        if (isNotEmpty(string)) {
            if (isAssignable(element.asType(), AnnotatedDevice.class)) {
                try {
                    new URI(string);
                } catch (Exception exception) {
                    print(ERROR, element,
                          "%s annotated with @%s but cannot convert '%s' to %s",
                          element.getKind(), annotation.getSimpleName(),
                          string, URI.class.getName());
                }
            }
        } else {
            print(ERROR, element,
                  "%s annotated with @%s but does not specify value()",
                  element.getKind(), annotation.getSimpleName());
        }
    }
}
