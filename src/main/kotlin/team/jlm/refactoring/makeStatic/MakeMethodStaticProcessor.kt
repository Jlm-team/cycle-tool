package team.jlm.refactoring.makeStatic

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.refactoring.makeStatic.MakeMethodStaticProcessor
import com.intellij.refactoring.makeStatic.Settings
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import team.jlm.refactoring.IRefactoringProcessor
import team.jlm.refactoring.interceptor.ExecuteInterceptor
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.getMethodFilter
import team.jlm.refactoring.multi.ICallFromMulti
import team.jlm.utils.instrument
import team.jlm.utils.redefinedBaseRefactoringProcessorClass

private val makeMethodStaticProcessorClass by lazy {
    instrument
    redefinedBaseRefactoringProcessorClass
    ByteBuddy().subclass(MakeMethodStaticProcessor::class.java)
        .defineField("callFromMulti", Boolean::class.java, Visibility.PRIVATE)
        .implement(ICallFromMulti::class.java)
        .intercept(FieldAccessor.ofBeanProperty())
        .implement(IRefactoringProcessor::class.java)
        .method(getMethodFilter())
        .intercept(MethodDelegation.to(RefactoringProcessorInterceptor::class.java))
        .method(ElementMatchers.named("execute"))
        .intercept(MethodDelegation.to(ExecuteInterceptor::class.java))
        .make()
        .load(ICallFromMulti::class.java.classLoader, ClassLoadingStrategy.Default.CHILD_FIRST)
        .loaded
}

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
    return makeMethodStaticProcessorClass.getConstructor(
        Project::class.java,
        PsiMethod::class.java,
        Settings::class.java
    ).newInstance(project, member, settings) as IRefactoringProcessor
}