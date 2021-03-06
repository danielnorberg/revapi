/*
 * Copyright 2014 Lukas Krejci
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
 * limitations under the License
 */

package org.revapi.java.checks.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

import org.revapi.Difference;
import org.revapi.java.spi.CheckBase;
import org.revapi.java.spi.Code;
import org.revapi.java.spi.Util;

/**
 * @author Lukas Krejci
 * @since 0.1
 */
public final class AttributeValueChanged extends CheckBase {
    @Override
    protected List<Difference> doVisitAnnotation(AnnotationMirror oldAnnotation,
        AnnotationMirror newAnnotation) {

        if (oldAnnotation == null || newAnnotation == null) {
            return null;
        }

        List<Difference> result = new ArrayList<>();

        Map<String, Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> oldAttrs = Util
            .keyAnnotationAttributesByName(oldAnnotation.getElementValues());
        Map<String, Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> newAttrs = Util
            .keyAnnotationAttributesByName(newAnnotation.getElementValues());

        for (Map.Entry<String, Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> oldE : oldAttrs
            .entrySet()) {

            String name = oldE.getKey();
            Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> oldValue = oldE.getValue();
            Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> newValue = newAttrs.get(name);

            if (newValue == null) {
                result.add(
                    createDifference(Code.ANNOTATION_ATTRIBUTE_REMOVED,
                        new String[]{name, Util.toHumanReadableString(oldAnnotation.getAnnotationType())},
                        oldValue.getKey(), oldAnnotation)
                );
            } else if (!Util.isEqual(oldValue.getValue(), newValue.getValue())) {
                result.add(createDifference(Code.ANNOTATION_ATTRIBUTE_VALUE_CHANGED,
                    new String[]{name, Util.toHumanReadableString(oldAnnotation.getAnnotationType()),
                        Util.toHumanReadableString(oldValue.getValue()),
                        Util.toHumanReadableString(newValue.getValue())}, oldValue.getKey(), oldAnnotation,
                    oldValue.getValue(), newValue.getValue()
                ));
            }

            newAttrs.remove(name);
        }

        for (Map.Entry<String, Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> newE : newAttrs
            .entrySet()) {
            String name = newE.getKey();
            Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> newValue = newE.getValue();
            Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> oldValue = oldAttrs.get(name);

            if (oldValue == null) {
                result.add(
                    createDifference(Code.ANNOTATION_ATTRIBUTE_ADDED,
                        new String[]{name, Util.toHumanReadableString(newAnnotation.getAnnotationType())},
                        newValue.getKey(), newAnnotation)
                );
            }
        }

        return result.isEmpty() ? null : result;
    }
}
