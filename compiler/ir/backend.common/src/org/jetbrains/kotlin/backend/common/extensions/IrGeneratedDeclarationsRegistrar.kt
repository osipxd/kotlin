/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite

abstract class IrGeneratedDeclarationsRegistrar {
    abstract fun addMetadataVisibleAnnotationsToElement(declaration: IrDeclaration, annotations: List<IrConstructorCall>)

    fun addMetadataVisibleAnnotationsToElement(declaration: IrDeclaration, vararg annotations: IrConstructorCall) {
        addMetadataVisibleAnnotationsToElement(declaration, annotations.toList())
    }

    abstract fun registerFunctionAsMetadataVisible(irFunction: IrSimpleFunction)
    abstract fun registerConstructorAsMetadataVisible(irConstructor: IrConstructor)

    // TODO: KT-63881
    // abstract fun registerPropertyAsMetadataVisible(irProperty: IrProperty)

    abstract fun <T : Any> addCustomMetadataExtension(
        irClass: IrClass,
        extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Class, T>,
        value: T
    )

    abstract fun <T : Any> addCustomMetadataExtension(
        irClass: IrConstructor,
        extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Constructor, T>,
        value: T
    )

    abstract fun <T : Any> addCustomMetadataExtension(
        irClass: IrSimpleFunction,
        extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Function, T>,
        value: T
    )

    abstract fun <T : Any> addCustomMetadataExtension(
        irClass: IrProperty,
        extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Property, T>,
        value: T
    )

    abstract fun <T : Any> getCustomMetadataExtension(
        irClass: IrClass,
        extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Class, T>,
    ): T?

    abstract fun <T : Any> getCustomMetadataExtension(
        irConstructor: IrConstructor,
        extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Constructor, T>,
    ): T?

    abstract fun <T : Any> getCustomMetadataExtension(
        irFunction: IrSimpleFunction,
        extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Function, T>,
    ): T?

    abstract fun <T : Any> getCustomMetadataExtension(
        irProperty: IrProperty,
        extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Property, T>,
    ): T?
}
