/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.buildJavaClass
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Builder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class BuilderGenerator(session: FirSession) : BuilderGeneratorBase<Builder>(session) {
    override fun getBuilder(classSymbol: FirClassSymbol<*>): Builder? {
        return lombokService.getBuilder(classSymbol)
    }

    override fun ClassId.constructBuilderType(): ConeClassLikeType = constructClassLikeType(emptyArray(), isNullable = false)

    override fun FirRegularClassSymbol.getBuilderType(): ConeKotlinType = defaultType()

    @OptIn(SymbolInternals::class)
    override fun createBuilderClass(classSymbol: FirClassSymbol<*>): FirJavaClass? {
        val javaClass = classSymbol.fir as? FirJavaClass ?: return null
        val builder = getBuilder(classSymbol) ?: return null
        val builderName = Name.identifier(builder.builderClassName.replace("*", classSymbol.name.asString()))
        val visibility = builder.visibility.toVisibility()
        val builderClass = classSymbol.createJavaClass(
            session,
            builderName,
            visibility,
            Modality.FINAL,
            isStatic = true,
            superTypeRefs = listOf(session.builtinTypes.anyType)
        )?.apply {
            declarations += symbol.createDefaultJavaConstructor(visibility)
            declarations += symbol.createJavaMethod(
                Name.identifier(builder.buildMethodName),
                valueParameters = emptyList(),
                returnTypeRef = classSymbol.defaultType().toFirResolvedTypeRef(),
                visibility = visibility,
                modality = Modality.FINAL
            )
            createMethodsForFields(javaClass, builder)
        } ?: return null

        return builderClass
    }

    @OptIn(SymbolInternals::class)
    fun FirClassSymbol<*>.createJavaClass(
        session: FirSession,
        name: Name,
        visibility: Visibility,
        modality: Modality,
        isStatic: Boolean,
        superTypeRefs: List<FirTypeRef>,
    ): FirJavaClass? {
        val containingClass = this.fir as? FirJavaClass ?: return null
        val classId = containingClass.classId.createNestedClassId(name)
        return buildJavaClass {
            moduleData = containingClass.moduleData
            symbol = FirRegularClassSymbol(classId)
            this.name = name
            isFromSource = true
            this.visibility = visibility
            this.modality = modality
            this.isStatic = isStatic
            classKind = ClassKind.CLASS
            javaTypeParameterStack = containingClass.javaTypeParameterStack
            scopeProvider = JavaScopeProvider
            if (!isStatic) {
                typeParameters += containingClass.typeParameters.map {
                    buildOuterClassTypeParameterRef { symbol = it.symbol }
                }
            }
            this.superTypeRefs += superTypeRefs
            val effectiveVisibility = containingClass.effectiveVisibility.lowerBound(
                visibility.toEffectiveVisibility(this@createJavaClass, forClass = true),
                session.typeContext
            )
            isTopLevel = false
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                modality,
                effectiveVisibility
            ).apply {
                this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
                isCompanion = false
                isData = false
                isInline = false
                isFun = classKind == ClassKind.INTERFACE
            }
        }
    }
}
