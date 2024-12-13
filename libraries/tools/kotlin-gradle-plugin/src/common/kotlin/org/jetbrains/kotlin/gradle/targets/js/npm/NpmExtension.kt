/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

@Deprecated(
    "Use JsNpmExtension instead",
    ReplaceWith(
        "JsNpmExtension",
        "org.jetbrains.kotlin.gradle.targets.js.npm.JsNpmExtension"
    )
)
open class NpmExtension(
    project: Project,
    nodeJsRoot: NodeJsRootExtension,
) : AbstractNpmExtension(
    project,
    nodeJsRoot
) {
    companion object {
        const val EXTENSION_NAME: String = JsNpmExtension.EXTENSION_NAME

        operator fun get(project: Project): JsNpmExtension =
            JsNpmExtension.Companion.get(project)
    }
}