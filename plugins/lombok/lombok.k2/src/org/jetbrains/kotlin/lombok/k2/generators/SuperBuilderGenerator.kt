/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.buildJavaClass
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.SuperBuilder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.collections.plusAssign

class SuperBuilderGenerator(session: FirSession) : BuilderGeneratorBase<SuperBuilder>(session) {
    override fun getBuilder(classSymbol: FirClassSymbol<*>): SuperBuilder? {
        return lombokService.getSuperBuilder(classSymbol)
    }

    override fun ClassId.constructBuilderType(): ConeClassLikeType {
        return constructClassLikeType(arrayOf(ConeStarProjection, ConeStarProjection), isNullable = false)
    }

    override fun FirRegularClassSymbol.getBuilderType(): ConeKotlinType = typeParameterSymbols[1].defaultType

    override fun FirJavaClass.getSuperType(): FirTypeRef {
        val superClass = superTypeRefs.mapNotNull { superTypeRef ->
            val superTypeSymbol = superTypeRef.toRegularClassSymbol(session) ?: return@mapNotNull null
            builderClassCache.getValue(superTypeSymbol)
        }.singleOrNull()
        return superClass?.symbol?.let {
            it.constructType(
                typeArguments = arrayOf(
                    it.typeParameterSymbols[0].defaultType.toFirResolvedTypeRef().coneType,
                    it.typeParameterSymbols[1].defaultType.toFirResolvedTypeRef().coneType,
                ),
                isNullable = false
            ).toFirResolvedTypeRef()
        } ?: session.builtinTypes.anyType
    }

    // There is also build impl class, but it's private, and it's used only for internal purposes. Not relevant for API.

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
        val classSymbol = FirRegularClassSymbol(classId)
        return buildJavaClass {
            moduleData = containingClass.moduleData
            symbol = classSymbol
            this.name = name
            isFromSource = true
            this.visibility = visibility
            this.modality = modality
            this.isStatic = true
            classKind = ClassKind.CLASS
            javaTypeParameterStack = containingClass.javaTypeParameterStack
            scopeProvider = JavaScopeProvider
            val classTypeParameterSymbol = FirTypeParameterSymbol()
            val builderTypeParameterSymbol = FirTypeParameterSymbol()
            typeParameters += buildTypeParameter {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Synthetic.PluginFile
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                this.name = Name.identifier("C")
                symbol = classTypeParameterSymbol
                containingDeclarationSymbol = classSymbol
                variance = Variance.INVARIANT
                isReified = false
                bounds += buildResolvedTypeRef {
                    coneType = defaultType().toFirResolvedTypeRef().coneType
                }
            }
            typeParameters += buildTypeParameter {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Synthetic.PluginFile
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                this.name = Name.identifier("B")
                symbol = builderTypeParameterSymbol
                containingDeclarationSymbol = classSymbol
                variance = Variance.INVARIANT
                isReified = false
                bounds += buildResolvedTypeRef {
                    coneType = classSymbol.constructType(
                        typeArguments = arrayOf(
                            ConeTypeParameterTypeImpl(classTypeParameterSymbol.toLookupTag(), isNullable = false),
                            ConeTypeParameterTypeImpl(builderTypeParameterSymbol.toLookupTag(), isNullable = false),
                        ),
                        isNullable = false
                    )
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

    override fun FirJavaClass.createBuilderMethods(builder: SuperBuilder) {
        val visibility = builder.visibility.toVisibility()
        declarations += symbol.createJavaMethod(
            Name.identifier("self"),
            valueParameters = emptyList(),
            returnTypeRef = symbol.typeParameterSymbols[1].defaultType.toFirResolvedTypeRef(),
            visibility = visibility,
            modality = Modality.FINAL
        )
        declarations += symbol.createJavaMethod(
            Name.identifier(builder.buildMethodName),
            valueParameters = emptyList(),
            returnTypeRef = symbol.typeParameterSymbols[0].defaultType.toFirResolvedTypeRef(),
            visibility = visibility,
            modality = Modality.FINAL
        )
    }
}