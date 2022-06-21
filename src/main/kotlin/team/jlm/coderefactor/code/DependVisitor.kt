package team.jlm.coderefactor.code

import com.intellij.packageDependencies.DependenciesBuilder.DependencyProcessor
import com.intellij.packageDependencies.DependencyVisitorFactory.VisitorOptions
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiUtil

class DependVisitor internal constructor(
    private val myProcessor: DependencyProcessor,
    private val myOptions: VisitorOptions,
) : JavaRecursiveElementWalkingVisitor() {
    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        visitElement(expression)
    }

    //
    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        if (element is PsiWhiteSpace || element is PsiPackageStatement
            || element is PsiKeyword || element is PsiFile
            || element is PsiComment || element is PsiJavaToken
            || element is PsiReferenceParameterList
        ) {
            return
        }

        val refs = element.references
        for (ref in refs) {
            val resolved = ref.resolve()
            if (resolved != null && resolved is PsiClass && resolved !is PsiCompiledElement) {
                myProcessor.process(ref.element, resolved)
            }
        }
    }

    override fun visitLiteralExpression(expression: PsiLiteralExpression) {}
    override fun visitDocComment(comment: PsiDocComment) {}
    override fun visitImportStatement(statement: PsiImportStatement) {
        if (!myOptions.skipImports()) {
            visitElement(statement)
        }
    }

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        super.visitMethodCallExpression(expression)
        val psiMethod = expression.resolveMethod()
        if (psiMethod != null) {
            val returnType = psiMethod.returnType
            if (returnType != null) {
                val psiClass = PsiUtil.resolveClassInType(returnType)
                if (psiClass != null && psiClass !is PsiTypeParameter && psiClass !is PsiCompiledElement) {
                    myProcessor.process(expression, psiClass)
                }
            }
        }
    }
}