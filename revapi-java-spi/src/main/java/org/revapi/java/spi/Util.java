/*
 * Copyright 2015 Lukas Krejci
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

package org.revapi.java.spi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import javax.lang.model.util.SimpleElementVisitor7;
import javax.lang.model.util.SimpleTypeVisitor7;
import javax.lang.model.util.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A random assortment of methods to help with implementing the Java API checks made public so that
 * extenders don't have to reinvent the wheel.
 *
 * @author Lukas Krejci
 * @since 0.1
 */
public final class Util {


    private static class StringBuilderAndState<T> {
        final StringBuilder bld = new StringBuilder();
        final Set<T> visitedObjects = new HashSet<>();
        boolean visitingMethod;
    }

    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    private static SimpleTypeVisitor7<Void, StringBuilderAndState<TypeMirror>> toUniqueStringVisitor = new SimpleTypeVisitor7<Void, StringBuilderAndState<TypeMirror>>() {

        @Override
        public Void visitPrimitive(PrimitiveType t, StringBuilderAndState<TypeMirror> state) {
            switch (t.getKind()) {
            case BOOLEAN:
                state.bld.append("boolean");
                break;
            case BYTE:
                state.bld.append("byte");
                break;
            case CHAR:
                state.bld.append("char");
                break;
            case DOUBLE:
                state.bld.append("double");
                break;
            case FLOAT:
                state.bld.append("float");
                break;
            case INT:
                state.bld.append("int");
                break;
            case LONG:
                state.bld.append("long");
                break;
            case SHORT:
                state.bld.append("short");
                break;
            default:
                break;
            }

            return null;
        }

        @Override
        public Void visitArray(ArrayType t, StringBuilderAndState<TypeMirror> bld) {
            IgnoreCompletionFailures.in(t::getComponentType).accept(this, bld);
            bld.bld.append("[]");
            return null;
        }

        @Override
        public Void visitIntersection(IntersectionType t, StringBuilderAndState<TypeMirror> state) {
            for (TypeMirror b : IgnoreCompletionFailures.in(t::getBounds)) {
                b.accept(this, state);
                state.bld.append("+");
            }

            return null;
        }

        @Override
        public Void visitTypeVariable(TypeVariable t, StringBuilderAndState<TypeMirror> state) {
            if (state.visitedObjects.contains(t)) {
                state.bld.append("%");
                return null;
            }

            state.visitedObjects.add(t);

            TypeMirror lowerBound = IgnoreCompletionFailures.in(t::getLowerBound);

            if (lowerBound != null && lowerBound.getKind() != TypeKind.NULL) {
                lowerBound.accept(this, state);
                state.bld.append("-");
            }

            IgnoreCompletionFailures.in(t::getUpperBound).accept(this, state);
            state.bld.append("+");
            return null;
        }

        @Override
        public Void visitWildcard(WildcardType t, StringBuilderAndState<TypeMirror> state) {
            TypeMirror superBound = IgnoreCompletionFailures.in(t::getSuperBound);

            if (superBound != null) {
                superBound.accept(this, state);
                state.bld.append("-");
            }

            TypeMirror extendsBound = IgnoreCompletionFailures.in(t::getExtendsBound);

            if (extendsBound != null) {
                extendsBound.accept(this, state);
                state.bld.append("+");
            }

            return null;
        }

        @Override
        public Void visitExecutable(ExecutableType t, StringBuilderAndState<TypeMirror> state) {
            visitTypeVars(IgnoreCompletionFailures.in(t::getTypeVariables), state);

            IgnoreCompletionFailures.in(t::getReturnType).accept(this, state);
            state.bld.append("(");

            Iterator<? extends TypeMirror> it = IgnoreCompletionFailures.in(t::getParameterTypes).iterator();
            if (it.hasNext()) {
                it.next().accept(this, state);
            }
            while (it.hasNext()) {
                state.bld.append(",");
                it.next().accept(this, state);
            }
            state.bld.append(")");

            List<? extends TypeMirror> thrownTypes = IgnoreCompletionFailures.in(t::getThrownTypes);

            if (!thrownTypes.isEmpty()) {
                state.bld.append("throws:");
                it = thrownTypes.iterator();

                it.next().accept(this, state);
                while (it.hasNext()) {
                    state.bld.append(",");
                    it.next().accept(this, state);
                }
            }

            return null;
        }

        @Override
        public Void visitNoType(NoType t, StringBuilderAndState<TypeMirror> state) {
            switch (t.getKind()) {
            case VOID:
                state.bld.append("void");
                break;
            case PACKAGE:
                state.bld.append("package");
                break;
            default:
                break;
            }

            return null;
        }

        @Override
        public Void visitDeclared(DeclaredType t, StringBuilderAndState<TypeMirror> state) {
            CharSequence name = ((TypeElement) t.asElement()).getQualifiedName();
            state.bld.append(name);

            visitTypeVars(IgnoreCompletionFailures.in(t::getTypeArguments), state);

            return null;
        }

        @Override
        public Void visitError(ErrorType t, StringBuilderAndState<TypeMirror> state) {
            //the missing types are like declared types but don't have any further info on them apart from the name...
            state.bld.append(((TypeElement) t.asElement()).getQualifiedName());
            return null;
        }

        private void visitTypeVars(List<? extends TypeMirror> vars, StringBuilderAndState<TypeMirror> state) {
            if (!vars.isEmpty()) {
                state.bld.append("<");
                Iterator<? extends TypeMirror> it = vars.iterator();
                it.next().accept(this, state);

                while (it.hasNext()) {
                    state.bld.append(",");
                    it.next().accept(this, state);
                }

                state.bld.append(">");
            }
        }
    };

    private static SimpleTypeVisitor7<Void, StringBuilderAndState<TypeMirror>> toHumanReadableStringVisitor = new SimpleTypeVisitor7<Void, StringBuilderAndState<TypeMirror>>() {

        @Override
        public Void visitPrimitive(PrimitiveType t, StringBuilderAndState<TypeMirror> state) {
            switch (t.getKind()) {
            case BOOLEAN:
                state.bld.append("boolean");
                break;
            case BYTE:
                state.bld.append("byte");
                break;
            case CHAR:
                state.bld.append("char");
                break;
            case DOUBLE:
                state.bld.append("double");
                break;
            case FLOAT:
                state.bld.append("float");
                break;
            case INT:
                state.bld.append("int");
                break;
            case LONG:
                state.bld.append("long");
                break;
            case SHORT:
                state.bld.append("short");
                break;
            default:
                break;
            }

            return null;
        }

        @Override
        public Void visitArray(ArrayType t, StringBuilderAndState<TypeMirror> state) {
            IgnoreCompletionFailures.in(t::getComponentType).accept(this, state);
            state.bld.append("[]");
            return null;
        }

        @Override
        public Void visitTypeVariable(TypeVariable t, StringBuilderAndState<TypeMirror> state) {
            if (state.visitedObjects.contains(t)) {
                state.bld.append(t.asElement().getSimpleName());
                return null;
            }

            state.visitedObjects.add(t);

            state.bld.append(t.asElement().getSimpleName());

            if (!state.visitingMethod) {
                TypeMirror lowerBound = IgnoreCompletionFailures.in(t::getLowerBound);

                if (lowerBound != null && lowerBound.getKind() != TypeKind.NULL) {
                    state.bld.append(" super ");
                    lowerBound.accept(this, state);
                }

                state.bld.append(" extends ");
                IgnoreCompletionFailures.in(t::getUpperBound).accept(this, state);
            }

            return null;
        }

        @Override
        public Void visitWildcard(WildcardType t, StringBuilderAndState<TypeMirror> state) {
            state.bld.append("?");

            TypeMirror superBound = IgnoreCompletionFailures.in(t::getSuperBound);
            if (superBound != null) {
                state.bld.append(" super ");
                superBound.accept(this, state);
            }

            TypeMirror extendsBound = IgnoreCompletionFailures.in(t::getExtendsBound);
            if (extendsBound != null) {
                state.bld.append(" extends ");
                extendsBound.accept(this, state);
            }

            return null;
        }

        @Override
        public Void visitExecutable(ExecutableType t, StringBuilderAndState<TypeMirror> state) {
            visitTypeVars(IgnoreCompletionFailures.in(t::getTypeVariables), state);

            state.visitingMethod = true;

            IgnoreCompletionFailures.in(t::getReturnType).accept(this, state);
            state.bld.append("(");

            Iterator<? extends TypeMirror> it = IgnoreCompletionFailures.in(t::getParameterTypes).iterator();
            if (it.hasNext()) {
                it.next().accept(this, state);
            }
            while (it.hasNext()) {
                state.bld.append(", ");
                it.next().accept(this, state);
            }
            state.bld.append(")");

            List<? extends TypeMirror> thrownTypes = IgnoreCompletionFailures.in(t::getThrownTypes);
            if (!thrownTypes.isEmpty()) {
                state.bld.append(" throws ");
                it = thrownTypes.iterator();

                it.next().accept(this, state);
                while (it.hasNext()) {
                    state.bld.append(", ");
                    it.next().accept(this, state);
                }
            }

            state.visitingMethod = false;
            return null;
        }

        @Override
        public Void visitNoType(NoType t, StringBuilderAndState<TypeMirror> state) {
            switch (t.getKind()) {
            case VOID:
                state.bld.append("void");
                break;
            case PACKAGE:
                state.bld.append("package");
                break;
            default:
                break;
            }

            return null;
        }

        @Override
        public Void visitDeclared(DeclaredType t, StringBuilderAndState<TypeMirror> state) {
            CharSequence name = ((TypeElement) t.asElement()).getQualifiedName();
            state.bld.append(name);
            try {
                visitTypeVars(IgnoreCompletionFailures.in(t::getTypeArguments), state);
            } catch (RuntimeException e) {
                LOG.debug("Failed to enumerate type arguments of '" + name + "'. Class is missing?", e);
            }

            return null;
        }

        @Override
        public Void visitIntersection(IntersectionType t, StringBuilderAndState<TypeMirror> state) {
            Iterator<? extends TypeMirror> it = IgnoreCompletionFailures.in(t::getBounds).iterator();
            if (it.hasNext()) {
                it.next().accept(this, state);
            }

            TypeVisitor<Void, StringBuilderAndState<TypeMirror>> me = this;

            it.forEachRemaining(b -> { state.bld.append(", "); b.accept(me, state); });

            return null;
        }

        private void visitTypeVars(List<? extends TypeMirror> vars, StringBuilderAndState<TypeMirror> state) {
            if (!vars.isEmpty()) {
                state.bld.append("<");
                Iterator<? extends TypeMirror> it = vars.iterator();
                it.next().accept(this, state);

                while (it.hasNext()) {
                    state.bld.append(", ");
                    it.next().accept(this, state);
                }

                state.bld.append(">");
            }
        }

    };

    private static SimpleElementVisitor7<Void, StringBuilderAndState<TypeMirror>> toHumanReadableStringElementVisitor = new SimpleElementVisitor7<Void, StringBuilderAndState<TypeMirror>>() {
        @Override
        public Void visitVariable(VariableElement e, StringBuilderAndState<TypeMirror> state) {
            Element enclosing = e.getEnclosingElement();
            if (enclosing instanceof TypeElement) {
                enclosing.accept(this, state);
                state.bld.append(".").append(e.getSimpleName());
            } else if (enclosing instanceof ExecutableElement) {
                if (state.visitingMethod) {
                    //we're visiting a method, so we need to output the in a simple way
                    e.asType().accept(toHumanReadableStringVisitor, state);
                    //NOTE the names of method params seem not to be available
                    //stringBuilder.append(" ").append(e.getSimpleName());
                } else {
                    //this means someone asked to directly output a string representation of a method parameter
                    //in this case, we need to identify the parameter inside the full method signature so that
                    //the full location is available.
                    int paramIdx = ((ExecutableElement) enclosing).getParameters().indexOf(e);
                    enclosing.accept(this, state);
                    int openPar = state.bld.indexOf("(");
                    int closePar = state.bld.indexOf(")", openPar);

                    int paramStart = openPar + 1;
                    int curParIdx = -1;
                    for (int i = openPar + 1; i < closePar; ++i) {
                        if (state.bld.charAt(i) == ',') {
                            curParIdx++;
                            if (curParIdx == paramIdx) {
                                String par = state.bld.substring(paramStart, i);
                                state.bld.replace(paramStart, i, "===" + par + "===");
                            } else {
                                //accommodate for the space after commas for the second and further parameters
                                paramStart = i + (paramIdx == 0 ? 1 : 2);
                            }
                        }
                    }

                    if (++curParIdx == paramIdx) {
                        String par = state.bld.substring(paramStart, closePar);
                        state.bld.replace(paramStart, closePar, "===" + par + "===");
                    }
                }
            } else {
                state.bld.append(e.getSimpleName());
            }

            return null;
        }

        @Override
        public Void visitPackage(PackageElement e, StringBuilderAndState<TypeMirror> state) {
            state.bld.append(e.getQualifiedName());
            return null;
        }

        @Override
        public Void visitType(TypeElement e, StringBuilderAndState<TypeMirror> state) {
            state.bld.append(e.getQualifiedName());

            List<? extends TypeParameterElement> typePars = IgnoreCompletionFailures.in(e::getTypeParameters);
            if (typePars.size() > 0) {
                state.bld.append("<");

                typePars.get(0).accept(this, state);
                for (int i = 1; i < typePars.size(); ++i) {
                    state.bld.append(", ");
                    typePars.get(i).accept(this, state);
                }
                state.bld.append(">");
            }

            return null;
        }

        @Override
        public Void visitExecutable(ExecutableElement e, StringBuilderAndState<TypeMirror> state) {
            state.visitingMethod = true;

            try {
                List<? extends TypeParameterElement> typePars = IgnoreCompletionFailures.in(e::getTypeParameters);
                if (typePars.size() > 0) {
                    state.bld.append("<");

                    typePars.get(0).accept(this, state);
                    for (int i = 1; i < typePars.size(); ++i) {
                        state.bld.append(", ");
                        typePars.get(i).accept(this, state);
                    }
                    state.bld.append("> ");
                }

                IgnoreCompletionFailures.in(e::getReturnType).accept(toHumanReadableStringVisitor, state);
                state.bld.append(" ");
                e.getEnclosingElement().accept(this, state);
                state.bld.append("::").append(e.getSimpleName()).append("(");

                List<? extends VariableElement> pars = IgnoreCompletionFailures.in(e::getParameters);
                if (pars.size() > 0) {
                    pars.get(0).accept(this, state);
                    for (int i = 1; i < pars.size(); ++i) {
                        state.bld.append(", ");
                        pars.get(i).accept(this, state);
                    }
                }

                state.bld.append(")");

                List<? extends TypeMirror> thrownTypes = IgnoreCompletionFailures.in(e::getThrownTypes);

                if (thrownTypes.size() > 0) {
                    state.bld.append(" throws ");
                    thrownTypes.get(0).accept(toHumanReadableStringVisitor, state);
                    for (int i = 1; i < thrownTypes.size(); ++i) {
                        state.bld.append(", ");
                        thrownTypes.get(i).accept(toHumanReadableStringVisitor, state);
                    }
                }

                return null;
            } finally {
                state.visitingMethod = false;
            }
        }

        @Override
        public Void visitTypeParameter(TypeParameterElement e, StringBuilderAndState<TypeMirror> state) {
            state.bld.append(e.getSimpleName());
            List<? extends TypeMirror> bounds = IgnoreCompletionFailures.in(e::getBounds);
            if (bounds.size() > 0) {
                if (bounds.size() == 1) {
                    TypeMirror firstBound = bounds.get(0);
                    String bs = toHumanReadableString(firstBound);
                    if (!"java.lang.Object".equals(bs)) {
                        state.bld.append(" extends ").append(bs);
                    }
                } else {
                    state.bld.append(" extends ");
                    bounds.get(0).accept(toHumanReadableStringVisitor, state);
                    for (int i = 1; i < bounds.size(); ++i) {
                        state.bld.append(", ");
                        bounds.get(i).accept(toHumanReadableStringVisitor, state);
                    }
                }
            }

            return null;
        }
    };

    private Util() {

    }

    /**
     * To be used to compare types from different compilations (which are not comparable by standard means in Types).
     * This just compares the type names.
     *
     * @param t1 first type
     * @param t2 second type
     *
     * @return true if the types have the same fqn, false otherwise
     */
    public static boolean isSameType(@Nonnull TypeMirror t1, @Nonnull TypeMirror t2) {
        String t1Name = toUniqueString(t1);
        String t2Name = toUniqueString(t2);

        return t1Name.equals(t2Name);
    }

    @Nonnull
    public static String toHumanReadableString(@Nonnull Element element) {
        StringBuilderAndState<TypeMirror> state = new StringBuilderAndState<>();
        element.accept(toHumanReadableStringElementVisitor, state);
        return state.bld.toString();
    }

    /**
     * Represents the type mirror as a string in such a way that it can be used for equality comparisons.
     *
     * @param t type to convert to string
     * @return the string representation of the type that is fit for equality comparisons
     */
    @Nonnull
    public static String toUniqueString(@Nonnull TypeMirror t) {
        StringBuilderAndState<TypeMirror> state = new StringBuilderAndState<>();
        t.accept(toUniqueStringVisitor, state);
        return state.bld.toString();
    }

    @Nonnull
    public static String toHumanReadableString(@Nonnull TypeMirror t) {
        StringBuilderAndState<TypeMirror> state = new StringBuilderAndState<>();
        t.accept(toHumanReadableStringVisitor, state);
        return state.bld.toString();
    }

    @Nonnull
    public static String toUniqueString(@Nonnull AnnotationValue v) {
        return toHumanReadableString(v);
    }

    @Nonnull
    public static String toHumanReadableString(@Nonnull AnnotationValue v) {
        return v.accept(new SimpleAnnotationValueVisitor7<String, Void>() {

            @Override
            protected String defaultAction(Object o, Void ignored) {
                return o.toString();
            }

            @Override
            public String visitType(TypeMirror t, Void ignored) {
                return toHumanReadableString(t) + ".class";
            }

            @Override
            public String visitEnumConstant(VariableElement c, Void ignored) {
                return toHumanReadableString(c.asType()) + "." + c.getSimpleName().toString();
            }

            @Override
            public String visitAnnotation(AnnotationMirror a, Void ignored) {
                StringBuilder bld = new StringBuilder("@").append(toHumanReadableString(a.getAnnotationType()));

                if (!a.getElementValues().isEmpty()) {
                    bld.append("(");
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : a.getElementValues()
                        .entrySet()) {

                        bld.append(e.getKey().getSimpleName().toString()).append(" = ");
                        bld.append(e.getValue().accept(this, null));
                        bld.append(", ");
                    }
                    bld.replace(bld.length() - 2, bld.length(), "");
                    bld.append(")");
                }
                return bld.toString();
            }

            @Override
            public String visitArray(List<? extends AnnotationValue> vals, Void ignored) {
                StringBuilder bld = new StringBuilder("[");

                Iterator<? extends AnnotationValue> it = vals.iterator();
                if (it.hasNext()) {
                    bld.append(it.next().accept(this, null));
                }

                while (it.hasNext()) {
                    bld.append(", ").append(it.next().accept(this, null));
                }

                bld.append("]");

                return bld.toString();
            }
        }, null);
    }

    /**
     * Returns all the super classes of given type. I.e. the returned list does NOT contain any interfaces
     * the class or tis superclasses implement.
     *
     * @param types the Types instance of the compilation environment from which the type comes from
     * @param type  the type
     *
     * @return the list of super classes
     */
    @Nonnull
    public static List<TypeMirror> getAllSuperClasses(@Nonnull Types types, @Nonnull TypeMirror type) {
        List<TypeMirror> ret = new ArrayList<>();

        try {
            List<? extends TypeMirror> superTypes = types.directSupertypes(type);
            while (superTypes != null && !superTypes.isEmpty()) {
                TypeMirror superClass = superTypes.get(0);
                ret.add(superClass);
                superTypes = types.directSupertypes(superClass);
            }
        } catch (RuntimeException e) {
            LOG.debug("Failed to find all super classes of type '" + toHumanReadableString(type) + ". Possibly " +
                "missing classes?", e);
        }

        return ret;
    }

    /**
     * Similar to {@link #getAllSuperClasses(javax.lang.model.util.Types, javax.lang.model.type.TypeMirror)} but
     * returns all super types including implemented interfaces.
     *
     * @param types the Types instance of the compilation environment from which the type comes from
     * @param type  the type
     *
     * @return the list of super tpyes
     */
    @Nonnull
    public static List<TypeMirror> getAllSuperTypes(@Nonnull Types types, @Nonnull TypeMirror type) {
        ArrayList<TypeMirror> ret = new ArrayList<>();
        fillAllSuperTypes(types, type, ret);

        return ret;
    }

    /**
     * Similar to {@link #getAllSuperTypes(javax.lang.model.util.Types, javax.lang.model.type.TypeMirror)} but avoids
     * instantiation of a new list.
     *
     * @param types  the Types instance of the compilation environment from which the type comes from
     * @param type   the type
     * @param result the list to add the results to.
     */
    public static void fillAllSuperTypes(@Nonnull Types types, @Nonnull TypeMirror type,
        @Nonnull List<TypeMirror> result) {

        try {
            List<? extends TypeMirror> superTypes = types.directSupertypes(type);

            for (TypeMirror t : superTypes) {
                result.add(t);
                fillAllSuperTypes(types, t, result);
            }
        } catch (RuntimeException e) {
            LOG.debug("Failed to find all super types of type '" + toHumanReadableString(type) + ". Possibly " +
                "missing classes?", e);
        }
    }

    /**
     * Checks whether given type is a sub type or is equal to one of the provided types.
     * Note that this does not require the type to come from the same type "environment"
     * or compilation as the super types.
     *
     * @param type            the type to check
     * @param superTypes      the list of supposed super types
     * @param typeEnvironment the environment in which the type lives
     *
     * @return true if type a sub type of one of the provided super types, false otherwise.
     */
    public static boolean isSubtype(@Nonnull TypeMirror type, @Nonnull List<? extends TypeMirror> superTypes,
        @Nonnull Types typeEnvironment) {

        List<TypeMirror> typeSuperTypes = getAllSuperTypes(typeEnvironment, type);
        typeSuperTypes.add(0, type);

        for (TypeMirror t : typeSuperTypes) {
            String oldi = toUniqueString(t);
            for (TypeMirror i : superTypes) {
                String newi = toUniqueString(i);
                if (oldi.equals(newi)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Extracts the names of the attributes from the executable elements that represents them in the given map and
     * returns a map keyed by those names.
     * <p>
     * I.e. while representing annotation attributes on an annotation type by executable elements is technically
     * correct
     * it is more convenient to address them simply by their names, which, in case of annotation types, are unique
     * (i.e. you cannot overload an annotation attribute, because they cannot have method parameters).
     *
     * @param attributes the attributes as obtained by
     *                   {@link javax.lang.model.element.AnnotationMirror#getElementValues()}
     *
     * @return the equivalent of the supplied map keyed by attribute names instead of the full-blown executable elements
     */
    @Nonnull
    public static Map<String, Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> keyAnnotationAttributesByName(
        @Nonnull Map<? extends ExecutableElement, ? extends AnnotationValue> attributes) {
        Map<String, Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> result = new LinkedHashMap<>();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : attributes.entrySet()) {
            result.put(e.getKey().getSimpleName().toString(), e);
        }

        return result;
    }

    public static boolean isEqual(@Nonnull AnnotationValue oldVal, @Nonnull AnnotationValue newVal) {
        return oldVal.accept(new SimpleAnnotationValueVisitor7<Boolean, Object>() {

            @Override
            protected Boolean defaultAction(Object o, Object o2) {
                return o.equals(o2);
            }

            @Override
            public Boolean visitType(TypeMirror t, Object o) {
                if (!(o instanceof TypeMirror)) {
                    return false;
                }

                String os = toUniqueString(t);
                String ns = toUniqueString((TypeMirror) o);

                return os.equals(ns);
            }

            @Override
            public Boolean visitEnumConstant(VariableElement c, Object o) {
                return o instanceof VariableElement &&
                    c.getSimpleName().toString().equals(((VariableElement) o).getSimpleName().toString());
            }

            @Override
            public Boolean visitAnnotation(AnnotationMirror a, Object o) {
                if (!(o instanceof AnnotationMirror)) {
                    return false;
                }

                AnnotationMirror oa = (AnnotationMirror) o;

                String ot = toUniqueString(a.getAnnotationType());
                String nt = toUniqueString(oa.getAnnotationType());

                if (!ot.equals(nt)) {
                    return false;
                }

                if (a.getElementValues().size() != oa.getElementValues().size()) {
                    return false;
                }

                Map<String, Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> aVals = keyAnnotationAttributesByName(
                    a.getElementValues());
                Map<String, Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> oVals = keyAnnotationAttributesByName(
                    oa.getElementValues());

                for (Map.Entry<String, Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> aVal : aVals
                    .entrySet()) {
                    String name = aVal.getKey();
                    Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> aAttr = aVal.getValue();
                    Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> oAttr = oVals.get(name);

                    if (oAttr == null) {
                        return false;
                    }

                    String as = toUniqueString(aAttr.getValue());
                    String os = toUniqueString(oAttr.getValue());

                    if (!as.equals(os)) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public Boolean visitArray(List<? extends AnnotationValue> vals, Object o) {
                if (!(o instanceof List)) {
                    return false;
                }

                @SuppressWarnings("unchecked")
                List<? extends AnnotationValue> ovals = (List<? extends AnnotationValue>) o;

                if (vals.size() != ovals.size()) {
                    return false;
                }

                for (int i = 0; i < vals.size(); ++i) {
                    if (!vals.get(i).accept(this, ovals.get(i).getValue())) {
                        return false;
                    }
                }

                return true;
            }
        }, newVal.getValue());
    }

    /**
     * Tries to find a type element using the provided Elements helper given its binary name. Note that this might NOT
     * be able to find some classes if there are conflicts in the canonical names (but that theoretically cannot happen
     * because the compiler should refuse to compile code with conflicting canonical names).
     *
     * @param elements   the elements instance to search the classpath
     * @param binaryName the binary name of the class
     *
     * @return the type element with given binary name
     */
    public static TypeElement findTypeByBinaryName(Elements elements, String binaryName) {
        return findTypeByBinaryName(elements, binaryName, 0);
    }

    private static TypeElement findTypeByBinaryName(Elements elements, String binaryName, int swapStartPos) {
        TypeElement ret;

        String attemptedName = binaryName;

        //this is optimized for the most common scenario of classes having no $ in their names...
        if (swapStartPos >= binaryName.length()) {
            return null;
        }

        if (attemptedName.indexOf('$', swapStartPos) == -1) {
            return elements.getTypeElement(attemptedName);
        }

        int dollarPos = swapStartPos;
        while ((ret = elements.getTypeElement(attemptedName)) == null) {
            dollarPos = attemptedName.indexOf('$', dollarPos);
            if (dollarPos == -1) {
                break;
            }

            if (dollarPos < binaryName.length()) {
                attemptedName = attemptedName.substring(0, dollarPos) + "." + attemptedName.substring(dollarPos + 1);
            }
        }

        if (ret == null) {
            //ok, we need to try if there isn't a match with a dollar on the current position...
            dollarPos = binaryName.indexOf('$', swapStartPos);
            if (dollarPos != -1) {
                ret = findTypeByBinaryName(elements, binaryName, dollarPos + 1);

                if (ret == null) {
                    //still nothing, so let's try a dot instead of a dollar on the current position
                    binaryName = binaryName.substring(0, dollarPos) + "." + binaryName.substring(dollarPos + 1);
                    ret = findTypeByBinaryName(elements, binaryName, swapStartPos + 1);
                }
            }
        }

        return ret;
    }
}
