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

package org.revapi.java;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * Can be used by various checks and problem transformations to work with two elements of the same type.
 * <p/>
 * Typical usage:
 * <pre><code>
 *     javax.lang.model.element.Element e1 = ...;
 *     javax.lang.model.element.Element e2 = ...;
 * <p/>
 *     e1.accept(new ElementPairVisitor&lt;Void&gt;() {
 * <p/>
 *         public Void visitType(TypeElement e1, TypeElement e2) {
 *             ...
 *         }
 *     }, e2);
 * </code></pre>
 *
 * @author Lukas Krejci
 * @since 0.1
 */
public class ElementPairVisitor<R> implements ElementVisitor<R, Element> {

    @SuppressWarnings("UnusedParameters")
    protected R unmatchedAction(Element element, Element otherElement) {
        return null;
    }

    protected R defaultMatchAction(Element element, Element otherElement) {
        return unmatchedAction(element, otherElement);
    }

    @Override
    public final R visit(Element element, Element otherElement) {
        return element.accept(this, otherElement);
    }

    @Override
    public final R visit(Element e) {
        return unmatchedAction(e, null);
    }

    @Override
    public final R visitPackage(PackageElement element, Element otherElement) {
        return otherElement instanceof PackageElement ? visitPackage(element, (PackageElement) otherElement) :
            unmatchedAction(element, otherElement);
    }

    protected R visitPackage(PackageElement element, PackageElement otherElement) {
        return defaultMatchAction(element, otherElement);
    }

    @Override
    public final R visitType(TypeElement element, Element otherElement) {
        return otherElement instanceof TypeElement ? visitType(element, (TypeElement) otherElement) :
            unmatchedAction(element, otherElement);
    }

    protected R visitType(TypeElement element, TypeElement otherElement) {
        return defaultMatchAction(element, otherElement);
    }

    @Override
    public final R visitVariable(VariableElement element, Element otherElement) {
        return otherElement instanceof VariableElement ? visitVariable(element, (VariableElement) otherElement) :
            unmatchedAction(element, otherElement);
    }

    protected R visitVariable(VariableElement element, VariableElement otherElement) {
        return defaultMatchAction(element, otherElement);
    }

    @Override
    public final R visitExecutable(ExecutableElement element, Element otherElement) {
        return otherElement instanceof ExecutableElement ? visitExecutable(element, (ExecutableElement) otherElement) :
            unmatchedAction(element, otherElement);
    }

    protected R visitExecutable(ExecutableElement element, ExecutableElement otherElement) {
        return defaultMatchAction(element, otherElement);
    }

    @Override
    public final R visitTypeParameter(TypeParameterElement element, Element otherElement) {
        return otherElement instanceof TypeParameterElement ? visitTypeParameter(element,
            (TypeParameterElement) otherElement) :
            unmatchedAction(element, otherElement);
    }

    protected R visitTypeParameter(TypeParameterElement element, TypeParameterElement otherElement) {
        return defaultMatchAction(element, otherElement);
    }

    @Override
    public R visitUnknown(Element element, Element otherElement) {
        return unmatchedAction(element, otherElement);
    }
}
