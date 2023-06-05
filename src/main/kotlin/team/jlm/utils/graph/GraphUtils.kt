@file:OptIn(ExperimentalSerializationApi::class)

package team.jlm.utils.graph

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import team.jlm.utils.file.pluginCacheFolderName
import java.io.File
import java.util.*
import kotlin.math.min

class Tarjan<T>(private var graph: Graph<T>) {
    private var timer: Int = 0
    val result = ArrayList<ArrayList<GNode<T>>>()
    private val dfn = Array(graph.adjList.size) { 0 }
    private val low = Array(graph.adjList.size) { 0 }
    private val s = Stack<Int>()
    private val visited = Array(graph.adjList.size) { false }
    private val nodesArray: ArrayList<GNode<T>> = ArrayList(graph.adjList.keys)

    init {
        for (i in 0 until graph.adjList.size) {
            if (dfn[i] == 0) {
                doTarjan(i)
            }
        }
    }

    private fun doTarjan(u: Int): ArrayList<ArrayList<GNode<T>>> {
        timer++
        dfn[u] = timer
        low[u] = timer
        s.push(u)
        visited[u] = true
        for (edge in graph.adjList[nodesArray[u]]!!.edgeOut) {
            val nextIndex = nodesArray.indexOf(edge.nodeTo)
            if (dfn[nextIndex] == 0) {//目标节点尚未遍历
                doTarjan(nextIndex)
                low[u] = min(low[u], low[nextIndex])
            } else if (visited[nextIndex]) {
                low[u] = min(low[u], dfn[nextIndex])
            }
        }
        if (low[u] == dfn[u]) {
            var v: Int
            val newSCC = ArrayList<GNode<T>>()
            do {
                v = s.pop()
                newSCC.add(nodesArray[v])
                visited[v] = false
            } while (u != v)
            result.add(newSCC)
        }
        return result
    }
}

private val json1 = Json { allowStructuredMapKeys = true }

fun Graph<String>.saveAsDependencyGraph(pathSuffix: String, projectBasePath: String) {
    /*for ((node, edgePair) in adjList) {
        pluginBaseFoldrExist(projectBasePath)
        val saveFile = File(getSavePath(node.data, pathSuffix, projectBasePath))
        val saveParent = saveFile.parentFile
        if (!saveParent.exists()) {
            saveParent.mkdirs()
        }
        if (!saveFile.exists()) {
            saveFile.createNewFile()
        }
        val str = saveFile.readText()
        val edgePairInFile = Json.decodeFromString<Graph.EdgePair<String>>(str)
        edgePairInFile += edgePair
        saveFile.outputStream().use {
            it.write(Json.encodeToString(edgePairInFile).toByteArray())
        }
    }*/
    val saveFile = File(
        "${projectBasePath}/${pluginCacheFolderName}/${pathSuffix}/" +
                "DependencyGraph.json"
    )
    val saveParent = saveFile.parentFile
    if (!saveParent.exists()) {
        saveParent.mkdirs()
    }
    if (!saveFile.exists()) {
        saveFile.createNewFile()
    }
    saveFile.outputStream().use {
        it.write(json1.encodeToString(this).toByteArray())
    }
}

private val json = Json { allowStructuredMapKeys = true }

fun Graph<String>.toJson(): String =
    json.encodeToString(this)

fun graphFromJson(str: String): Graph<String> =
    json.decodeFromString(str)


private operator fun <T> Graph.EdgePair<T>.plusAssign(other: Graph.EdgePair<T>) {
    other.edgeOut.forEach {
        if (!edgeOut.contains(it)) {
            edgeOut.add(it)
        }
    }
    other.edgeIn.forEach {
        if (!edgeIn.contains(it)) {
            edgeIn.add(it)
        }
    }
}
