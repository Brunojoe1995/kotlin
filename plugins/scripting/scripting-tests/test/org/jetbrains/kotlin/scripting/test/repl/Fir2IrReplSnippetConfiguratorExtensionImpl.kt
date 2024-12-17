/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.repl

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.jvm.originalSnippetValueSymbol
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.backend.Fir2IrVisitor
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.originalReplSnippetSymbol
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.bindSymbolToLookupTag
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrReplSnippet
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

val ScriptingHostConfigurationKeys.replStateObjectFqName by PropertiesCollection.key<String>()

object ScriptingGeneratorKey : GeneratedDeclarationKey() {
    override fun toString(): String {
        return "ScriptingGeneratorKey"
    }
}

class Fir2IrReplSnippetConfiguratorExtensionImpl(
    session: FirSession,
    hostConfiguration: ScriptingHostConfiguration,
) : Fir2IrReplSnippetConfiguratorExtension(session) {


    @OptIn(SymbolInternals::class, LookupTagInternals::class)
    val replStateObject: FirRegularClass by lazy(LazyThreadSafetyMode.PUBLICATION) {
        fun fqn2cid(s: String): ClassId {
            val fqn = FqName(s)
            return ClassId(fqn.parent(), fqn.shortName())
        }

        val classId = fqn2cid(hostConfiguration[ScriptingHostConfiguration.replStateObjectFqName]!!)
        (session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol)?.fir
            ?: run {
                val hashMapClassSymbol =
                    session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(
                        fqn2cid("kotlin.collections.HashMap")
                    )?.fullyExpandedClass(session) ?: error("HashMap class not found")
                val firReplStateSymbol = FirRegularClassSymbol(classId)
                val constructor = buildPrimaryConstructor {
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.FromOtherReplSnippet
                    status = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public
                    )
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    symbol = FirConstructorSymbol(classId)
                    returnTypeRef = FirImplicitTypeRefImplWithoutSource
                    dispatchReceiverType = firReplStateSymbol.constructType()
                }
                buildRegularClass {
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.FromOtherReplSnippet
                    this.name = classId.shortClassName
                    status = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public
                    )
                    classKind = ClassKind.OBJECT
                    symbol = firReplStateSymbol
                    superTypeRefs += hashMapClassSymbol.defaultType().toFirResolvedTypeRef(null)
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    scopeProvider = session.kotlinScopeProvider
                    declarations += constructor
                }.also {
                    (it.symbol.toLookupTag() as? ConeClassLikeLookupTagImpl)?.bindSymbolToLookupTag(session, it.symbol)
                    constructor.replaceReturnTypeRef(it.defaultType().toFirResolvedTypeRef())
                    val delegatingConstructorCall = buildDelegatedConstructorCall {
                        constructedTypeRef = it.superTypeRefs.singleOrNull() ?: error("No single super type for repl state found")
                        val superConstructorSymbol = hashMapClassSymbol.declaredMemberScope(session, memberRequiredPhase = null)
                            .getDeclaredConstructors()
                            .firstOrNull { it.valueParameterSymbols.isEmpty() }
                            ?: error("No arguments constructor for HashMap not found")
                        calleeReference = buildResolvedNamedReference {
                            name = superConstructorSymbol.name
                            resolvedSymbol = superConstructorSymbol
                        }
                        argumentList = FirEmptyArgumentList
                        isThis = false
                    }
                    constructor.replaceDelegatedConstructor(delegatingConstructorCall)
                }
            }
    }

    @OptIn(SymbolInternals::class, UnsafeDuringIrConstructionAPI::class)
    override fun Fir2IrComponents.prepareSnippet(fir2IrVisitor: Fir2IrVisitor, firReplSnippet: FirReplSnippet, irSnippet: IrReplSnippet) {
        val propertiesFromState = hashSetOf<Pair<FirReplSnippetSymbol, FirPropertySymbol>>()
        val functionsFromState = hashSetOf<Pair<FirReplSnippetSymbol, FirNamedFunctionSymbol>>()
        val classesFromState = hashSetOf<Pair<FirReplSnippetSymbol, FirRegularClassSymbol>>()

        CollectAccessToOtherState(
            session,
            propertiesFromState,
            functionsFromState,
            classesFromState
        ).visitReplSnippet(firReplSnippet)

        val usedOtherSnippets = HashSet<FirReplSnippetSymbol>()
        propertiesFromState.mapTo(usedOtherSnippets) { it.first }
        functionsFromState.mapTo(usedOtherSnippets) { it.first }
        classesFromState.mapTo(usedOtherSnippets) { it.first }
        usedOtherSnippets.remove(firReplSnippet.symbol)
        usedOtherSnippets.forEach {
            val packageFragment = declarationStorage.getIrExternalPackageFragment(it.packageFqName(), it.moduleData)
            classifierStorage.createAndCacheEarlierSnippetClass(it, packageFragment)
        }

        propertiesFromState.forEach { (snippetSymbol, propertySymbol) ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                declarationStorage.createAndCacheIrVariable(
                    propertySymbol.fir, irSnippet, IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                ).also { varFromOtherSnippet ->
                    irSnippet.variablesFromOtherSnippets.add(varFromOtherSnippet)
                    val field = originalSnippet.addField {
                        name = varFromOtherSnippet.name
                        type = varFromOtherSnippet.type
                        visibility = DescriptorVisibilities.PUBLIC
                        origin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                    }
                    varFromOtherSnippet.originalSnippetValueSymbol = field.symbol
                }
            }
        }

        functionsFromState.forEach { (snippetSymbol, functionSymbol) ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                declarationStorage.createAndCacheIrFunction(
                    functionSymbol.fir,
                    originalSnippet,
                    predefinedOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET,
                    fakeOverrideOwnerLookupTag = null,
                    allowLazyDeclarationsCreation = true
                ).run {
                    parent = originalSnippet
                    visibility = DescriptorVisibilities.PUBLIC
                    dispatchReceiverParameter = IrFactoryImpl.createValueParameter(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = origin,
                        kind = null,
                        name = SpecialNames.THIS,
                        type = originalSnippet.thisReceiver!!.type,
                        isAssignable = false,
                        symbol = IrValueParameterSymbolImpl(),
                        varargElementType = null,
                        isCrossinline = false,
                        isNoinline = false,
                        isHidden = false,
                    ).apply {
                        parent = this@run
                    }
                    irSnippet.capturingDeclarationsFromOtherSnippets.add(this)
                }
            }
        }

        classesFromState.forEach { (snippetSymbol, classSymbol) ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                classifierStorage.createAndCacheIrClass(
                    classSymbol.fir,
                    originalSnippet,
                    predefinedOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET,
                ).run {
                    parent = originalSnippet
                    visibility = DescriptorVisibilities.PUBLIC
                    createThisReceiverParameter()
                    classSymbol.fir.primaryConstructorIfAny(session)?.let {
                        declarationStorage.createAndCacheIrConstructor(it.fir, { this }, isLocal = false).also {
                            it.addValueParameter {
                                name = Name.special("<snippet>")
                                type = originalSnippet.defaultType
                            }
                        }
                    }
                    classSymbol.fir.declarations.forEach { declaration ->
                        when (declaration) {
                            is FirProperty -> declarationStorage.createAndCacheIrProperty(
                                declaration,
                                this,
                                predefinedOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                            )
                            else -> {}
                        }
                    }
                    irSnippet.capturingDeclarationsFromOtherSnippets.add(this)
                }
            }
        }
        val stateObject =
            if (replStateObject.origin is FirDeclarationOrigin.FromOtherReplSnippet) {
                classifierStorage.createAndCacheIrClass(replStateObject, irSnippet.parent).also { irReplStateObject ->
                    classifiersGenerator.processClassHeader(replStateObject, irReplStateObject)
                    declarationStorage.createAndCacheIrConstructor(
                        replStateObject.declarations.filterIsInstance<FirPrimaryConstructor>().first(),
                        { irReplStateObject }, isLocal = false
                    )
                    replStateObject.accept(fir2IrVisitor, null)
                    Unit
                }
            } else {
                lazyDeclarationsGenerator.createIrLazyClass(
                    replStateObject,
                    declarationStorage.getIrExternalPackageFragment(replStateObject.symbol.classId.packageFqName, session.moduleData),
                    IrClassSymbolImpl()
                )
            }

        irSnippet.stateObject = stateObject.symbol
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> Fir2IrReplSnippetConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}

private fun FirReplSnippetSymbol.getTargetClassId(): ClassId {
    // TODO: either make this transformation here but configure/retain target script name somewhere, or abstract it away, or make it on lowering
    val snippetTargetName = NameUtils.getScriptNameForFile(name.asStringStripSpecialMarkers().removePrefix("script-"))
    // TODO: take base package from snippet symbol (see todo elsewhere for adding it to the symbol)
    return ClassId(FqName.ROOT, snippetTargetName)
}

private class CollectAccessToOtherState(
    val session: FirSession,
    val properties: MutableSet<Pair<FirReplSnippetSymbol, FirPropertySymbol>>,
    val functions: MutableSet<Pair<FirReplSnippetSymbol, FirNamedFunctionSymbol>>,
    val classes: MutableSet<Pair<FirReplSnippetSymbol, FirRegularClassSymbol>>,
) : FirDefaultVisitorVoid() {

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    @OptIn(SymbolInternals::class)
    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        val resolvedSymbol = resolvedNamedReference.resolvedSymbol
        val symbol = when (resolvedSymbol) {
            is FirConstructorSymbol -> (resolvedSymbol.fir.returnTypeRef as? FirResolvedTypeRef)?.coneType?.toSymbol(session)
            else -> null
        } ?: resolvedSymbol
        val originalSnippet = symbol.fir.originalReplSnippetSymbol ?: return
        when (symbol) {
            is FirPropertySymbol -> properties.add(originalSnippet to symbol)
            is FirNamedFunctionSymbol -> functions.add(originalSnippet to symbol)
            is FirRegularClassSymbol -> classes.add(originalSnippet to symbol)
            else -> {}
        }
    }
}