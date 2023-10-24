package team.jlm.refactoring.makeStatic

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.refactoring.makeStatic.MakeMethodStaticProcessor
import com.intellij.refactoring.makeStatic.Settings
import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.NoOp
import team.jlm.refactoring.IRefactoringProcessor
import team.jlm.refactoring.RefactoringProcessorInterceptor
import team.jlm.refactoring.RefactoringProcessorInterceptor.Companion.createUsageViewDescriptorMethod0
import team.jlm.refactoring.RefactoringProcessorInterceptor.Companion.preprocessUsagesMethod0
import team.jlm.refactoring.RefactoringProcessorInterceptor.Companion.setRefactoringTransactionMethod
import team.jlm.refactoring.RefactoringProcessorInterceptor.Companion.setTransactionSetterMethod

fun createMakeMethodStaticProcess(
    project: Project,
    member: PsiMethod,
    settings: Settings = Settings(
        true,
        JavaCodeStyleManager.getInstance(project)
            .suggestVariableName(
                VariableKind.PARAMETER, null, null,
                JavaPsiFacade.getElementFactory(project).createType(member.containingClass!!)
            ).names[0],
        null
    ),
): IRefactoringProcessor {
    val enhancer = Enhancer()
    enhancer.setSuperclass(MakeMethodStaticProcessor::class.java)
    enhancer.setInterfaces(arrayOf(IRefactoringProcessor::class.java))
    enhancer.setCallbackFilter {
        if (it == preprocessUsagesMethod0 || it == createUsageViewDescriptorMethod0
            || it == setRefactoringTransactionMethod || it == setTransactionSetterMethod
        ) {
            return@setCallbackFilter 0
        } else
            return@setCallbackFilter 1
    }
    enhancer.setCallbacks(
        arrayOf(
            RefactoringProcessorInterceptor(project),
            NoOp.INSTANCE
        )
    )
    enhancer.classLoader = IRefactoringProcessor::class.java.classLoader
    enhancer.createClass()
    return enhancer.create(
        arrayOf(Project::class.java, PsiMethod::class.java, Settings::class.java),
        arrayOf(project, member, settings)
    ) as IRefactoringProcessor
}