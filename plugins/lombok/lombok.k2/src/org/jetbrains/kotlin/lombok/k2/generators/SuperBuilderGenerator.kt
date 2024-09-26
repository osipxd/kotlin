/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.SuperBuilder
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.Nothing
import kotlin.collections.plusAssign

// See https://projectlombok.org/features/experimental/SuperBuilder for more details
class SuperBuilderGenerator(session: FirSession) : BuilderGenerator<SuperBuilder>(
    session,
    builderModality = Modality.ABSTRACT,
    getBuilder = { classSymbol, lombokService ->
        // There is also a build impl class, but it's private, and it's used only for internal purposes. Not relevant for API.
        lombokService.getSuperBuilder(classSymbol)
    },
    constructBuilderType = {
        it.constructClassLikeType(arrayOf(ConeStarProjection, ConeStarProjection), isMarkedNullable = false)
    },
    getBuilderType = {
        it.typeParameterSymbols[BUILDER_TYPE_PARAMETER_INDEX].defaultType
    },
    getBuilderMethods = { builder: SuperBuilder, classSymbol: FirClassSymbol<*>, builderClassSymbol: FirRegularClassSymbol ->
        val builderTypeParameterSymbols = builderClassSymbol.typeParameterSymbols

        listOf(
            builderClassSymbol.createJavaMethod(
                Name.identifier("self"),
                valueParameters = emptyList(),
                returnTypeRef = builderTypeParameterSymbols[BUILDER_TYPE_PARAMETER_INDEX].defaultType.toFirResolvedTypeRef(),
                visibility = Visibilities.Protected,
                modality = Modality.ABSTRACT
            ),
            builderClassSymbol.createJavaMethod(
                Name.identifier(builder.buildMethodName),
                valueParameters = emptyList(),
                returnTypeRef = builderTypeParameterSymbols[CLASS_TYPE_PARAMETER_INDEX].defaultType.toFirResolvedTypeRef(),
                visibility = Visibilities.Public,
                modality = Modality.ABSTRACT
            )
        )
    },
    completeBuilder = { classSymbol: FirClassSymbol<*>, builderClassSymbol: FirRegularClassSymbol, builderClassCache: FirCache<FirClassSymbol<*>, FirJavaClass?, Nothing?> ->
        val classTypeParameterSymbol = FirTypeParameterSymbol()
        val builderTypeParameterSymbol = FirTypeParameterSymbol()

        typeParameters += buildTypeParameter {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Synthetic.PluginFile
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            // Create a special name to avoid ambiguity with classes with the same name
            this.name = Name.special("<C>")
            symbol = classTypeParameterSymbol
            containingDeclarationSymbol = builderClassSymbol
            variance = Variance.INVARIANT
            isReified = false
            bounds += buildResolvedTypeRef {
                coneType = classSymbol.defaultType()
            }
        }
        typeParameters += buildTypeParameter {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Synthetic.PluginFile
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            this.name = Name.special("<B>")
            symbol = builderTypeParameterSymbol
            containingDeclarationSymbol = builderClassSymbol
            variance = Variance.INVARIANT
            isReified = false
            bounds += buildResolvedTypeRef {
                coneType = builderClassSymbol.constructType(
                    typeArguments = arrayOf(
                        classTypeParameterSymbol.defaultType,
                        builderTypeParameterSymbol.defaultType,
                    ),
                    isMarkedNullable = false
                )
            }
        }

        val superBuilderClass = classSymbol.resolvedSuperTypeRefs.mapNotNull { superTypeRef ->
            val superTypeSymbol = superTypeRef.toRegularClassSymbol(session) ?: return@mapNotNull null
            builderClassCache.getValue(superTypeSymbol)
        }.singleOrNull()
        val superBuilderTypeRef = superBuilderClass?.symbol?.constructType(
            typeArguments = arrayOf(
                classTypeParameterSymbol.defaultType,
                builderTypeParameterSymbol.defaultType,
            ),
            isMarkedNullable = false
        )?.toFirResolvedTypeRef() ?: session.builtinTypes.anyType

        superTypeRefs += listOf(superBuilderTypeRef)
    }
) {
    companion object {
        const val CLASS_TYPE_PARAMETER_INDEX = 0
        const val BUILDER_TYPE_PARAMETER_INDEX = 1
    }
}