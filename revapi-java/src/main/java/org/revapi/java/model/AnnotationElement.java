/*
 * Copyright 2014 Lukas Krejci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package org.revapi.java.model;

import java.util.SortedSet;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.revapi.Element;
import org.revapi.java.JavaAnnotationElement;
import org.revapi.java.compilation.ProbingEnvironment;
import org.revapi.simple.SimpleElement;

/**
 * @author Lukas Krejci
 * @since 0.1
 */
public final class AnnotationElement extends SimpleElement implements JavaAnnotationElement {
    private final AnnotationMirror annotation;
    private final ProbingEnvironment environment;

    public AnnotationElement(ProbingEnvironment environment, AnnotationMirror annotation) {
        this.environment = environment;
        this.annotation = annotation;
    }

    @Override
    public AnnotationMirror getAnnotation() {
        return annotation;
    }

    @Override
    public Types getTypeUtils() {
        return environment.getTypeUtils();
    }

    @Override
    public Elements getElementUtils() {
        return environment.getElementUtils();
    }

    @Override
    public int compareTo(Element o) {
        if (!(o instanceof AnnotationElement)) {
            return 1;
        }

        return toString().compareTo(o.toString());
    }

    @Override
    protected SortedSet<Element> newChildrenInstance() {
        //TODO init the values
        return super.newChildrenInstance();
    }

    @Override
    public String toString() {
        return annotation.getAnnotationType().asElement().getSimpleName().toString();
    }
}
