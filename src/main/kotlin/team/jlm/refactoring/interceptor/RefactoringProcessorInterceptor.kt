package team.jlm.refactoring.interceptor

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.implementation.bind.annotation.*
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers
import net.sf.cglib.proxy.CallbackFilter
import team.jlm.refactoring.IRefactoringProcessor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.Callable


class RefactoringProcessorInterceptor(
    private val project: Project,
) {
    companion object {

        fun <T : MethodDescription> getMethodFilter(): ElementMatcher<T> {
            return ElementMatchers.`is`<T>(preprocessUsagesMethod0)
                .or(ElementMatchers.`is`(createUsageViewDescriptorMethod0))
                .or(ElementMatchers.`is`(performRefactoringMethod))
                .or(
                    ElementMatchers.named<MethodDescription>("doRefactoring")
                        .and(ElementMatchers.isDeclaredBy(BaseRefactoringProcessor::class.java))
                )

        }

        val callbackFilter = CallbackFilter { it ->
            if (it == preprocessUsagesMethod0 || it == createUsageViewDescriptorMethod0
                || it == performRefactoringMethod
                || it == doRefactoringMethod
            ) {
                0
            } else
                1
        }

        val performRefactoringMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("performRefactoring", emptyArray<UsageInfo>().javaClass).apply {
                isAccessible = true
            }
        val createUsageViewDescriptorMethod0: Method = IRefactoringProcessor::class.java
            .getDeclaredMethod("createUsageViewDescriptor").apply {
                isAccessible = true
            }
        private val createUsageViewDescriptorMethod1: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("createUsageViewDescriptor", emptyArray<UsageInfo>().javaClass).apply {
                isAccessible = true
            }
        val preprocessUsagesMethod0: Method = IRefactoringProcessor::class.java
            .getDeclaredMethod("preprocessUsages").apply {
                isAccessible = true
            }
        private val preprocessUsagesMethod1 = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("preprocessUsages", Ref::class.java).apply {
                isAccessible = true
            }

        val myTransactionField: Field = BaseRefactoringProcessor::class.java
            .getDeclaredField("myTransaction").apply {
                isAccessible = true
            }

        val doRefactoringMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("doRefactoring", java.util.Collection::class.java).apply {
                isAccessible = true
            }

        val isToBeChangedMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("isToBeChanged", UsageInfo::class.java).apply {
                isAccessible = true
            }

        val getCommandNameMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("getCommandName").apply { isAccessible = true }

        val myProjectField: Field = BaseRefactoringProcessor::class.java
            .getDeclaredField("myProject").apply { isAccessible = true }

        val canPerformRefactoringInBranchMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("canPerformRefactoringInBranch").apply {
                isAccessible = true
            }

        val callPerformRefactoringMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod(
                "callPerformRefactoring", emptyArray<UsageInfo>()::class.java,
                Runnable::class.java
            ).apply { isAccessible = true }

        val performInBranchMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("performInBranch", emptyArray<UsageInfo>()::class.java)
            .apply { isAccessible = true }

        val isPreviewUsagesMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("isPreviewUsages", emptyArray<UsageInfo>()::class.java)
            .apply { isAccessible = true }

        val performPsiSpoilingRefactoringMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("performPsiSpoilingRefactoring")
            .apply { isAccessible = true }

        val findUsagesMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("findUsages")
            .apply { isAccessible = true }

        val refreshElementsMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("refreshElements", emptyArray<PsiElement>()::class.java)
            .apply { isAccessible = true }

        val isGlobalUndoActionMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("isGlobalUndoAction")
            .apply { isAccessible = true }

        val getUndoConfirmationPolicyMethod: Method = BaseRefactoringProcessor::class.java
            .getDeclaredMethod("getUndoConfirmationPolicy")
            .apply { isAccessible = true }

        @JvmStatic
        fun interceptPreprocessUsagesMethod0(@SuperCall superCall: Any): Boolean {
            if (superCall is Callable<*>)
                return superCall.call() as Boolean
            else throw Exception()
        }

        @JvmStatic
        fun interceptPreprocessUsagesMethod0(): Boolean {
            /*if (superCall is Callable<*>)
                return superCall.call() as Boolean*/
            throw Exception()
        }

        @JvmStatic
        fun interceptCreateUsageViewDescriptorMethod0(@This obj: BaseRefactoringProcessor): UsageViewDescriptor {
            val refUsages = Ref<Array<out UsageInfo>>()
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    refUsages.set(
                        ReadAction.compute<Array<out UsageInfo>, RuntimeException> {
                            findUsagesMethod.invoke(obj) as Array<UsageInfo>
                        }
                    )
                }, RefactoringBundle.message("progress.text"),
                true, myProjectField.get(obj) as Project
            )
            return createUsageViewDescriptorMethod1.invoke(obj, refUsages.get()) as UsageViewDescriptor
        }

        @JvmStatic
        @RuntimeType
        fun intercept(
            @This obj: Any,
            @Origin method: Method,
            @AllArguments @RuntimeType args: Array<out Any>,
            @SuperCall superCall: Callable<*>,
        ): Any? {
            obj as IRefactoringProcessor
            obj as BaseRefactoringProcessor
            method.isAccessible = true
            /*if (method == doRefactoringMethod) {
                return obj.doRefactor(args[0] as MutableCollection<UsageInfo>)
            }
            if (method == performRefactoringMethod) {
                this.myTransactionField.set(obj, obj.refactoringTransaction)
                return superCall.call()
            }*/
            val refUsages = Ref<Array<out UsageInfo>>()
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    refUsages.set(
                        ReadAction.compute<Array<out UsageInfo>, RuntimeException> {
                            findUsagesMethod.invoke(obj) as Array<UsageInfo>
                        }
                    )
                }, RefactoringBundle.message("progress.text"),
                true, myProjectField.get(obj) as Project
            )

            when (method) {
                preprocessUsagesMethod0 -> {
                    return preprocessUsagesMethod1.invoke(obj, refUsages)
                }

                createUsageViewDescriptorMethod0 -> {
                    return createUsageViewDescriptorMethod1.invoke(obj, emptyArray<UsageInfo>())
                }
            }
            return null
        }

        @JvmStatic
        @RuntimeType
        @BindingPriority(1)
        fun intercept1(
            @This obj: Any,
            @Origin method: Method,
            @AllArguments @RuntimeType args: Array<out Any>,
            @SuperCall superCall: Runnable,
        ): Any? {
            obj as IRefactoringProcessor
            obj as BaseRefactoringProcessor
            method.isAccessible = true
            /*if (method == doRefactoringMethod) {
                return obj.doRefactor(args[0] as MutableCollection<UsageInfo>)
            }
            if (method == performRefactoringMethod) {
                this.myTransactionField.set(obj, obj.refactoringTransaction)
                return superCall.run()
            }*/
            val refUsages = Ref<Array<out UsageInfo>>()
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    refUsages.set(
                        ReadAction.compute<Array<out UsageInfo>, RuntimeException> {
                            obj.findUsages()
                        }
                    )
                }, RefactoringBundle.message("progress.text"),
                true, myProjectField.get(obj) as Project
            )

            when (method) {
                preprocessUsagesMethod0 -> {
                    return preprocessUsagesMethod1.invoke(obj, refUsages)
                }

                createUsageViewDescriptorMethod0 -> {
                    return createUsageViewDescriptorMethod1.invoke(obj, emptyArray<UsageInfo>())
                }

            }
            return null
        }
    }

    /*private fun doRefactorInterceptor(obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy): Any? {
        return (obj as BaseRefactoringProcessor).doRefactor(args[0] as MutableCollection<UsageInfo>)
    }*/

}