package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.objcexport.ObjCExportContext

internal class ObjClassNameManglerImpl : ObjCManglerCache<KaClassSymbol>() {

    fun mangle(selector: String, func: KaClassSymbol, context: ObjCExportContext): String {
        return getOrPut(context, func) {
            generateSequence(selector) { selector ->
                buildString {
                    append(selector)
                    append('_')
                }
            }
        }
    }

    override fun ObjCExportContext.conflict(first: KaClassSymbol, second: KaClassSymbol): Boolean = true
}