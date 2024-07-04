/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirDeprecationChecker
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny

object FirDeprecatedTypeChecker : FirResolvedTypeRefChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = typeRef.source ?: return
        if (source.kind is KtFakeSourceElementKind) return
        val symbol = typeRef.type.abbreviatedTypeOrSelf.classLikeLookupTagIfAny?.toSymbol(context.session) ?: return

        FirDeprecationChecker.reportApiStatusIfNeeded(source, symbol, context, reporter)
    }
}

