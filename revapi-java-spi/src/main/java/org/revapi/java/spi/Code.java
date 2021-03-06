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

import org.revapi.CompatibilityType;
import org.revapi.Difference;
import org.revapi.DifferenceSeverity;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.*;

import static org.revapi.CompatibilityType.*;
import static org.revapi.DifferenceSeverity.*;

/**
 * The is a list of all difference codes Revapi's Java extension can emit. This can be used by others when they want to
 * override the default detection behavior by providing custom difference transforms.
 *
 * @author Lukas Krejci
 * @since 0.1
 */
public enum Code {
    //these two are used during class tree initialization even before any "normal" checks can occur
    MISSING_IN_OLD_API("java.missing.oldClass", POTENTIALLY_BREAKING, POTENTIALLY_BREAKING, null),
    MISSING_IN_NEW_API("java.missing.newClass", POTENTIALLY_BREAKING, POTENTIALLY_BREAKING, null),

    ELEMENT_NO_LONGER_DEPRECATED("java.element.noLongerDeprecated", NON_BREAKING, NON_BREAKING, null),
    ELEMENT_NOW_DEPRECATED("java.element.nowDeprecated", NON_BREAKING, NON_BREAKING, null),

    CLASS_VISIBILITY_INCREASED("java.class.visibilityIncreased", NON_BREAKING, NON_BREAKING, null),
    CLASS_VISIBILITY_REDUCED("java.class.visibilityReduced", BREAKING, BREAKING, null),
    CLASS_KIND_CHANGED("java.class.kindChanged", BREAKING, BREAKING, null),
    CLASS_NO_LONGER_FINAL("java.class.noLongerFinal", NON_BREAKING, NON_BREAKING, null),
    CLASS_NOW_FINAL("java.class.nowFinal", BREAKING, BREAKING, null),
    CLASS_NO_LONGER_ABSTRACT("java.class.noLongerAbstract", NON_BREAKING, NON_BREAKING, null),
    CLASS_NOW_ABSTRACT("java.class.nowAbstract", BREAKING, BREAKING, null),
    CLASS_ADDED("java.class.added", NON_BREAKING, NON_BREAKING, null),
    CLASS_REMOVED("java.class.removed", BREAKING, BREAKING, null),
    CLASS_NO_LONGER_IMPLEMENTS_INTERFACE("java.class.noLongerImplementsInterface", BREAKING, BREAKING, null),
    CLASS_NOW_IMPLEMENTS_INTERFACE("java.class.nowImplementsInterface", NON_BREAKING, NON_BREAKING, null),
    CLASS_FINAL_CLASS_INHERITS_FROM_NEW_CLASS("java.class.finalClassInheritsFromNewClass", NON_BREAKING, NON_BREAKING,
        null),
    CLASS_NON_FINAL_CLASS_INHERITS_FROM_NEW_CLASS("java.class.nonFinalClassInheritsFromNewClass", POTENTIALLY_BREAKING,
        POTENTIALLY_BREAKING, null),
    CLASS_NOW_CHECKED_EXCEPTION("java.class.nowCheckedException", BREAKING, NON_BREAKING, null),
    CLASS_NO_LONGER_INHERITS_FROM_CLASS("java.class.noLongerInheritsFromClass", BREAKING, BREAKING, null),
    CLASS_NON_PUBLIC_PART_OF_API("java.class.nonPublicPartOfAPI", NON_BREAKING, NON_BREAKING, BREAKING),
    CLASS_SUPER_TYPE_TYPE_PARAMETERS_CHANGED("java.class.superTypeTypeParametersChanged", POTENTIALLY_BREAKING,
        POTENTIALLY_BREAKING, null),
    CLASS_EXTERNAL_CLASS_EXPOSED_IN_API("java.class.externalClassExposedInAPI", NON_BREAKING, NON_BREAKING,
            POTENTIALLY_BREAKING),
    CLASS_EXTERNAL_CLASS_NO_LONGER_EXPOSED_IN_API("java.class.externalClassNoLongerExposedInAPI", BREAKING, BREAKING,
            null),

    ANNOTATION_ADDED("java.annotation.added", NON_BREAKING, NON_BREAKING, POTENTIALLY_BREAKING),
    ANNOTATION_REMOVED("java.annotation.removed", NON_BREAKING, NON_BREAKING, POTENTIALLY_BREAKING),
    ANNOTATION_ATTRIBUTE_VALUE_CHANGED("java.annotation.attributeValueChanged", NON_BREAKING, NON_BREAKING,
        POTENTIALLY_BREAKING),
    ANNOTATION_ATTRIBUTE_ADDED("java.annotation.attributeAdded", NON_BREAKING, NON_BREAKING, POTENTIALLY_BREAKING),
    ANNOTATION_ATTRIBUTE_REMOVED("java.annotation.attributeRemoved", NON_BREAKING, NON_BREAKING, POTENTIALLY_BREAKING),
    ANNOTATION_NO_LONGER_INHERITED("java.annotation.noLongerInherited", NON_BREAKING, NON_BREAKING,
        POTENTIALLY_BREAKING),
    ANNOTATION_NOW_INHERITED("java.annotation.nowInherited", NON_BREAKING, NON_BREAKING, POTENTIALLY_BREAKING),
    ANNOTATION_NO_LONGER_PRESENT("java.annotation.noLongerPresent", NON_BREAKING, NON_BREAKING, POTENTIALLY_BREAKING),

    FIELD_ADDED_STATIC_FIELD("java.field.addedStaticField", NON_BREAKING, NON_BREAKING, null),
    FIELD_ADDED("java.field.added", NON_BREAKING, NON_BREAKING, null),
    FIELD_REMOVED("java.field.removed", BREAKING, BREAKING, null),
    FIELD_CONSTANT_REMOVED("java.field.removedWithConstant", BREAKING, NON_BREAKING, POTENTIALLY_BREAKING),
    FIELD_CONSTANT_VALUE_CHANGED("java.field.constantValueChanged", NON_BREAKING, NON_BREAKING, BREAKING),
    FIELD_NOW_CONSTANT("java.field.nowConstant", NON_BREAKING, NON_BREAKING, null),
    FIELD_NO_LONGER_CONSTANT("java.field.noLongerConstant", NON_BREAKING, NON_BREAKING, null),
    FIELD_NOW_FINAL("java.field.nowFinal", BREAKING, BREAKING, null),
    FIELD_NO_LONGER_FINAL("java.field.noLongerFinal", NON_BREAKING, NON_BREAKING, null),
    FIELD_NO_LONGER_STATIC("java.field.noLongerStatic", BREAKING, BREAKING, null),
    FIELD_NOW_STATIC("java.field.nowStatic", NON_BREAKING, BREAKING, null),
    FIELD_TYPE_CHANGED("java.field.typeChanged", BREAKING, BREAKING, null),
    FIELD_SERIAL_VERSION_UID_UNCHANGED("java.field.serialVersionUIDUnchanged", NON_BREAKING, NON_BREAKING,
        POTENTIALLY_BREAKING),
    FIELD_VISIBILITY_INCREASED("java.field.visibilityIncreased", NON_BREAKING, NON_BREAKING, null),
    FIELD_VISIBILITY_REDUCED("java.field.visibilityReduced", BREAKING, BREAKING, null),
    FIELD_ENUM_CONSTANT_ORDER_CHANGED("java.field.enumConstantOrderChanged", NON_BREAKING, NON_BREAKING,
            POTENTIALLY_BREAKING),

    METHOD_DEFAULT_VALUE_ADDED("java.method.defaultValueAdded", NON_BREAKING, NON_BREAKING, null),
    METHOD_DEFAULT_VALUE_CHANGED("java.method.defaultValueChanged", NON_BREAKING, NON_BREAKING, POTENTIALLY_BREAKING),
    METHOD_DEFAULT_VALUE_REMOVED("java.method.defaultValueRemoved", BREAKING, NON_BREAKING, BREAKING),
    METHOD_ADDED_TO_INTERFACE("java.method.addedToInterface", BREAKING, POTENTIALLY_BREAKING, null),
    METHOD_ATTRIBUTE_WITH_NO_DEFAULT_ADDED_TO_ANNOTATION_TYPE("java.method.attributeWithNoDefaultAddedToAnnotationType",
        BREAKING, NON_BREAKING, BREAKING),
    METHOD_ATTRIBUTE_WITH_DEFAULT_ADDED_TO_ANNOTATION_TYPE("java.method.attributeWithDefaultAddedToAnnotationType",
        NON_BREAKING, NON_BREAKING, null),
    METHOD_ABSTRACT_METHOD_ADDED("java.method.abstractMethodAdded", BREAKING, BREAKING, null),
    METHOD_ADDED("java.method.added", NON_BREAKING, NON_BREAKING, null),
    METHOD_FINAL_METHOD_ADDED_TO_NON_FINAL_CLASS("java.method.finalMethodAddedToNonFinalClass", POTENTIALLY_BREAKING,
        POTENTIALLY_BREAKING, null),
    METHOD_REMOVED("java.method.removed", BREAKING, BREAKING, null),
    METHOD_OVERRIDING_METHOD_REMOVED("java.method.overridingMethodRemoved", NON_BREAKING, NON_BREAKING, null),
    METHOD_REPLACED_BY_ABSTRACT_METHOD_IN_SUPERCLASS("java.method.replacedByAbstractMethodInSuperClass", BREAKING,
        BREAKING, null),
    METHOD_NON_FINAL_METHOD_REPLACED_BY_FINAL_IN_SUPERCLASS("java.method.nonFinalReplacedByFinalInSuperclass",
        POTENTIALLY_BREAKING,
        POTENTIALLY_BREAKING, null),
    METHOD_ATTRIBUTE_REMOVED_FROM_ANNOTATION_TYPE(
        "java.method.attributeRemovedFromAnnotationType", BREAKING, BREAKING, null),
    METHOD_NO_LONGER_FINAL("java.method.noLongerFinal", NON_BREAKING, NON_BREAKING, null),
    METHOD_NOW_FINAL("java.method.nowFinal", BREAKING, BREAKING, null),
    METHOD_VISIBILITY_INCREASED("java.method.visibilityIncreased", NON_BREAKING, NON_BREAKING, null),
    METHOD_VISIBILITY_REDUCED("java.method.visibilityReduced", BREAKING, BREAKING, null),
    METHOD_RETURN_TYPE_CHANGED("java.method.returnTypeChanged", POTENTIALLY_BREAKING, BREAKING, null),
    METHOD_RETURN_TYPE_TYPE_PARAMETERS_CHANGED("java.method.returnTypeTypeParametersChanged", BREAKING,
        NON_BREAKING, null),
    METHOD_NUMBER_OF_PARAMETERS_CHANGED("java.method.numberOfParametersChanged", BREAKING, BREAKING, null),
    METHOD_PARAMETER_TYPE_CHANGED("java.method.parameterTypeChanged", POTENTIALLY_BREAKING, BREAKING, null),
    METHOD_NO_LONGER_STATIC("java.method.noLongerStatic", BREAKING, BREAKING, null),
    METHOD_NOW_STATIC("java.method.nowStatic", NON_BREAKING, BREAKING, null),
    METHOD_CHECKED_EXCEPTION_ADDED("java.method.exception.checkedAdded", BREAKING, NON_BREAKING, null),
    METHOD_RUNTIME_EXCEPTION_ADDED("java.method.exception.runtimeAdded", NON_BREAKING, NON_BREAKING, null),
    METHOD_CHECKED_EXCEPTION_REMOVED("java.method.exception.checkedRemoved", BREAKING, NON_BREAKING, null),
    METHOD_RUNTIME_EXCEPTION_REMOVED("java.method.exception.runtimeRemoved", NON_BREAKING, NON_BREAKING, null),

    GENERICS_ELEMENT_NOW_PARAMETERIZED("java.generics.elementNowParameterized", NON_BREAKING, NON_BREAKING,
        POTENTIALLY_BREAKING),
    GENERICS_FORMAL_TYPE_PARAMETER_ADDED("java.generics.formalTypeParameterAdded", BREAKING, NON_BREAKING, null),
    GENERICS_FORMAL_TYPE_PARAMETER_REMOVED("java.generics.formalTypeParameterRemoved", BREAKING, NON_BREAKING, null),
    GENERICS_FORMAL_TYPE_PARAMETER_CHANGED("java.generics.formalTypeParameterChanged", BREAKING, NON_BREAKING, null);

    private final String code;
    private final EnumMap<CompatibilityType, DifferenceSeverity> classification;

    private Code(String code, DifferenceSeverity sourceSeverity, DifferenceSeverity binarySeverity,
        DifferenceSeverity semanticSeverity) {
        this.code = code;
        classification = new EnumMap<>(CompatibilityType.class);
        addClassification(SOURCE, sourceSeverity);
        addClassification(BINARY, binarySeverity);
        addClassification(SEMANTIC, semanticSeverity);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static Code fromCode(String code) {
        for (Code c : Code.values()) {
            if (c.code.equals(code)) {
                return c;
            }
        }

        return null;
    }

    public String code() {
        return code;
    }

    public Difference createDifference(@Nonnull Locale locale) {
        Message message = getMessages(locale).get(code);
        Difference.Builder bld = Difference.builder().withCode(code).withName(message.name)
            .withDescription(message.description);
        for (Map.Entry<CompatibilityType, DifferenceSeverity> e : classification.entrySet()) {
            bld.addClassification(e.getKey(), e.getValue());
        }

        return bld.build();
    }

    public Difference createDifference(@Nonnull Locale locale, Object[] params, Object... attachments) {
        Message message = getMessages(locale).get(code);
        String description = MessageFormat.format(message.description, params);
        Difference.Builder bld = Difference.builder().withCode(code).withName(message.name)
            .withDescription(description).addAttachments(attachments);

        for (Map.Entry<CompatibilityType, DifferenceSeverity> e : classification.entrySet()) {
            bld.addClassification(e.getKey(), e.getValue());
        }

        return bld.build();

    }

    private static class Message {
        final String name;
        final String description;

        private Message(String name, String description) {
            this.description = description;
            this.name = name;
        }
    }

    private static class Messages {

        private final ResourceBundle names;
        private final ResourceBundle descriptions;

        public Messages(Locale locale) {
            descriptions = ResourceBundle.getBundle("org.revapi.java.checks.descriptions", locale);
            names = ResourceBundle.getBundle("org.revapi.java.checks.names", locale);
        }

        Message get(String key) {
            String name = names.getString(key);
            String description = descriptions.getString(key);
            return new Message(name, description);
        }
    }

    private static WeakHashMap<Locale, WeakReference<Messages>> messagesCache = new WeakHashMap<>();

    private static synchronized Messages getMessages(Locale locale) {
        WeakReference<Messages> messageRef = messagesCache.get(locale);
        if (messageRef == null || messageRef.get() == null) {
            messageRef = new WeakReference<>(new Messages(locale));
            messagesCache.put(locale, messageRef);
        }

        return messageRef.get();
    }

    private void addClassification(CompatibilityType compatibilityType, DifferenceSeverity severity) {
        if (severity != null) {
            classification.put(compatibilityType, severity);
        }
    }
}
