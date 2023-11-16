/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.visitors.SwiftIrTransformer
import org.jetbrains.kotlin.sir.visitors.SwiftIrVisitor

/**
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.init]
 */
abstract class SwiftIrInit : SwiftIrElementBase(), SwiftIrCallable {
    abstract override val origin: Origin
    abstract override val attributes: List<Attribute>
    abstract override val visibility: SwiftVisibility
    abstract override var parent: SwiftIrDeclarationParent

    override fun <R, D> accept(visitor: SwiftIrVisitor<R, D>, data: D): R =
        visitor.visitInit(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : SwiftIrElement, D> transform(transformer: SwiftIrTransformer<D>, data: D): E =
        transformer.transformInit(this, data) as E
}
