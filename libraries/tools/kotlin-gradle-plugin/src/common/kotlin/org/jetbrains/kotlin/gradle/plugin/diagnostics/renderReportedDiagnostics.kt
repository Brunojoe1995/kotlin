/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalStyle.blue
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalStyle.bold
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalStyle.green
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalStyle.italic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalStyle.red
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalStyle.yellow
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*

internal fun renderReportedDiagnostics(
    diagnostics: Collection<ToolingDiagnostic>,
    logger: Logger,
    renderingOptions: ToolingDiagnosticRenderingOptions,
) {
    for (diagnostic in diagnostics) {
        renderReportedDiagnostic(diagnostic, logger, renderingOptions)
    }
}

internal fun renderReportedDiagnostic(
    diagnostic: ToolingDiagnostic,
    logger: Logger,
    renderingOptions: ToolingDiagnosticRenderingOptions
) {
    when (diagnostic.severity) {
        WARNING -> logger.warn("w: ${diagnostic.render(renderingOptions)}\n")

        ERROR -> logger.error("e: ${diagnostic.render(renderingOptions)}\n")

        FATAL -> throw diagnostic.createAnExceptionForFatalDiagnostic(renderingOptions)
    }
}

internal fun ToolingDiagnostic.createAnExceptionForFatalDiagnostic(
    renderingOptions: ToolingDiagnosticRenderingOptions
): InvalidUserCodeException {
    // NB: override showStacktrace to false, because it will be shown as 'cause' anyways
    val message = render(renderingOptions, showStacktrace = false)
    if (throwable != null)
        throw InvalidUserCodeException(message, throwable)
    else
        throw InvalidUserCodeException(message)
}

private fun ToolingDiagnostic.render(
    renderingOptions: ToolingDiagnosticRenderingOptions,
    showStacktrace: Boolean = renderingOptions.showStacktrace,
): String = buildString {
    val styledDiagnostic = styled()
    with(renderingOptions) {
        if (!useParsableFormat && showSeverityEmoji) {
            appendLine(styledDiagnostic.name)
        }

        // Main message
        if (useParsableFormat) {
            appendLine(this@render)
        } else {
            appendLine(styledDiagnostic.message)
            styledDiagnostic.solution?.let {
                appendLine(it)
            }
            styledDiagnostic.documentation?.let {
                appendLine(it)
            }
        }

        // Additional stacktrace, if requested
        if (showStacktrace) renderStacktrace(this@render.throwable, useParsableFormat)

        // Separator, if in verbose mode
        if (useParsableFormat) appendLine(DIAGNOSTIC_SEPARATOR)
    }
}

private fun StringBuilder.renderStacktrace(throwable: Throwable?, useParsableFormatting: Boolean) {
    if (throwable == null) return
    appendLine()
    appendLine(DIAGNOSTIC_STACKTRACE_START)
    appendLine(throwable.stackTraceToString().trim().prependIndent("    "))
    if (useParsableFormatting) appendLine(DIAGNOSTIC_STACKTRACE_END_SEPARATOR)
}

/**
 * Provides ANSI escape codes for applying various styles and colors to terminal text.
 * Includes constants for commonly used styles and colors as well as extension functions
 * to easily style strings.
 *
 * The object is designed to simplify the process of formatting text for terminal output.
 * Reset codes are automatically appended after applying styles to ensure proper formatting.
 */
object TerminalStyle {
    // ANSI color and style constants
    const val RESET = "\u001B[0m"
    const val YELLOW = "\u001B[33m"
    const val GREEN = "\u001B[32m"
    const val BOLD = "\u001B[1m"
    const val ITALIC = "\u001B[3m"
    const val RED = "\u001B[31m"
    const val BLUE = "\u001B[34m"

    // Convenience extension functions for styling
    fun String.bold() = "$BOLD$this$RESET"
    fun String.italic() = "$ITALIC$this$RESET"
    fun String.yellow() = "$YELLOW$this$RESET"
    fun String.green() = "$GREEN$this$RESET"
    fun String.red() = "$RED$this$RESET"
    fun String.blue() = "$BLUE$this$RESET"
}

/**
 * Represents diagnostic icons used to indicate the severity level of diagnostics.
 *
 * @property icon The visual representation of the diagnostic icon, such as a warning or error symbol.
 */
enum class DiagnosticIcon(val icon: String) {
    WARNING("⚠️"),
    ERROR("❌"),
}

/**
 * Represents a diagnostic message in a styled form, intended for tools and plugins.
 *
 * Provides information about the diagnostic, including a name, a message describing the issue,
 * an optional solution, and optional documentation references.
 *
 * @property name The name of the diagnostic, providing a brief identifier for the issue.
 * @property message The descriptive message explaining the diagnostic or issue.
 * @property solution An optional proposed solution or recommended steps to resolve the issue.
 * @property documentation Optional documentation reference offering additional context or resources.
 */
interface StyledToolingDiagnostic {
    val name: String
    val message: String
    val solution: String?
    val documentation: String?
}

/**
 * This class provides a styled implementation of the `StyledToolingDiagnostic` interface.
 *
 * It wraps a `ToolingDiagnostic` to present its fields in a styled format
 * through methods and properties like `name`, `message`, `solution`, and `documentation`.
 *
 * @constructor Creates an instance of `StyledToolingDiagnosticImp` using a `ToolingDiagnostic`.
 * @param diagnostic The `ToolingDiagnostic` instance containing raw diagnostic data.
 *
 * The following details are styled:
 * - The `name` is constructed with a severity-based icon and a colored identifier name.
 * - The `message` is formatted with bold text.
 * - The `solution` is presented in a formatted list or as a single-line message, with green styling.
 * - The `documentation` is styled in blue, if available.
 *
 * Severity-based styling:
 * - `WARNING`: Yellow text styling for the identifier name.
 * - `ERROR` or `FATAL`: Red text styling for the identifier name.
 *
 * Solution presentation:
 * - If one solution is present, it is labeled "Solution" and italicized.
 * - If multiple solutions exist, each is listed with a bullet point, italicized, and styled in green.
 */
private class StyledToolingDiagnosticImp(private val diagnostic: ToolingDiagnostic) : StyledToolingDiagnostic {
    override val name: String get() = buildName()
    override val message: String get() = buildMessage()
    override val solution: String? get() = buildSolution()
    override val documentation: String? get() = buildDocumentation()

    private fun buildName(): String {
        val icon = when (diagnostic.severity) {
            WARNING -> DiagnosticIcon.WARNING
            else -> DiagnosticIcon.ERROR
        }
        return buildString {
            append(icon.icon)
            append(" ")
            append(diagnostic.identifier.displayName.bold().let {
                when (diagnostic.severity) {
                    WARNING -> it.yellow()
                    ERROR, FATAL -> it.red()
                }
            })
        }
    }

    private fun buildMessage(): String =
        diagnostic.message.bold()

    private fun buildSolution(): String? {
        val solutions = diagnostic.solutions
        return solutions.takeIf { it.isNotEmpty() }?.let {
            buildString {
                when {
                    solutions.size == 1 -> {
                        appendLine("Solution:".bold().green())
                        append(solutions.single().italic().green())
                    }
                    else -> {
                        appendLine("Solutions:".bold().green())
                        solutions.forEachIndexed { index, solution ->
                            append(" • ${solution.italic()}".green())
                            if (index < solutions.size - 1) appendLine()
                        }
                    }
                }
            }
        }
    }

    private fun buildDocumentation(): String? =
        diagnostic.documentation?.additionalUrlContext?.blue()
}

private fun ToolingDiagnostic.styled(): StyledToolingDiagnostic = StyledToolingDiagnosticImp(this)

internal const val DIAGNOSTIC_SEPARATOR = "#diagnostic-end"
internal const val DIAGNOSTIC_STACKTRACE_START = "Stacktrace:"
internal const val DIAGNOSTIC_STACKTRACE_END_SEPARATOR = "#stacktrace-end"
