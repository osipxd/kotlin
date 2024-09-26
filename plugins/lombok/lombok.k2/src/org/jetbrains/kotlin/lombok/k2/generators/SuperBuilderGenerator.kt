/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
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
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
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

    @OptIn(SymbolInternals::class)
    override fun createBuilderClass(classSymbol: FirClassSymbol<*>): FirJavaClass? {
        val javaClass = classSymbol.fir as? FirJavaClass ?: return null
        val superBuilder = javaClass.superTypeRefs.mapNotNull { superTypeRef ->
            val superTypeSymbol = superTypeRef.toRegularClassSymbol(session) ?: return@mapNotNull null
            builderClassCache.getValue(superTypeSymbol)
        }.singleOrNull()
        val builder = lombokService.getSuperBuilder(classSymbol) ?: return null
        val builderNameString = builder.builderClassName.replace("*", classSymbol.name.asString())
        val visibility = Visibilities.DEFAULT_VISIBILITY
        val builderClass = classSymbol.createBuilder(
            session,
            Name.identifier(builderNameString),
            visibility,
            Modality.FINAL,
            superBuilder = superBuilder,
        )?.apply {
            declarations += symbol.createDefaultJavaConstructor(visibility)
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
            createMethodsForFields(javaClass, builder)

        } ?: return null

        // There is also build impl class, but it's private, and it's used only for internal purposes. Not relevant for API.

        return builderClass
    }

    @OptIn(SymbolInternals::class)
    private fun FirClassSymbol<*>.createBuilder(
        session: FirSession,
        name: Name,
        visibility: Visibility,
        modality: Modality,
        superBuilder: FirJavaClass?,
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
            val superTypeRef = if (superBuilder != null) {
                superBuilder.symbol.constructType(
                    typeArguments = arrayOf(
                        typeParameters[0].symbol.defaultType.toFirResolvedTypeRef().coneType,
                        typeParameters[1].symbol.defaultType.toFirResolvedTypeRef().coneType,
                    ),
                    isNullable = false
                ).toFirResolvedTypeRef()
            } else {
                session.builtinTypes.anyType
            }
            this.superTypeRefs += listOf(superTypeRef)
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
}