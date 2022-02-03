/*
 * Copyright 2016 the original author or authors.
 *
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
 */

package org.gradle.internal.component.external.model;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ArtifactSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.DefaultArtifactSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ResolvableArtifact;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.specs.ExcludeSpec;
import org.gradle.api.internal.artifacts.type.ArtifactTypeRegistry;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.internal.component.model.ComponentArtifacts;
import org.gradle.internal.component.model.ComponentResolveMetadata;
import org.gradle.internal.component.model.ConfigurationMetadata;
import org.gradle.internal.component.model.VariantResolveMetadata;
import org.gradle.internal.model.CalculatedValueContainerFactory;
import org.gradle.internal.resolve.resolver.ArtifactResolver;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Uses the artifacts attached to each variant.
 */
public class MetadataSourcedAllVariantsComponentArtifacts implements ComponentArtifacts {
    @Override
    public ArtifactSet getArtifactsFor(ComponentResolveMetadata component, ConfigurationMetadata configuration, ArtifactResolver artifactResolver, Map<ComponentArtifactIdentifier, ResolvableArtifact> allResolvedArtifacts, ArtifactTypeRegistry artifactTypeRegistry, ExcludeSpec exclusions, ImmutableAttributes overriddenAttributes, CalculatedValueContainerFactory calculatedValueContainerFactory) {
        Optional<ImmutableList<? extends ConfigurationMetadata>> variantsForGraphTraversal = component.getVariantsForGraphTraversal();
        final Set<VariantResolveMetadata> variants = new LinkedHashSet<>();
        if (variantsForGraphTraversal.isPresent()) {
            ImmutableList<? extends ConfigurationMetadata> allVariants = variantsForGraphTraversal.get();
            // filter variant capabilities based on the configuration's capability
            for (ConfigurationMetadata configurationMetadata : allVariants) {
                if (configurationMetadata.getCapabilities().getCapabilities().equals(configuration.getCapabilities().getCapabilities())) {
                    variants.addAll(configurationMetadata.getVariants());
                }
            }
        } else {
            variants.addAll(configuration.getVariants());
        }

        return DefaultArtifactSet.createFromVariantMetadata(component.getId(), component.getModuleVersionId(), component.getSources(), exclusions, variants, component.getAttributesSchema(), artifactResolver, allResolvedArtifacts, artifactTypeRegistry, overriddenAttributes, calculatedValueContainerFactory);
    }
}
