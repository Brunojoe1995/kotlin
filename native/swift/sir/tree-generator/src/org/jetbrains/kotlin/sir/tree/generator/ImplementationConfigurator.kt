/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.traverseParents2
import org.jetbrains.kotlin.sir.tree.generator.config.AbstractSwiftIrTreeImplementationConfigurator

object ImplementationConfigurator : AbstractSwiftIrTreeImplementationConfigurator() {

    fun configureImplementations(model: Model) {
        configure()
        generateDefaultImplementations(model.elements)
        configureAllImplementations()
    }

    private fun configure() = with(SwiftIrTree) {
        // Declare custom implementation classes, see org.jetbrains.kotlin.fir.tree.generator.ImplementationConfigurator
    }

    private fun configureAllImplementations() {
        // Use configureFieldInAllImplementations to customize certain fields in all implementation classes
        configureFieldInAllImplementations(
            field = "parent",
        ) {
            isMutable(it)
            isLateinit(it)
        }
    }
}