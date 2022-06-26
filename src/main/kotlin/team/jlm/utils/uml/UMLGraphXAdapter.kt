package team.jlm.utils.uml

import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.model.mxCell
import com.mxgraph.swing.handler.mxKeyboardHandler
import com.mxgraph.swing.handler.mxRubberband
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.swing.mxGraphOutline
import com.mxgraph.util.mxConstants
import org.jgrapht.ext.JGraphXAdapter
import org.jgrapht.graph.AbstractBaseGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultListenableGraph
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.JPanel


class UMLGraphXAdapter() : JPanel() {

    lateinit var jgxAdapter: JGraphXAdapter<String, DefaultEdge>
    lateinit var graph: AbstractBaseGraph<String, DefaultEdge>
    lateinit var graphComponent: mxGraphComponent
    lateinit var graphOutline: mxGraphOutline


    fun init() {
        val xgrapgh = DefaultListenableGraph(graph)
        jgxAdapter = JGraphXAdapter(xgrapgh)
        jgxAdapter.gridSize = 25
        graphComponent = mxGraphComponent(jgxAdapter)
        graphOutline = mxGraphOutline(graphComponent)



        reSizeCell(25.0,50.0)
        val edgeStyle = jgxAdapter.stylesheet.styles["defaultEdge"]
        val vertexStyle = jgxAdapter.stylesheet.styles["defaultVertex"]
        edgeStyle!!.put(mxConstants.STYLE_STROKECOLOR,"#2f65ca")
        edgeStyle.put(mxConstants.STYLE_FONTCOLOR,"#ffffff")
        vertexStyle!!.put(mxConstants.STYLE_FONTCOLOR,"#ffffff")
        vertexStyle.put(mxConstants.STYLE_FILLCOLOR,"#4c5052")
        vertexStyle.put(mxConstants.STYLE_STROKECOLOR,"#ababab")


        val screen = Toolkit.getDefaultToolkit().screenSize
        val sh: Int = (screen.height * 2) / 3
        val sw: Int = (screen.width * 2) / 3
        val DEFAULT_SIZE = Dimension(sw, sh)


        graphComponent.isGridVisible = true
        graphComponent.gridStyle = 2
        graphComponent.isAutoExtend = true
        graphComponent.graph.isCellsMovable = true
        graphComponent.graph.isCellsLocked = false
        graphComponent.graph.isAutoSizeCells = true
        graphComponent.isConnectable = false
        graphComponent.graph.isAllowDanglingEdges = false
        val layout = mxCircleLayout(jgxAdapter, 300.0)
//        val radius = 300
//        layout.x0 = sw / 3.0 -radius
//        layout.y0 = sh / 3.0 - radius
//        layout.radius = radius.toDouble()
//        layout.isMoveCircle = true
        layout.execute(jgxAdapter.defaultParent)
        add(graphComponent, BorderLayout.CENTER)
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
//    fun graphStye(isDarcula :Boolean){
//        val style =jgxAdapter.stylesheet
//        val edgeStyle = style.defaultEdgeStyle
//        val vertexStyle = style.defaultVertexStyle
//        if(isDarcula){
//
//        }
//        else{
//
//        }
//    }

}