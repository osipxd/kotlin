package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.objcexport.ObjCExportContext

/**
 * Instance of [ObjCManglerCache] holds references to symbols which names potentially must be mangled.
 *
 * Every group of symbols has it's own implementation:
 * - [ObjCMethodManglerImpl]
 * - [SwiftAttributeManglerImpl]
 *
 * See K1 implementation [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.Mapping]
 */
internal abstract class ObjCManglerCache<in T : Any> {

    private val elementToStringName = mutableMapOf<T, String>()
    private val nameToElements = mutableMapOf<String, MutableList<T>>()

    abstract fun ObjCExportContext.conflict(first: T, second: T): Boolean
    open fun reserved(name: String) = false

    /**
     * [nameCandidates] must returns sequence which consists of:
     * 1. String mangled first element, it will be used for symbol which met for the first time during traversal
     * 2. All other elements, where every next element consists of `previous + _`
     */
    protected fun getOrPut(context: ObjCExportContext, element: T, nameCandidates: () -> Sequence<String>): String {

        val cached = getIfAssigned(element)
        if (cached != null) return cached

        nameCandidates().forEach {
            if (reserved(it)) return it
            if (with(context) { tryAssign(element, it) }) {
                return it
            }
        }

        error("name candidates run out")
    }

    private fun getIfAssigned(element: T): String? = elementToStringName[element]

    private fun ObjCExportContext.tryAssign(element: T, name: String): Boolean {

        if (element in elementToStringName) error(element)
        if (nameToElements[name].orEmpty().any { conflict(element, it) }) return false

        nameToElements.getOrPut(name) { mutableListOf() } += element
        elementToStringName[element] = name

        return true
    }
}