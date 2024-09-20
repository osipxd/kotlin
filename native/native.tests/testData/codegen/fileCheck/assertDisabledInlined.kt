// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// ASSERTIONS_MODE: always-disable
// WITH_STDLIB

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

// CHECK-LABEL: define void @"kfun:#foo(kotlin.Boolean){}"
// CHECK-NOT: call void @"kfun:kotlin.AssertionError#<init>(kotlin.Any?){}"
inline fun foo(x: Boolean) = assert(x)

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-NOT: call void @"kfun:kotlin.AssertionError#<init>(kotlin.Any?){}"
fun box(): String {
    foo(false)
    return "OK"
}