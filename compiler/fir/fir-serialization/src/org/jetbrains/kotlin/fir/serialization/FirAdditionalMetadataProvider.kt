/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.protobuf.MessageLite

abstract class FirAdditionalMetadataProvider {
    abstract fun findGeneratedAnnotationsFor(declaration: FirDeclaration): List<FirAnnotation>
    abstract fun hasGeneratedAnnotationsFor(declaration: FirDeclaration): Boolean

    abstract fun findMetadataExtensionsFor(klass: FirClass): List<MetadataExtensionDescription<ProtoBuf.Class, *>>
    abstract fun findMetadataExtensionsFor(constructor: FirConstructor): List<MetadataExtensionDescription<ProtoBuf.Constructor, *>>
    abstract fun findMetadataExtensionsFor(function: FirFunction): List<MetadataExtensionDescription<ProtoBuf.Function, *>>
    abstract fun findMetadataExtensionsFor(property: FirProperty): List<MetadataExtensionDescription<ProtoBuf.Property, *>>

    data class MetadataExtensionDescription<ContainingType : MessageLite, Type : Any>(
        val extension: GeneratedMessageLite.GeneratedExtension<ContainingType, Type>,
        val value: Type
    )
}
