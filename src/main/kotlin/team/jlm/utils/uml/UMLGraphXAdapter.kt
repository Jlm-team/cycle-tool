package team.jlm.utils.uml

import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.swing.mxGraphComponent
import org.jgrapht.ext.JGraphXAdapter
import org.jgrapht.graph.AbstractBaseGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultListenableGraph
import java.awt.Dimension
import javax.swing.JPanel

class UMLGraphXAdapter() : JPanel() {

    lateinit var jgxAdapter: JGraphXAdapter<String, DefaultEdge>
    var graph: AbstractBaseGraph<String, DefaultEdge>? = null
    val DEFAULT_SIZE = Dimension(600, 420)

    @Override
    fun init() {
        val xgrapgh = DefaultListenableGraph(graph)
        jgxAdapter = JGraphXAdapter(xgrapgh)
        preferredSize = DEFAULT_SIZE
        val component = mxGraphComponent(jgxAdapter)
        component.isConnectable = false
        component.graph.isAllowDanglingEdges = false
        add(component)

        val layout = mxCircleLayout(jgxAdapter)
        val radius = 100
        layout.x0 = DEFAULT_SIZE.width / 2.0 - radius
        layout.y0 = DEFAULT_SIZE.height / 2.0 - radius
        layout.radius = radius.toDouble()
        layout.isMoveCircle = true
        layout.execute(jgxAdapter.defaultParent)
    }
}