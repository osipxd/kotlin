/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Builder
import org.jetbrains.kotlin.name.Name
import kotlin.Nothing

class BuilderGeneratorImpl(session: FirSession) : BuilderGenerator<Builder>(
    session,
    builderModality = Modality.FINAL,
    getBuilder = { classSymbol, lombokService ->
        lombokService.getBuilder(classSymbol)
    },
    constructBuilderType = {
        it.constructClassLikeType(emptyArray(), isMarkedNullable = false)
    },
    getBuilderType = {
        it.defaultType()
    },
    getBuilderMethods = { builder: Builder, classSymbol: FirClassSymbol<*>, builderClassSymbol: FirRegularClassSymbol ->
        listOf(
            builderClassSymbol.createJavaMethod(
                Name.identifier(builder.buildMethodName),
                valueParameters = emptyList(),
                returnTypeRef = classSymbol.defaultType().toFirResolvedTypeRef(),
                visibility = builder.visibility.toVisibility(),
                modality = Modality.FINAL
            )
        )
    },
    completeBuilder = { classSymbol: FirClassSymbol<*>, builderClassSymbol: FirRegularClassSymbol, builderClassCache: FirCache<FirClassSymbol<*>, FirJavaClass?, Nothing?> ->
        superTypeRefs += listOf(session.builtinTypes.anyType)
    }
)
