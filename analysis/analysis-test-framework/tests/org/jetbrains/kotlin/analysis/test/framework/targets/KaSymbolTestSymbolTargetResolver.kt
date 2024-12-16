/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

internal class KaSymbolTestSymbolTargetResolver(private val session: KaSession) : TestSymbolTargetResolver<KaSymbol>() {
    override fun resolvePackageTarget(target: PackageTarget): List<KaSymbol> = with(session) {
        val symbol = findPackage(target.packageFqName) ?: error("Cannot find a symbol for the package `${target.packageFqName}`.")
        listOf(symbol)
    }

    override fun resolveClassTarget(target: ClassTarget): List<KaSymbol> = with(session) {
        val symbol = resolveClass(target.classId)
        listOf(symbol)
    }

    private fun KaSession.resolveClass(classId: ClassId): KaClassSymbol =
        findClass(classId) ?: error("Cannot find a symbol for the class `$classId`.")

    override fun resolveScriptTarget(target: ScriptTarget): List<KaSymbol> = with(session) {
        val script = target.file.script ?: error("The file `${target.file.name}` is not a script.")
        listOf(script.symbol)
    }

    override fun resolveTypeAliasTarget(target: TypeAliasTarget): List<KaSymbol> = with(session) {
        val symbol = findTypeAlias(target.classId) ?: error("Cannot find a symbol for the type alias `${target.classId}`.")
        listOf(symbol)
    }

    override fun resolveCallableTarget(target: CallableTarget): List<KaSymbol> = with(session) {
        val callableId = target.callableId
        val classId = callableId.classId

        val symbols = if (classId == null) {
            findTopLevelCallables(callableId.packageName, callableId.callableName).toList()
        } else {
            val classSymbol = resolveClass(classId)
            findMatchingCallableSymbols(callableId, classSymbol)
        }

        if (symbols.isEmpty()) {
            error("Cannot find a symbol for the callable `$callableId`.")
        }

        symbols
    }

    private fun KaSession.findMatchingCallableSymbols(callableId: CallableId, classSymbol: KaClassSymbol): List<KaCallableSymbol> {
        val declaredSymbols = classSymbol.combinedDeclaredMemberScope
            .callables(callableId.callableName)
            .toList()

        if (declaredSymbols.isNotEmpty()) {
            return declaredSymbols
        }

        // Fake overrides are absent in the declared member scope.
        return classSymbol.combinedMemberScope
            .callables(callableId.callableName)
            .filter { it.containingDeclaration == classSymbol }
            .toList()
    }

    override fun resolveEnumEntryInitializerTarget(target: EnumEntryInitializerTarget): List<KaSymbol> = with(session) {
        val enumEntryId = target.enumEntryId

        val classSymbol = enumEntryId.classId?.let { findClass(it) }
            ?: error("Cannot find a symbol for the enum class `${enumEntryId.classId}`.")

        require(classSymbol is KaNamedClassSymbol) { "`${enumEntryId.classId}` must be a named class." }
        require(classSymbol.classKind == KaClassKind.ENUM_CLASS) { "`${enumEntryId.classId}` must be an enum class." }

        val enumEntrySymbol = classSymbol.staticDeclaredMemberScope
            .callables(enumEntryId.callableName)
            .filterIsInstance<KaEnumEntrySymbol>().find {
                it.name == enumEntryId.callableName
            }
            ?: error("Cannot find a symbol for the enum entry `$enumEntryId`.")

        val initializerSymbol = enumEntrySymbol.enumEntryInitializer ?: error("`${enumEntryId.callableName}` must have an initializer.")
        listOf(initializerSymbol)
    }

    override fun resolveSamConstructorTarget(target: SamConstructorTarget): List<KaSymbol> = with(session) {
        val symbol = findClassLike(target.classId) ?: error("Cannot find a symbol for the class `${target.classId}`.")
        val samConstructor = symbol.samConstructor ?: error("Cannot find a symbol for the SAM constructor of `${target.classId}`.")
        listOf(samConstructor)
    }

    override fun resolveTypeParameterTarget(target: TypeParameterTarget, owners: List<KaSymbol>): List<KaSymbol> = with(session) {
        val owner = owners.singleOrNull() as? KaDeclarationSymbol
            ?: error("Expected a single `${KaDeclarationSymbol::class.simpleName}` owner for `$target`, but found: $owners.")

        val parameterSymbol = owner.typeParameters.find { it.name == target.name }
            ?: error("Cannot find a type parameter `${target.name}` in the owner `$owner`. Found type parameters: ${owner.typeParameters}.")

        listOf(parameterSymbol)
    }

    override fun resolveValueParameterTarget(target: ValueParameterTarget, owners: List<KaSymbol>): List<KaSymbol> = with(session) {
        val owner = owners.singleOrNull() as? KaFunctionSymbol
            ?: error("Expected a single `${KaFunctionSymbol::class.simpleName}` owner for `$target`, but found: $owners.")

        val parameterSymbol = owner.valueParameters.find { it.name == target.name }
            ?: error("Cannot find a value parameter `${target.name}` in the owner `$owner`. Found value parameters: ${owner.valueParameters}.")

        listOf(parameterSymbol)
    }
}
