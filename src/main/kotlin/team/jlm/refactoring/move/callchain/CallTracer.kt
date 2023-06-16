package team.jlm.refactoring.move.callchain

import team.jlm.dependency.DependencyInfo

/**
 * Call tracer
 *
 * @property callerName 调用者函数名称
 * @property callerContainingClass 调用者所在类
 * @property calleeName 被调用者名称
 * @property calleeContainingClass 被调用者所在类
 * @property dependencyInfo 依赖信息 参见 [team.jlm.dependency.DependencyInfo]
 * @constructor Create empty Call tracer
 */
data class CallTracer(
    val callerName: String?,
    val callerContainingClass: String?,
    val calleeName: String?,
    val calleeContainingClass: String?,
    val dependencyInfo: DependencyInfo,
)