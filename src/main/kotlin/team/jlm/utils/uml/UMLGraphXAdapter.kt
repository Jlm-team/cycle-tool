package team.jlm.utils.uml

import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.model.mxCell
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.swing.mxGraphOutline
import com.mxgraph.util.mxConstants
import com.mxgraph.view.mxGraph
import org.jgrapht.ext.JGraphXAdapter
import org.jgrapht.graph.AbstractBaseGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultListenableGraph
import java.awt.Color
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.JPanel


class UMLGraphXAdapter() : JPanel() {

    lateinit var jgxAdapter: mxGraph
    lateinit var graph: AbstractBaseGraph<String, DefaultEdge>
    lateinit var graphComponent: mxGraphComponent
    lateinit var graphOutline: mxGraphOutline


    fun init(isUnderDarcula: Boolean) {
        val xgrapgh = DefaultListenableGraph(graph)
        jgxAdapter = JGraphXAdapter(xgrapgh)
        jgxAdapter.gridSize = 25
        graphComponent = mxGraphComponent(jgxAdapter)
        graphOutline = mxGraphOutline(graphComponent)

        val edgeStyle = jgxAdapter.stylesheet.styles["defaultEdge"]
        val vertexStyle = jgxAdapter.stylesheet.styles["defaultVertex"]
        if (isUnderDarcula) {
            graphComponent.gridColor = Color(171, 171, 171)
            edgeStyle!!.put(mxConstants.STYLE_STROKECOLOR, "#2f65ca")
            edgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#ffffff")
            edgeStyle.put(mxConstants.STYLE_MOVABLE, 0)
            vertexStyle!!.put(mxConstants.STYLE_FONTCOLOR, "#ffffff")
            vertexStyle.put(mxConstants.STYLE_FILLCOLOR, "#4c5052")
            vertexStyle.put(mxConstants.STYLE_STROKECOLOR, "#ababab")
            vertexStyle.put(mxConstants.STYLE_FONTSTYLE, 1)
        } else {
            graphComponent.gridColor = Color(230, 230, 230)
            vertexStyle!!.put(mxConstants.STYLE_FILLCOLOR, "#c9c9c9")
            vertexStyle.put(mxConstants.STYLE_FONTCOLOR, "#3c3f41")
            vertexStyle.put(mxConstants.STYLE_STROKECOLOR, "#acacac")
            vertexStyle.put(mxConstants.STYLE_FONTSTYLE, 1)
            edgeStyle!!.put(mxConstants.STYLE_MOVABLE, 0)
            edgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#3c3f41")
            edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#479345")
        }


        reSizeCell(25.0, 50.0)


        val screen = Toolkit.getDefaultToolkit().screenSize
        val sh: Int = (screen.height * 2) / 3
        val sw: Int = (screen.width * 2) / 3
        val DEFAULT_SIZE = Dimension(sw, sh)


        graphComponent.isGridVisible = true
        graphComponent.gridStyle = 2
        graphComponent.isAutoExtend = true
        graphComponent.graph.isAutoSizeCells = true
        graphComponent.graph.isAllowDanglingEdges = false
        val layout = mxCircleLayout(jgxAdapter, 300.0)
//        val radius = 300
//        layout.x0 = sw / 3.0 -radius
//        layout.y0 = sh / 3.0 - radius
//        layout.radius = radius.toDouble()
//        layout.isMoveCircle = true
        layout.execute(jgxAdapter.defaultParent)
        add(graphComponent)
        preferredSize = DEFAULT_SIZE
        size = DEFAULT_SIZE

    }

    fun reSizeCell(h: Double, w: Double) {
        val cell = jgxAdapter.getModel().getRoot() as mxCell
        val count = cell.childCount
        for (i in 0 until count) {
            val cells = cell.getChildAt(i) as mxCell
            val counts = cells.childCount
            for (j in 0 until counts) {
                val cellss = cells.getChildAt(j) as mxCell
                if (cellss.value is DefaultEdge) {
                    cellss.value = ""
                } else {
                    cellss.geometry.width = w
                    cellss.geometry.height = h
                }
            }
        }
    }


}