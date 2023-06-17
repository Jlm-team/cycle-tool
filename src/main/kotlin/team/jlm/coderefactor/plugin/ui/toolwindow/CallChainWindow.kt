package team.jlm.coderefactor.plugin.ui.toolwindow

import team.jlm.coderefactor.plugin.ui.DefaultScrollPaneTable
import team.jlm.refactoring.move.callchain.CallChain
import javax.swing.JPanel

object CallChainWindow {
    fun getWindow(content: HashSet<CallChain>): JPanel {
        val data = content.map {
            arrayOf(
                it.callerContainingClass,
                it.callerName,
                it.calleeContainingClass,
                it.calleeName,
                it.callTargetContainingClass,
                it.callTarget
            )
        }.toTypedArray()
        val column = arrayOf("调用者所在类", "调用者", "被调用者所在类", "被调用者", "调用目标类", "调用目标")
        return DefaultScrollPaneTable.getScrollTable(data, column)
    }
}