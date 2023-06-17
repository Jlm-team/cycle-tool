package team.jlm.coderefactor.plugin.ui.toolwindow

import com.intellij.ui.components.JBScrollPane
import team.jlm.coderefactor.plugin.ui.CollapsiblePanel
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JPanel


object UnUsedImportWindow {
    fun getWindow(content: ConcurrentHashMap<String, Int>): JPanel {
        val layoutConstraints = GridBagConstraints()
        layoutConstraints.fill = GridBagConstraints.HORIZONTAL
        layoutConstraints.weightx = 1.0
        layoutConstraints.gridx = 0
        layoutConstraints.gridy = 0
        val panel = JPanel(GridBagLayout())

        val map = HashMap<String,ArrayList<Array<String?>>>(content.size)

        content.forEach { (k,v) ->
            val (key,nextKey) = k.split("/")
            map[key]?.add(arrayOf(nextKey,v.toString())) ?:map.put(key, arrayListOf(arrayOf(nextKey,v.toString())))
        }

        val column = arrayOf("文件名","移除未使用Import数量")

        map.forEach { (k,v)->
            val contentPanel = CollapsiblePanel(k,v.toTypedArray(),column,false,0,null)
            panel.add(contentPanel, layoutConstraints)
            layoutConstraints.gridy++
        }
        val scrollPane = JPanel(BorderLayout())
        scrollPane.add(JBScrollPane(panel), BorderLayout.NORTH)
        return scrollPane
    }

}