package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.objcexport.ObjCExportContext

internal class SwiftAttributeManglerImpl : ObjCManglerCache<KaFunctionSymbol>() {

    override fun ObjCExportContext.conflict(first: KaFunctionSymbol, second: KaFunctionSymbol): Boolean {
        return !canHaveSameSelector(first, second, false)
    }

    fun mangle(swiftName: String, func: KaFunctionSymbol, context: ObjCExportContext): String {
        return getOrPut(context, func) {
            generateSequence(swiftName) { selector ->
                buildString {
                    append(selector)
                    insert(lastIndex - 1, '_')
                }
            }
        }
    }
}