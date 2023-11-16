/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir.impl

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.util.transformInPlace
import org.jetbrains.kotlin.sir.visitors.SwiftIrTransformer
import org.jetbrains.kotlin.sir.visitors.SwiftIrVisitor

internal class SwiftIrEnumImpl(
    override val origin: Origin,
    override val attributes: MutableList<Attribute>,
    override val visibility: SwiftVisibility,
    override val name: String,
    override val declarations: MutableList<SwiftIrDeclaration>,
    override val cases: MutableList<SwiftIrEnumCase>,
) : SwiftIrEnum() {
    override lateinit var parent: SwiftIrDeclarationParent

    override fun <R, D> acceptChildren(visitor: SwiftIrVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
        cases.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: SwiftIrTransformer<D>, data: D) {
        declarations.transformInPlace(transformer, data)
        cases.transformInPlace(transformer, data)
    }
}
