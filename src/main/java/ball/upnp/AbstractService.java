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
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

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
@NoArgsConstructor(access = PROTECTED) @EqualsAndHashCode
public abstract class AbstractService implements AnnotatedService {
    @Getter
    private final List<? extends Action> actionList = new LinkedList<>();
    @Getter
    private final List<? extends StateVariable> serviceStateTable =
        new LinkedList<>();
    private Map<URI,Set<URI>> map = null;

    @Synchronized
    @Override
    public Map<URI,Set<URI>> getUSNMap() {
        if (map == null) {
            map = AnnotatedService.super.getUSNMap();
        }

        return map;
    }

    @Override
    public String toString() { return getServiceType().toString(); }
}
