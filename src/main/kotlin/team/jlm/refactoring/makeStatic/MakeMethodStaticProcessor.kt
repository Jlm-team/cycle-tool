package team.jlm.refactoring.makeStatic

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.refactoring.util.LambdaRefactoringUtil
import com.intellij.refactoring.util.javadoc.MethodJavaDocHelper
import com.intellij.usageView.UsageInfo

class MakeMethodStaticProcessor(project: Project, member: PsiMethod) :
    MakeMethodOrClassStaticProcessor<PsiMethod>(project, member) {

    private val suggestionParameterName: String

    init {
        val type = JavaPsiFacade.getElementFactory(project).createType(member.containingClass!!)
        val names = JavaCodeStyleManager.getInstance(project)
            .suggestVariableName(VariableKind.PARAMETER, null, null, type).names
        suggestionParameterName = names[0]
    }

    override fun findExternalUsages(result: ArrayList<UsageInfo>) {
        findExternalReferences(member, result)
    }

    override fun changeSelf(factory: PsiElementFactory, usages: Array<out UsageInfo>) {
        val javaDocHelper = MethodJavaDocHelper(member)
        val paramList: PsiParameterList = member.parameterList
        val containingClass: PsiClass = member.containingClass!!
        val parameterType: PsiType = factory.createType(containingClass, PsiSubstitutor.EMPTY)
        val classParameterName: String = suggestionParameterName
        val parameter = factory.createParameter(classParameterName, parameterType)
        if (makeClassParameterFinal(usages)) {
            PsiUtil.setModifierProperty(parameter, PsiModifier.FINAL, true)
        }
        paramList.addAfter(parameter, null)
        javaDocHelper.addParameterAfter(classParameterName, null)
//        }

        /*if (settings.isMakeFieldParameters) {
            val parameters: List<FieldParameter> = settings.parameterOrderList
            for (fieldParameter in parameters) {
                val fieldParameterType = fieldParameter.field.type
                val parameter = factory!!.createParameter(fieldParameter.name, fieldParameterType)
                if (makeFieldParameterFinal(fieldParameter.field, usages)) {
                    PsiUtil.setModifierProperty(parameter, PsiModifier.FINAL, true)
                }
                addParameterAfter = paramList.addAfter(parameter, addParameterAfter)
                anchor = javaDocHelper.addParameterAfter(fieldParameter.name, anchor)
            }
        }*/
        makeStatic(member)
    }

    private fun makeStatic(member: PsiMethod) {
        val overrideAnnotation = AnnotationUtil.findAnnotation(member, CommonClassNames.JAVA_LANG_OVERRIDE)
        overrideAnnotation?.delete()
        setupTypeParameterList(member)
        // Add static modifier
        val modifierList = member.modifierList
        modifierList.setModifierProperty(PsiModifier.STATIC, true)
        modifierList.setModifierProperty(PsiModifier.FINAL, false)
        modifierList.setModifierProperty(PsiModifier.DEFAULT, false)
        val receiverParameter = PsiTreeUtil.getChildOfType(
            member.parameterList,
            PsiReceiverParameter::class.java
        )
        receiverParameter?.delete()
    }

    override fun changeSelfUsage(usageInfo: SelfUsageInfo) {
        val element = usageInfo.element
        var parent = element!!.parent
        if (element is PsiMethodReferenceExpression) {
            parent = if (needLambdaConversion(element)) {
                val methodCallExpression = getMethodCallExpression(element) ?: return
                methodCallExpression
            } else {
                val factory = JavaPsiFacade.getElementFactory(parent.project)
                val memberClass: PsiClass = member.containingClass!!
                val qualifier = element.qualifier
                qualifier!!.replace(factory.createReferenceExpression(memberClass))
                return
            }
            return
        }
        val methodCall = parent as PsiMethodCallExpression
        val qualifier = methodCall.methodExpression.qualifierExpression
        qualifier?.delete()

        val factory = JavaPsiFacade.getElementFactory(methodCall.project)
        val args = methodCall.argumentList
        val arg: PsiElement = factory.createExpressionFromText(suggestionParameterName, null)
        args.addAfter(arg, null)
    }

    override fun changeInternalUsage(usage: InternalUsageInfo, factory: PsiElementFactory) {
        when (val element = usage.element) {
            is PsiReferenceExpression -> {
                var newRef: PsiReferenceExpression?
                newRef = factory.createExpressionFromText(
                    suggestionParameterName + "." + element.getText(), null
                ) as PsiReferenceExpression
                val codeStyleManager = CodeStyleManager.getInstance(
                    myProject!!
                )
                newRef = codeStyleManager.reformat(newRef) as PsiReferenceExpression
                element.replace(newRef)
            }

            is PsiThisExpression -> {
                element.replace(factory.createExpressionFromText(suggestionParameterName, null))
            }

            is PsiSuperExpression -> {
                element.replace(factory.createExpressionFromText(suggestionParameterName, null))
            }

            is PsiNewExpression -> {
                val newText: String = suggestionParameterName + "." + element.text
                val expr = factory.createExpressionFromText(newText, null)
                element.replace(expr)
            }
        }
    }

    override fun changeExternalUsage(usage: UsageInfo, factory: PsiElementFactory) {
        val element = usage!!.element as? PsiReferenceExpression ?: return
        var methodRef = element
        if (methodRef is PsiMethodReferenceExpression && needLambdaConversion(methodRef)) {
            val expression = getMethodCallExpression(methodRef) ?: return
            methodRef = expression.methodExpression
        }
        val parent = methodRef.parent
        var instanceRef: PsiExpression?
        instanceRef = methodRef.qualifierExpression
        val newQualifier: PsiElement?
        val memberClass: PsiClass = member.containingClass!!
        if (instanceRef == null || instanceRef is PsiSuperExpression) {
            val contextClass = PsiTreeUtil.getParentOfType(
                element,
                PsiClass::class.java
            )
            instanceRef = if (!InheritanceUtil.isInheritorOrSelf(contextClass, memberClass, true)) {
                factory.createExpressionFromText(memberClass.qualifiedName + ".this", null)
            } else {
                factory.createExpressionFromText("this", null)
            }
            newQualifier = null
        } else {
            newQualifier = factory.createReferenceExpression(memberClass)
        }
        val argList: PsiExpressionList?
        val exprs: Array<out PsiExpression>?
        if (parent is PsiMethodCallExpression) {
            argList = parent.argumentList
            exprs = argList.expressions
            if (exprs.isNotEmpty()) {
                argList.addBefore(instanceRef, exprs[0])
            } else {
                argList.add(instanceRef)
            }
        }
        if (newQualifier != null) {
            methodRef.qualifierExpression!!.replace(newQualifier)
        }
    }

    private fun getMethodCallExpression(methodRef: PsiMethodReferenceExpression): PsiMethodCallExpression? {
        val lambdaExpression = LambdaRefactoringUtil.convertMethodReferenceToLambda(methodRef, true, true)
        val returnExpressions = LambdaUtil.getReturnExpressions(lambdaExpression)
        if (returnExpressions.size != 1) return null
        val expression = returnExpressions[0]
        return if (expression !is PsiMethodCallExpression) null else expression
    }

    private fun needLambdaConversion(methodRef: PsiMethodReferenceExpression): Boolean {
        return if (PsiMethodReferenceUtil.isResolvedBySecondSearch(methodRef)) {
            member.parameters.isNotEmpty()
        } else true
    }

}