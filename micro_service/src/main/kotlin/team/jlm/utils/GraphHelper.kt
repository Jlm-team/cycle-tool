package team.jlm.utils

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import team.jlm.entity.Edge
import team.jlm.entity.GraphBean
import team.jlm.entity.Node
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer



fun deSerialization(str: String): Graph<String> {
    val json =  Json { allowStructuredMapKeys = true }
    return  json.decodeFromString(str)
}


private fun graphToBean(id: String, graph: Graph<String>): GraphBean {
    val nodes = ArrayList<Node>()
    val edges = ArrayList<Edge>()
    for (i in graph.adjList.keys) {
        nodes.add(Node(i.data, i.data, 100, 50,"rect"))
    }
    for (i in graph.adjList) {
        val inEdge = i.value.edgeIn
        for (e in inEdge) {
            edges.add(Edge(e.nodeFrom.data, e.nodeTo.data,"edge"))
        }
    }
    return GraphBean(id, nodes, edges)
}

fun testGraph(): GraphBean {
    val graph= Graph<String>()
    val nodes= ArrayList<GNode<String>>()
    for (i in 0..5) {
        nodes.add(graph.addNode(('A'.code + i.toChar().code).toChar().toString()))
    }
    graph.addEdge(nodes[0], nodes[1]) //A -> B

    graph.addEdge(nodes[0], nodes[1]) //A -> B

    graph.addEdge(nodes[0], nodes[2]) //A -> C

    graph.addEdge(nodes[1], nodes[3]) //B -> D

    graph.addEdge(nodes[1], nodes[4]) //B -> E

    graph.addEdge(nodes[2], nodes[3]) //C -> D

    graph.addEdge(nodes[3], nodes[4]) //D -> E

    graph.addEdge(nodes[3], nodes[5]) //D -> F
    return graphToBean("1",graph)

}
fun packJson(path: String): ArrayList<GraphBean> {
    val folder = File(path)
    val graphs = ArrayList<GraphBean>()
    if (!folder.exists() || !folder.isDirectory)
        throw IOException("No Such Directory!")
    else {
        val files = folder.listFiles() ?: throw IOException("Failed To Traverse The File!")
        if (files.isNotEmpty()) {
            for (i in files) {
                try {
                    val inputStream = FileInputStream(i)
                    val channel = inputStream.channel

                    val buffer = ByteBuffer.allocate(1024)
                    var length = -1
                    val sb = StringBuilder()
                    while (channel.read(buffer).also { length = it } !== -1) {
                        buffer.clear()
                        sb.append(String(buffer.array(), 0, length))
                    }
                    channel.close()
                    inputStream.close()
                    val graph = deSerialization(sb.toString())
                    graphs.add(graphToBean(i.name, graph))
                } catch (e: Exception) {
                    println(e.message)
                    throw e
                }
            }
        } else {
            throw IOException("Empty Directory!")
        }
    }
    return graphs
}