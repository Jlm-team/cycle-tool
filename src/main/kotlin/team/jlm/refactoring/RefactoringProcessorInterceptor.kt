package team.jlm.refactoring

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.listeners.impl.RefactoringTransaction
import com.intellij.usageView.UsageInfo
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import java.lang.reflect.Field
import java.lang.reflect.Method

class RefactoringProcessorInterceptor(
    private val project: Project,
) : MethodInterceptor {
    companion object {
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

        val setRefactoringTransactionMethod: Method = IRefactoringProcessor::class.java
            .getDeclaredMethod("setRefactoringTransaction", RefactoringTransaction::class.java)

        private val myTransactionFiled: Field = BaseRefactoringProcessor::class.java
            .getDeclaredField("myTransaction").apply {
                isAccessible = true
            }

        val setTransactionSetterMethod: Method = IRefactoringProcessor::class.java
            .getDeclaredMethod("setTransactionSetter", kotlin.jvm.functions.Function1::class.java)
    }

    override fun intercept(obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy): Any? {
        obj as IRefactoringProcessor
        method.isAccessible = true
        val refUsages = Ref<Array<out UsageInfo>>()
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                refUsages.set(
                    ReadAction.compute<Array<out UsageInfo>, RuntimeException> {
                        obj.findUsages()
                    }
                )
            }, RefactoringBundle.message("progress.text"),
            true, project
        )

        when (method) {
            preprocessUsagesMethod0 -> {
                return preprocessUsagesMethod1.invoke(obj, refUsages)
            }

            createUsageViewDescriptorMethod0 -> {
                return createUsageViewDescriptorMethod1.invoke(obj, emptyArray<UsageInfo>())
            }

            setRefactoringTransactionMethod -> {
                myTransactionFiled.set(obj, args[0])
                return null
            }
            // 本来是用于拦截myTransactionFiled的，但是上方已经实现，因此此处不做实现即可
            setTransactionSetterMethod -> return null
        }
        return null
    }
}