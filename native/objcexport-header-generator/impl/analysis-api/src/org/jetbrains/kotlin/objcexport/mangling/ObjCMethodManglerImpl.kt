package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.objcexport.ObjCExportContext

internal class ObjCMethodManglerImpl : ObjCManglerCache<KaFunctionSymbol>() {

    override fun reserved(name: String): Boolean {
        return name in setOf(
            "retain", "release", "autorelease",
            "class", "superclass",
            "hash"
        )
    }

    fun mangle(selector: String, symbol: KaFunctionSymbol, context: ObjCExportContext): String {
        return getOrPut(context, symbol) {
            generateSequence(selector) { selector ->
                buildString {
                    append(selector)
                    if (symbol.valueParameters.isEmpty()) append('_') else insert(lastIndex, '_')
                }
            }
        }
    }

    override fun ObjCExportContext.conflict(first: KaFunctionSymbol, second: KaFunctionSymbol): Boolean {
        return !canHaveSameSelector(first, second, false)
    }
}