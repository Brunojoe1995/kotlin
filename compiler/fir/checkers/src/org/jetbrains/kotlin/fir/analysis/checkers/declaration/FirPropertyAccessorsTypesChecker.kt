/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.findClosestClassOrObject
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.canHaveAbstractDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

object FirPropertyAccessorsTypesChecker : FirPropertyChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        checkGetter(declaration, context, reporter)
        checkSetter(declaration, context, reporter)
    }

    private fun checkGetter(property: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val getter = property.getter ?: return
        val propertyType = property.returnTypeRef.coneType

        checkAccessorForDelegatedProperty(property, getter, context, reporter)

        if (getter.isImplicitDelegateAccessor()) {
            return
        }
        if (getter.visibility != property.visibility) {
            reporter.reportOn(getter.source, FirErrors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, context)
        }
        if (property.symbol.callableId.classId != null && getter.body != null && property.delegate == null) {
            if (isLegallyAbstract(property, context)) {
                reporter.reportOn(getter.source, FirErrors.ABSTRACT_PROPERTY_WITH_GETTER, context)
            }
        }
        val getterReturnTypeRef = getter.returnTypeRef
        if (getterReturnTypeRef.source?.kind is KtFakeSourceElementKind) {
            return
        }
        val getterReturnType = getterReturnTypeRef.coneType
        if (propertyType is ConeErrorType || getterReturnType is ConeErrorType) {
            return
        }
        if (getterReturnType != property.returnTypeRef.coneType) {
            val getterReturnTypeSource = getterReturnTypeRef.source
            reporter.reportOn(getterReturnTypeSource, FirErrors.WRONG_GETTER_RETURN_TYPE, propertyType, getterReturnType, context)
        }
    }

    private fun checkSetter(property: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val setter = property.setter ?: return
        val propertyType = property.returnTypeRef.coneType

        if (property.isVal) {
            reporter.reportOn(setter.source, FirErrors.VAL_WITH_SETTER, context)
        }
        checkAccessorForDelegatedProperty(property, setter, context, reporter)

        if (setter.isImplicitDelegateAccessor()) {
            return
        }
        val visibilityCompareResult = setter.visibility.compareTo(property.visibility)
        if (visibilityCompareResult == null || visibilityCompareResult > 0) {
            reporter.reportOn(setter.source, FirErrors.SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY, context)
        }
        if (property.symbol.callableId.classId != null && property.delegate == null) {
            val isLegallyAbstract = isLegallyAbstract(property, context)
            if (setter.visibility == Visibilities.Private && property.visibility != Visibilities.Private) {
                if (isLegallyAbstract) {
                    reporter.reportOn(setter.source, FirErrors.PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY, context)
                } else if (!property.isEffectivelyFinal()) {
                    reporter.reportOn(setter.source, FirErrors.PRIVATE_SETTER_FOR_OPEN_PROPERTY, context)
                }
            }
            if (isLegallyAbstract && setter.body != null) {
                reporter.reportOn(setter.source, FirErrors.ABSTRACT_PROPERTY_WITH_SETTER, context)
            }
        }

        val valueSetterParameter = setter.valueParameters.first()
        if (valueSetterParameter.isVararg) {
            return
        }
        val valueSetterType = valueSetterParameter.returnTypeRef.coneType
        val valueSetterTypeSource = valueSetterParameter.returnTypeRef.source
        if (propertyType is ConeErrorType || valueSetterType is ConeErrorType) {
            return
        }

        if (valueSetterType.withAttributes(ConeAttributes.Empty) != propertyType.withAttributes(ConeAttributes.Empty) && !valueSetterType.hasError()) {
            reporter.reportOn(valueSetterTypeSource, FirErrors.WRONG_SETTER_PARAMETER_TYPE, propertyType, valueSetterType, context)
        }

        val setterReturnType = setter.returnTypeRef.coneType.fullyExpandedType(context.session)

        if (!setterReturnType.isUnit) {
            reporter.reportOn(setter.returnTypeRef.source, FirErrors.WRONG_SETTER_RETURN_TYPE, context)
        }
    }

    private fun checkAccessorForDelegatedProperty(
        property: FirProperty,
        accessor: FirPropertyAccessor,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (property.delegateFieldSymbol != null && accessor.body != null &&
            accessor.source?.kind != KtFakeSourceElementKind.DelegatedPropertyAccessor
        ) {
            reporter.reportOn(accessor.source, FirErrors.ACCESSOR_FOR_DELEGATED_PROPERTY, context)
        }
    }

    private fun FirPropertyAccessor.isImplicitDelegateAccessor(): Boolean =
        source?.kind == KtFakeSourceElementKind.DelegatedPropertyAccessor

    private fun isLegallyAbstract(property: FirProperty, context: CheckerContext): Boolean {
        return property.isAbstract && context.findClosestClassOrObject().let { it is FirRegularClass && it.canHaveAbstractDeclaration }
    }
}
