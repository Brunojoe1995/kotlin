/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget.*

/**
 * Resolves a [TestSymbolTarget] to one or more results of type [R].
 */
abstract class TestSymbolTargetResolver<R> {
    fun resolveTarget(target: TestSymbolTarget): List<R> = when (target) {
        is PackageTarget -> resolvePackageTarget(target)
        is ClassTarget -> resolveClassTarget(target)
        is ScriptTarget -> resolveScriptTarget(target)
        is TypeAliasTarget -> resolveTypeAliasTarget(target)
        is CallableTarget -> resolveCallableTarget(target)
        is EnumEntryInitializerTarget -> resolveEnumEntryInitializerTarget(target)
        is SamConstructorTarget -> resolveSamConstructorTarget(target)
        is TargetWithOwner -> resolveTargetWithOwner(target)
    }

    private fun resolveTargetWithOwner(target: TargetWithOwner): List<R> {
        // We need to allow multiple owners in case there is an ambiguity which needs to culminate in multiple resolution results.
        val owners = resolveTarget(target.ownerTarget)
        if (owners.isEmpty()) {
            error("Couldn't resolve an owner for `$target`.")
        }

        return when (target) {
            is TypeParameterTarget -> resolveTypeParameterTarget(target, owners)
            is ValueParameterTarget -> resolveValueParameterTarget(target, owners)
        }
    }

    protected open fun resolvePackageTarget(target: PackageTarget): List<R> = unsupportedTarget(target)
    protected open fun resolveClassTarget(target: ClassTarget): List<R> = unsupportedTarget(target)
    protected open fun resolveScriptTarget(target: ScriptTarget): List<R> = unsupportedTarget(target)
    protected open fun resolveTypeAliasTarget(target: TypeAliasTarget): List<R> = unsupportedTarget(target)
    protected open fun resolveCallableTarget(target: CallableTarget): List<R> = unsupportedTarget(target)
    protected open fun resolveEnumEntryInitializerTarget(target: EnumEntryInitializerTarget): List<R> = unsupportedTarget(target)
    protected open fun resolveSamConstructorTarget(target: SamConstructorTarget): List<R> = unsupportedTarget(target)
    protected open fun resolveTypeParameterTarget(target: TypeParameterTarget, owners: List<R>): List<R> = unsupportedTarget(target)
    protected open fun resolveValueParameterTarget(target: ValueParameterTarget, owners: List<R>): List<R> = unsupportedTarget(target)

    private fun unsupportedTarget(target: TestSymbolTarget): Nothing =
        error("`${this::class.simpleName}` doesn't support `${target::class.simpleName}`.")
}
