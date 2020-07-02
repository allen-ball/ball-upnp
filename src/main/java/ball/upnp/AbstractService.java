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
import ball.upnp.annotation.Template;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP}
 * {@link Service}s.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Template("urn:schemas-upnp-org:service-1-0")
@NoArgsConstructor(access = PROTECTED)
public abstract class AbstractService implements AnnotatedService {
    @Getter
    private final List<? extends Action> actionList = new LinkedList<>();
    @Getter
    private final List<? extends StateVariable> serviceStateTable =
        new LinkedList<>();

    @Override
    public String toString() { return getServiceType().toString(); }
}