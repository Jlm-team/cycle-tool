package team.jlm.utils

import com.google.gson.Gson
import com.xyzboom.algorithm.graph.Graph
import team.jlm.entity.Edge
import team.jlm.entity.GraphBean
import team.jlm.entity.Node
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer

private fun deSerialization(obj: String): Graph<String> {
    return Gson().fromJson(obj, Graph<String>().javaClass)
}

private fun graphToBean(id: String, graph: Graph<String>): GraphBean {
    val nodes = ArrayList<Node>()
    val edges = ArrayList<Edge>()
    for ((index, i) in graph.adjList.keys.withIndex()) {
        nodes.add(Node(index, i.data, 100, 50))
    }
    for (i in graph.adjList) {
        val inEdge = i.value.edgeIn
        for (e in inEdge) {
            edges.add(Edge(e.nodeFrom.data, e.nodeTo.data))
        }
    }
    return GraphBean(id, nodes, edges)
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
                } catch (e: IOException) {
                    throw e
                }
            }
        } else {
            throw IOException("Empty Directory!")
        }
    }
    return graphs
}