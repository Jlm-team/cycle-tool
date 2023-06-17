package team.jlm.coderefactor.plugin.ui

import javax.swing.table.DefaultTableModel

class SampleTableModel(data: Array<Array<String?>>, column: Array<String>) : DefaultTableModel(data, column) {
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return if (dataVector[rowIndex][columnIndex] == null) "无" else dataVector[rowIndex][columnIndex] as Any
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return false
    }
}