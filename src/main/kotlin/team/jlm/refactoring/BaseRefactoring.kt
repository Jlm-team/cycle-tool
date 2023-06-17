package team.jlm.refactoring

import com.intellij.openapi.util.Ref
import com.intellij.refactoring.Refactoring
import com.intellij.usageView.UsageInfo

abstract class BaseRefactoring<T>(protected val myProcessor: T) : Refactoring
        where T : BaseRefactoringProcessor {

    override fun setPreviewUsages(value: Boolean) {
        myProcessor.setPreviewUsages(value)
    }

    override fun isPreviewUsages(): Boolean {
        return myProcessor.isPreviewUsages()
    }

    override fun setInteractive(prepareSuccessfulCallback: Runnable?) {
        myProcessor.setPrepareSuccessfulSwingThreadCallback(prepareSuccessfulCallback)
    }

    override fun isInteractive(): Boolean {
        return myProcessor.myPrepareSuccessfulSwingThreadCallback != null
    }

    override fun findUsages(): Array<out UsageInfo> {
        return myProcessor.findUsages()
    }

    override fun preprocessUsages(usages: Ref<Array<out UsageInfo>>): Boolean {
        return myProcessor.preprocessUsages(usages)
    }

    override fun shouldPreviewUsages(usages: Array<UsageInfo>): Boolean {
        return myProcessor.isPreviewUsages(usages)
    }

    override fun doRefactoring(usages: Array<UsageInfo>) {
        myProcessor.execute(usages)
    }

    override fun run() {
        myProcessor.run()
    }

}