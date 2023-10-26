package team.jlm.refactoring.callchain

/**
 * Call chain
 *
 * @property callerName 调用者函数名称
 * @property callerContainingClass 调用者所在类
 * @property calleeName 被调用者名称
 * @property calleeContainingClass 被调用者所在类
 * @property callTarget 链式调用目标
 * @property callTargetContainingClass 链式调用目标所在类
 * @constructor Create empty Call chain
 */
data class CallChain(
    val callerName: String?,
    val callerContainingClass: String?,
    val calleeName: String?,
    val calleeContainingClass: String?,
    val callTarget: String?,
    val callTargetContainingClass: String?,
) {
    override fun toString(): String {
        return "$callerContainingClass,$callerName,$calleeContainingClass,$calleeName,$callTargetContainingClass,$callTarget"
    }
}