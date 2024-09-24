package org.jetbrains.kotlin.objcexport.mangling

class KtObjCExportMangling {
    internal val swiftAttributeMangler = SwiftAttributeManglerImpl()
    internal val methodMangler = ObjCMethodManglerImpl()
    internal val classNameMangler = ObjClassNameManglerImpl()
}