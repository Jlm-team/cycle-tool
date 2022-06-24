package team.jlm.utils.uml

import com.xyzboom.algorithm.graph.Graph
import org.jgrapht.graph.*


enum class myGraphType {
    SimpleGraph,
    DirectedMultigraph,
    Multigraph,
}


fun buildGraph(graph: Graph<String>, type: myGraphType): AbstractBaseGraph<String, DefaultEdge> {
    when (type) {
        myGraphType.SimpleGraph -> {
            return buildSimpleGraph(graph)
        }
        myGraphType.DirectedMultigraph -> {
            return buildDirectedMultigraph(graph)
        }
        myGraphType.Multigraph -> {
            return buildMultigraph(graph)
        }
    }
}

/**
 * @description 多重边 有向 无权 无自环
 */

private fun buildDirectedMultigraph(graph: Graph<String>): DirectedMultigraph<String, DefaultEdge> {
    val graphT = DirectedMultigraph<String, DefaultEdge>(DefaultEdge::class.java)
    for (i in graph.adjList.keys) {
        graphT.addVertex(i.data)
    }
    for (i in graph.adjList) {
        val inEdge = i.value.edgeIn
//        val outEdge = i.value.edgeOut
        for (edge in inEdge) {
            graphT.addEdge(edge.nodeFrom.data, edge.nodeTo.data)
        }
    }
    return graphT
}

/**
 * @description 单边 无向 无权 无自环
 */
private fun buildSimpleGraph(graph: Graph<String>): SimpleGraph<String, DefaultEdge> {
    val graphT = SimpleGraph<String, DefaultEdge>(DefaultEdge::class.java)
    for (i in graph.adjList.keys) {
        graphT.addVertex(i.data)
    }
    for (i in graph.adjList) {
        val inEdge = i.value.edgeIn
//        val outEdge = i.value.edgeOut
        for (edge in inEdge) {
            graphT.addEdge(edge.nodeFrom.data, edge.nodeTo.data)
        }
    }
    return graphT
}

/**
 * @description 多重边 无向 无权 无自环
 */
private fun buildMultigraph(graph: Graph<String>): Multigraph<String, DefaultEdge> {
    val graphT = Multigraph<String, DefaultEdge>(DefaultEdge::class.java)
    for (i in graph.adjList.keys) {
        graphT.addVertex(i.data)
    }
    for (i in graph.adjList) {
        val inEdge = i.value.edgeIn
//        val outEdge = i.value.edgeOut
        for (edge in inEdge) {
            graphT.addEdge(edge.nodeFrom.data, edge.nodeTo.data)
        }
    }
    return graphT
}

