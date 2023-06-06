package team.jlm.refactoring

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
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
    val dependencyInfo: DependencyInfo
)

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
    val callTargetContainingClass: String?
) {
    override fun toString(): String {
        return "$callerContainingClass,$callerName,$calleeContainingClass,$calleeName,$callTargetContainingClass,$callTarget"
    }
}

fun detectCallChain(project: Project, el: ArrayList<DependencyInfo>): ArrayList<CallChain> {
    val psiElement = el.mapNotNull {
        val calleePsi = it.providerCache.getPsi(project)
        val callerPsi = it.userCache.getPsi(project)
        if (callerPsi is PsiMethod && calleePsi is PsiMethod) {
            val callerClass = PsiTreeUtil.getParentOfType(callerPsi, PsiClass::class.java)
            val calleeClass = PsiTreeUtil.getParentOfType(calleePsi, PsiClass::class.java)
            CallTracer(
                callerPsi.name,
                callerClass?.qualifiedName,
                calleePsi.name,
                calleeClass?.qualifiedName,
                it
            )
        } else {
            null
        }
    }
    val callChain = ArrayList<CallChain>(16)


    psiElement.forEach {
        val callee = it.dependencyInfo.providerCache.getPsi(project) as PsiMethod
        val calleeParams = callee.parameterList
        val calleeReturnType = callee.returnType ?: return@forEach

        val paramsMap = HashMap<PsiType, Int>()
        calleeParams.parameters.forEach { param ->
            paramsMap[param.type]?.let { paramsMap[param.type] = paramsMap[param.type]!! + 1 }
                ?: paramsMap.put(param.type, 1)
        }

        callee.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
                val method = expression?.resolveMethod()
                if (method != null) {
                    val methodParams = callee.parameterList
                    val methodReturnType = method.returnType!!
                    if ((calleeParams.parametersCount == methodParams.parametersCount) &&
                        (calleeReturnType == methodReturnType)
                    ) {
                        /**
                         * 函数参数检查，如果参数类型不符合绝对不可能是可处理的链式调用
                         */
                        val paramsCount = HashMap<PsiType, Int>(paramsMap)
                        var paramsTypeChecked = true
                        methodParams.parameters.forEach paramsLoop@{ param ->
                            if (paramsCount[param.type] != null && paramsCount[param.type]!! > 0) {
                                paramsCount[param.type] = paramsCount[param.type]!! - 1
                            } else {
                                paramsTypeChecked = false
                                return@paramsLoop
                            }
                        }

                        /**
                         * 入参赋值跟踪，如果callee的入参在传入targetFunc时改变，则不是可解决的链式调用
                         */
                        var paramsValueChecked = true
                        if (paramsTypeChecked) {
                            val scoop = LocalSearchScope(callee)
                            methodParams.parameters.forEach checkParamsValue@{ param ->
                                val reference = ReferencesSearch.search(param, scoop).findAll()
                                reference.forEach { ref ->
                                    val t =
                                        PsiTreeUtil.getParentOfType(ref.element, PsiAssignmentExpression::class.java)
                                            ?: PsiTreeUtil.getParentOfType(
                                                ref.element,
                                                PsiDeclarationStatement::class.java
                                            )
                                    if (t != null) {
                                        paramsValueChecked = false
                                        return@checkParamsValue
                                    }
                                }
                            }
                        }

                        /**
                         * 结果跟踪，如果targetFunc的结果在callee返回之前修改，则不是可处理的链式调用
                         */
                        if (paramsValueChecked) {
                            if (methodReturnType != PsiType.VOID) {
                                val possibleAssignment =
                                    PsiTreeUtil.getParentOfType(expression, PsiAssignmentExpression::class.java)
                                val possibleReturn =
                                    PsiTreeUtil.getParentOfType(expression, PsiReturnStatement::class.java)

                                /**
                                 * 说明是以下情况
                                 * return func.nextFunc(argv)
                                 */
                                if (possibleReturn != null) {
                                    callChain.add(
                                        CallChain(
                                            it.callerName,
                                            it.callerContainingClass,
                                            it.calleeName,
                                            it.calleeContainingClass,
                                            method.name,
                                            method.containingClass?.qualifiedName
                                        )
                                    )
                                } else {

                                    /**
                                     * 有可能是
                                     * Type value;
                                     * value = func.nextFunc(argv)
                                     * Some operator ...
                                     * return value
                                     */
                                    val variable: PsiVariable = if (possibleAssignment != null) {
                                        possibleAssignment.lExpression.reference!!.resolve() as PsiVariable
                                    }
                                    /**
                                     * 有可能是
                                     * Type value = func.nextFunc(argv)
                                     * Some operator  ......
                                     * return value
                                     */
                                    else {
                                        val assigment =
                                            PsiTreeUtil.getParentOfType(expression, PsiDeclarationStatement::class.java)
                                        assigment!!.firstChild as PsiVariable
                                    }
                                    val scoop = LocalSearchScope(callee)
                                    val reference = ReferencesSearch.search(variable, scoop).findAll()

                                    var resultChecked = true
                                    if (!reference.isEmpty()) {
                                        resultChecked = false
                                    }
                                    if (resultChecked) {
                                        callChain.add(
                                            CallChain(
                                                it.callerName,
                                                it.callerContainingClass,
                                                it.calleeName,
                                                it.calleeContainingClass,
                                                method.name,
                                                method.containingClass?.qualifiedName
                                            )
                                        )
                                    }
                                }
                            } else {
                                callChain.add(
                                    CallChain(
                                        it.callerName,
                                        it.callerContainingClass,
                                        it.calleeName,
                                        it.calleeContainingClass,
                                        method.name,
                                        method.containingClass?.qualifiedName
                                    )
                                )
                            }
                        }
                    }
                }
            }
        })

    }
    return callChain
}

//fun handlerCallChain(project: Project, callChain: CallChain) {
//}