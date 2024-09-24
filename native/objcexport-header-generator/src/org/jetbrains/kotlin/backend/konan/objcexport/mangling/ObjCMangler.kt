/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.StubRenderer
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf

/**
 * Represents mangling logic for each type of a stub:
 * [ObjCMethodMangler]: [ObjCMethod]
 * [ObjCClassNameMangler]: [ObjCInterface]
 *
 * Provided by translation implementation(either K1 or AA) and executed by [StubRenderer]
 */
interface ObjCMangler

interface ObjCMethodMangler : ObjCMangler {
    val mangleSelectors: () -> List<String>
    val mangleAttributes: () -> List<String>
}

interface ObjCClassNameMangler : ObjCMangler {
    val mangleName: () -> String
}

private val objCManglerKey = extrasKeyOf<ObjCMangler?>()

val ObjCExportStub.objCMangler: ObjCMangler?
    get() {
        return extras[objCManglerKey]
    }

var MutableExtras.objCMangler: ObjCMangler?
    get() = this[objCManglerKey]
    set(value) {
        this[objCManglerKey] = value
    }