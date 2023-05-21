package team.jlm.refactoring

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import team.jlm.utils.psi.getAllJavaFilesInProject
import team.jlm.utils.psi.getOuterClass

/**
 * Deprecated method
 *
 * @property containingClass 使用弃用方法的类
 * @property methodName 使用弃用方法的方法
 * @property deprecatedCallContainingClass 弃用方法所属类
 * @property deprecatedCallContainingMethod 弃用方法
 * @property lineNumber 所在行数
 * @property type 类型
 * @property fileUrl 文件路径
 * @constructor Create empty Deprecated method
 */
data class DeprecatedMethod(
    val containingClass: String?,
    val methodName: String?,
    val deprecatedCallContainingClass: String?,
    val deprecatedCallContainingMethod: String?,
    val lineNumber: String?,
    val type: String,
    val fileUrl: String
) {
    override fun toString(): String {
        return "$containingClass,$methodName,$deprecatedCallContainingClass,$deprecatedCallContainingMethod,$type"
    }
}

fun handleDeprecatedMethod(project: Project): HashMap<String, ArrayList<DeprecatedMethod>> {
    val javaFileList = getAllJavaFilesInProject(project)
    val deprecatedMap = HashMap<String, ArrayList<DeprecatedMethod>>()
    javaFileList.forEach { javaFile ->
        javaFile.accept((object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression?) {
                val callMethod = expression?.resolveMethod()
                if (callMethod != null && callMethod.isDeprecated) {
                    val thisMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod::class.java)
                    var thisField: PsiField? = null
                    if (thisMethod == null) {
                        thisField = PsiTreeUtil.getParentOfType(expression, PsiField::class.java)
                    }
                    val callStatement = PsiTreeUtil.getParentOfType(expression, PsiElement::class.java)
                    val document = PsiDocumentManager.getInstance(project).getDocument(javaFile)
                    var lineNumber = document?.getLineNumber(callStatement!!.textOffset)
                    if (lineNumber != null)
                        lineNumber++
                    deprecatedMap[javaFile.name]?.add(
                        DeprecatedMethod(
                            expression.getOuterClass()?.qualifiedName ?: let { "匿名内部类" },
                            thisMethod?.name ?: thisField?.name,
                            callMethod.containingClass?.qualifiedName,
                            callMethod.name,
                            lineNumber.toString(),
                            if (thisMethod != null) "Method" else "Field",
                            javaFile.virtualFile.url
                        )
                    ) ?: let {
                        deprecatedMap[javaFile.name] = arrayListOf(
                            DeprecatedMethod(
                                expression.getOuterClass()?.qualifiedName ?: let { "匿名内部类" },
                                thisMethod?.name ?: thisField?.name,
                                callMethod.containingClass?.qualifiedName,
                                callMethod.name,
                                lineNumber.toString(),
                                if (thisMethod != null) "Method" else "Field",
                                javaFile.virtualFile.url
                            )
                        )
                    }
                }
                super.visitMethodCallExpression(expression)
            }

        }))
    }
    return deprecatedMap
}