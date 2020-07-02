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
import ball.annotation.CompileTimeCheck;
import ball.upnp.annotation.Template;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlTransient;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.annotation.AnnotationUtils;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

/**
 * {@link Templated} support for {@link Template} annotation.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface Templated {

    /**
     * {@link #TEMPLATE} = {@value #TEMPLATE}
     */
    public static final String TEMPLATE = "/templates/%s.xml";

    /**
     * {@link Pattern} to parse {@link #getTemplate()} to generate a
     * {@link SpecVersion}.  Provides "{@code major}" and  "{@code minor}"
     * matching groups.
     */
    @CompileTimeCheck
    public static final Pattern SPEC_VERSION_PATTERN =
        Pattern.compile("(?i)^.+-(?<major>[0-9]+)-(?<minor>[0-9]+)$");

    /**
     * Method to get the {@link Template}'s name.
     *
     * @return  The name.
     */
    @XmlTransient @JsonIgnore
    default String getTemplate() {
        Template annotation =
            AnnotationUtils.findAnnotation(getClass(), Template.class);

        return (annotation != null) ? annotation.value() : null;
    }

    /**
     * Method to get the {@link Template}'s resource path.
     *
     * @return  The resource path.
     *
     * @throws  NullPointerException
     *                          If {@link #getTemplate()} returns
     *                          {@code null}.
     */
    @XmlTransient @JsonIgnore
    default String getTemplatePath() {
        return String.format(TEMPLATE, requireNonNull(getTemplate()));
    }

    /**
     * Method to generate {@link SpecVersion} from {@link Template}.
     *
     * @return  The {@link SpecVersion}.
     */
    default SpecVersion getSpecVersion() {
        SpecVersion version = null;
        Matcher matcher = SPEC_VERSION_PATTERN.matcher(getTemplate());

        if (matcher.matches()) {
            version =
                new SpecVersion(matcher.group("major"),
                                matcher.group("minor"));
        }

        return version;
    }

    /**
     * See {@link #getSpecVersion()}.
     *
     * {@bean.info}
     */
    @AllArgsConstructor(access = PRIVATE) @Data
    public class SpecVersion {
        private String major = null;
        private String minor = null;
    }
}
