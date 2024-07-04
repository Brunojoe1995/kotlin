/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirLiteralExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirStringConcatenationCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.ConstantValueKind

object RedundantInterpolationPrefixCheckerConcatenation : FirStringConcatenationCallChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(expression: FirStringConcatenationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.interpolationPrefix == "$") {
            reporter.reportOn(expression.source, FirErrors.REDUNDANT_INTERPOLATION_PREFIX, context)
        }
    }
}

object RedundantInterpolationPrefixCheckerLiteral : FirLiteralExpressionChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(expression: FirLiteralExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val prefix = expression.prefix
        if (expression.kind == ConstantValueKind.String && !prefix.isNullOrEmpty()) {
            val value = expression.source.text?.drop(prefix.length) ?: return
            // approximation of interpolated values: $ followed either by start of an identifier, or braces
            if (!Regex("""[^\\]\$(\w|\{|`[^`])""").containsMatchIn(value)) {
                reporter.reportOn(expression.source, FirErrors.REDUNDANT_INTERPOLATION_PREFIX, context)
            }
        }
    }
}
