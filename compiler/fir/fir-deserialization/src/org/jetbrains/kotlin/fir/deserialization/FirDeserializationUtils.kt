/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.protobuf.ByteString
import org.jetbrains.kotlin.protobuf.MessageLiteOrBuilder
import java.lang.reflect.Field

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

internal fun VersionRequirement.Companion.create(proto: MessageLiteOrBuilder, context: FirDeserializationContext): List<VersionRequirement> =
    create(proto, context.nameResolver, context.versionRequirementTable)

internal val classUnknownFieldsAccessor: Field = run {
    val clazz = ProtoBuf.Class::class.java
    clazz.getDeclaredField("unknownFields").also { it.isAccessible = true }
}

internal val ProtoBuf.Class.unknownFields: ByteString?
    get() = classUnknownFieldsAccessor.get(this) as ByteString?


internal val constructorUnknownFieldsAccessor: Field = run {
    val clazz = ProtoBuf.Constructor::class.java
    clazz.getDeclaredField("unknownFields").also { it.isAccessible = true }
}

internal val ProtoBuf.Constructor.unknownFields: ByteString?
    get() = constructorUnknownFieldsAccessor.get(this) as ByteString?


internal val functionUnknownFieldsAccessor: Field = run {
    val clazz = ProtoBuf.Function::class.java
    clazz.getDeclaredField("unknownFields").also { it.isAccessible = true }
}

internal val ProtoBuf.Function.unknownFields: ByteString?
    get() = functionUnknownFieldsAccessor.get(this) as ByteString?


internal val propertyUnknownFieldsAccessor: Field = run {
    val clazz = ProtoBuf.Property::class.java
    clazz.getDeclaredField("unknownFields").also { it.isAccessible = true }
}

internal val ProtoBuf.Property.unknownFields: ByteString?
    get() = propertyUnknownFieldsAccessor.get(this) as ByteString?

