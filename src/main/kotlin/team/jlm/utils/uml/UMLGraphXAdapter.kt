package team.jlm.utils.uml

import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.layout.mxCompactTreeLayout
import com.mxgraph.swing.mxGraphComponent
import org.jgrapht.ext.JGraphXAdapter
import org.jgrapht.graph.AbstractBaseGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultListenableGraph
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.JPanel

class UMLGraphXAdapter() : JPanel() {

    lateinit var jgxAdapter: JGraphXAdapter<String, DefaultEdge>
    lateinit var graph: AbstractBaseGraph<String, DefaultEdge>

    fun init() {
        val xgrapgh = DefaultListenableGraph(graph)
        jgxAdapter = JGraphXAdapter(xgrapgh)
        val screen = Toolkit.getDefaultToolkit().screenSize
        val sh: Int = (screen.height * 2) / 3
        val sw: Int = (screen.width * 2) / 3
        val DEFAULT_SIZE = Dimension(sw, sh)
        val component = mxGraphComponent(jgxAdapter)
        component.isAutoExtend =true
        component.graph.isCellsMovable =true
        component.graph.isCellsLocked = false
        component.graph.isAutoSizeCells =true
        component.isConnectable = false
        component.graph.isAllowDanglingEdges = false
        val layout = mxCircleLayout(jgxAdapter,300.0)
//        val radius = 300
//        layout.x0 = sw / 3.0 -radius
//        layout.y0 = sh / 3.0 - radius
//        layout.radius = radius.toDouble()
//        layout.isMoveCircle = true
        layout.execute(jgxAdapter.defaultParent)
        add(component,BorderLayout.CENTER)
        preferredSize = DEFAULT_SIZE
        size = DEFAULT_SIZE

    }
}