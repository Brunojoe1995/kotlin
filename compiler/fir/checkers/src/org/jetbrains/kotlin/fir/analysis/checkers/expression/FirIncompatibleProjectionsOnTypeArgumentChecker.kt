/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkModifiersCompatibility
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression


object FirIncompatibleProjectionsOnTypeArgumentChecker : FirQualifiedAccessExpressionChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        for (it in expression.typeArguments) {
            checkModifiersCompatibility(it, context, reporter)
        }
    }
}
