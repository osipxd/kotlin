// JVM_DEFAULT: all

package a

interface DefaultImplsInInterface {
    val isVisible: Boolean
    val isPersistent: Boolean
    suspend fun show(mutatePriority: Int)
    fun dismiss()
    fun onDispose()
}