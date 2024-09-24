package org.jetbrains.kotlin.backend.konan.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod

fun ObjCMethod.mangledSelectors(): List<String> {
    return (objCMangler as? ObjCMethodMangler)?.run {
        mangleSelectors()
    } ?: selectors
}

fun ObjCMethod.mangledAttributes(): List<String> {
    return (objCMangler as? ObjCMethodMangler)?.run { mangleAttributes() } ?: attributes
}

fun ObjCInterface.mangledName(): String {
    return (objCMangler as? ObjCClassNameMangler)?.run {
        mangleName()
    } ?: name
}