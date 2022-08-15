package com.xyzboom.algorithm.graph

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modifyModules
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import team.jlm.utils.file.excludePluginBaseFolder
import team.jlm.utils.file.getPluginFoldrPath
import team.jlm.utils.file.getSavePath
import team.jlm.utils.file.pluginBaseFoldrExist
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
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

fun Graph<String>.saveAsDependencyGraph(pathSuffix: String,projectBasePath:String,event: AnActionEvent) {
    for ((node, edgePair) in adjList) {
        if(!pluginBaseFoldrExist(projectBasePath))
        {
            val module = event.getData(LangDataKeys.MODULE)
            val vsfiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            module?.let { vsfiles?.let { it1 -> excludePluginBaseFolder(it, it1, getPluginFoldrPath(projectBasePath)) } }
        }
        val saveFile = File(getSavePath(node.data, pathSuffix,projectBasePath,null))
        val saveParent = saveFile.parentFile
        if (!saveParent.exists()) {
            saveParent.mkdirs()
        }
        if (!saveFile.exists()) {
            saveFile.createNewFile()
        }
        val str = saveFile.readText()
        var edgePairInFile = Gson().fromJson(str, edgePair.javaClass)
        if (edgePairInFile != null) {
            edgePairInFile += edgePair
        } else {
            edgePairInFile = edgePair
        }
        saveFile.outputStream().use {
            it.write(Gson().toJson(edgePairInFile).toByteArray())
        }
    }
}

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
