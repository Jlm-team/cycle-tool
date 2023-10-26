package team.jlm.refactoring.callchain

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.refactoring.inline.InlineMethodProcessor
import com.intellij.usageView.UsageInfo
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Argument
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.This
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.matcher.ElementMatchers.`is`
import team.jlm.refactoring.IRefactoringProcessor
import team.jlm.refactoring.interceptor.ExecuteInterceptor
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor.Companion.getMethodFilter
import team.jlm.refactoring.multi.ICallFromMulti
import team.jlm.utils.instrument
import team.jlm.utils.redefinedBaseRefactoringProcessorClass
import java.lang.reflect.Method

private val inlineMethodProcessorClass by lazy {
    instrument
    redefinedBaseRefactoringProcessorClass
    ByteBuddy().subclass(InlineMethodProcessor::class.java)
        .method(`is`(inlineMethodProcessorPerformRefactoringMethod))
        .intercept(MethodDelegation.to(InlineMethodProcessorInterceptor::class.java))
        .method(ElementMatchers.named("execute"))
        .intercept(MethodDelegation.to(ExecuteInterceptor::class.java))
        .defineField("callFromMulti", Boolean::class.java, Visibility.PRIVATE)
        .implement(ICallFromMulti::class.java)
        .intercept(FieldAccessor.ofBeanProperty())
        .implement(IRefactoringProcessor::class.java)
        .method(getMethodFilter())
        .intercept(MethodDelegation.to(RefactoringProcessorInterceptor::class.java))
        .make()
        .load(ICallFromMulti::class.java.classLoader, ClassLoadingStrategy.Default.CHILD_FIRST)
        .loaded
}

private val inlineMethodProcessorDoRefactoringMethod: Method by lazy {
    InlineMethodProcessor::class.java
        .getDeclaredMethod("doRefactoring", emptyArray<UsageInfo>()::class.java)
        .apply { isAccessible = true }
}

private val inlineMethodProcessorPerformRefactoringMethod: Method by lazy {
    InlineMethodProcessor::class.java
        .getDeclaredMethod("performRefactoring", emptyArray<UsageInfo>()::class.java)
        .apply { isAccessible = true }
}

open class InlineMethodProcessorInterceptor {
    companion object {
        private fun performRefactoring(obj: InlineMethodProcessor, usages: Array<out UsageInfo>) {
            obj.apply {
                inlineMethodProcessorDoRefactoringMethod.invoke(this, usages)
            }
        }

        @JvmStatic
        @RuntimeType
        fun performRefactoringInterceptor(
            @This obj: Any,
            @Argument(0) usages: Array<out UsageInfo>,
        ) {
            if (obj is ICallFromMulti && obj.callFromMulti) {
                performRefactoring(obj as InlineMethodProcessor, usages)
            } else {
                inlineMethodProcessorPerformRefactoringMethod.invoke(obj, usages)
            }
        }
    }
}

fun createInlineMethodProcessor(
    project: Project,
    method: PsiMethod,
    reference: PsiReference?,
    editor: Editor? = null,
    isInlineThisOnly: Boolean = true,
): IRefactoringProcessor {
    return inlineMethodProcessorClass.getConstructor(
        Project::class.java,
        PsiMethod::class.java,
        PsiReference::class.java,
        Editor::class.java,
        Boolean::class.java,
//        Runnable::class.java
    ).newInstance(project, method, reference, editor, isInlineThisOnly) as IRefactoringProcessor
}

