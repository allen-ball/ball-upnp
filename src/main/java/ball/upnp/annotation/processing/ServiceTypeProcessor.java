package ball.upnp.annotation.processing;
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
import ball.annotation.ServiceProviderFor;
import ball.annotation.processing.AnnotatedProcessor;
import ball.annotation.processing.For;
import ball.upnp.annotation.ServiceType;
import javax.annotation.processing.Processor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link ServiceType} annotation {@link Processor}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Processor.class })
@For({ ServiceType.class })
@NoArgsConstructor @ToString
public class ServiceTypeProcessor extends AnnotatedProcessor {
}
