package team.jlm.utils

import com.intellij.refactoring.BaseRefactoringProcessor
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import team.jlm.refactoring.interceptor.RefactoringProcessorInterceptor
import java.lang.instrument.Instrumentation

val instrument: Instrumentation by lazy {
    ByteBuddyAgent.install()
}

val redefinedBaseRefactoringProcessorClass: Class<out BaseRefactoringProcessor> by lazy {
    ByteBuddy()
        .redefine(BaseRefactoringProcessor::class.java)
        .method(ElementMatchers.named<MethodDescription?>("doRefactoring")
            .and(ElementMatchers.isDeclaredBy(BaseRefactoringProcessor::class.java)))
        .intercept(MethodDelegation.to(RefactoringProcessorInterceptor::class.java))
        .make()
        .load(RefactoringProcessorInterceptor::class.java.classLoader, ClassLoadingStrategy.Default.CHILD_FIRST)
        .loaded
}