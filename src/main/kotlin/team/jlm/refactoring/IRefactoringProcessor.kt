package team.jlm.refactoring

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.impl.RefactoringTransaction
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor

interface IRefactoringProcessor {
    var myPrepareSuccessfulSwingThreadCallback: Runnable?
    fun createUsageViewDescriptor(): UsageViewDescriptor

    /**
     * Is called inside atomic action.
     */
    fun findUsages(): Array<out UsageInfo>
    fun getCommandName(): @NlsContexts.Command String
    fun setPreviewUsages(isPreviewUsages: Boolean)

    /**
     * Is called inside atomic action.
     */
    fun isPreviewUsages(usages: Array<out UsageInfo>): Boolean
    fun isPreviewUsages(): Boolean
    fun setPrepareSuccessfulSwingThreadCallback(prepareSuccessfulSwingThreadCallback: Runnable?)
    fun run()

    /**
     * Is called inside atomic action.
     *
     * @param refUsage usages to be filtered
     * @return true if preprocessed successfully
     */
    fun preprocessUsages(refUsage: Ref<Array<out UsageInfo>>): Boolean {
        // 此处不设为抽象是因为本插件内的接口实现需要默认实现，而idea自带的实现无需默认实现
        throw IllegalStateException(
            "fun preprocessUsages(refUsage: Ref<Array<out UsageInfo>>): Boolean " +
                    "must be override"
        )
    }

    fun preprocessUsages(): Boolean {
        // 此处不设为抽象是因为本插件内的接口实现需要默认实现，而idea自带的实现无需默认实现
        val refUsages = Ref<Array<out UsageInfo>>()
        refUsages.set(
            ReadAction.compute<Array<out UsageInfo>, RuntimeException> { findUsages() }
        )
        return preprocessUsages(refUsages)
    }

    fun execute(usages: Array<out UsageInfo>)
    fun refreshElements(elements: Array<out PsiElement>)
    /**
     * 对于不继承`com.intellij.refactoring.BaseRefactoringProcessor`的实现类，必须重写此属性
     */
    var refactoringTransaction: RefactoringTransaction
        get() = RefactoringProcessorInterceptor.myTransactionFiled.get(this) as RefactoringTransaction
        set(value) {
            RefactoringProcessorInterceptor.myTransactionFiled.set(this, value)
        }
}