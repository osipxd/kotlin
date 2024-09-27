/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.buildJavaClass
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
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

    override fun FirJavaClass.getSuperType(): FirTypeRef = session.builtinTypes.anyType

    @OptIn(SymbolInternals::class)
    override fun FirClassSymbol<*>.createBuilder(
        session: FirSession,
        name: Name,
        visibility: Visibility,
        modality: Modality,
        isStatic: Boolean,
        superTypeRef: FirTypeRef?,
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
            this.superTypeRefs += listOf(superTypeRef ?: session.builtinTypes.anyType)
            val effectiveVisibility = containingClass.effectiveVisibility.lowerBound(
                visibility.toEffectiveVisibility(this@createBuilder, forClass = true),
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

    override fun FirJavaClass.createBuilderMethods(builder: Builder) {
        declarations += symbol.createJavaMethod(
            Name.identifier(builder.buildMethodName),
            valueParameters = emptyList(),
            returnTypeRef = symbol.defaultType().toFirResolvedTypeRef(),
            visibility = builder.visibility.toVisibility(),
            modality = Modality.FINAL
        )
    }
}
