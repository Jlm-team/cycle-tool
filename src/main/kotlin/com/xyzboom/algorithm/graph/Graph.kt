package com.xyzboom.algorithm.graph

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/**
 * 图的边，不指定长度时默认为1
 * @param nodeTo 边指向的节点
 * @param nodeFrom 边到来的节点
 * @param length 边的长度
 */
class GEdge<T>(val nodeFrom: GNode<T>, val nodeTo: GNode<T>, val length: Int = 1) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GEdge<*>

        if (nodeFrom != other.nodeFrom) return false
        if (nodeTo != other.nodeTo) return false
        if (length != other.length) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodeFrom.hashCode()
        result = 31 * result + nodeTo.hashCode()
        result = 31 * result + length
        return result
    }

    override fun toString(): String {
        return "GEdge(nodeFrom=$nodeFrom, nodeTo=$nodeTo, length=$length)"
    }
}

/**
 * 图的节点，存储出边和入边的邻接表
 * @param data 节点存储的数据
 */
class GNode<T>(val data: T) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GNode<*>

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "GNode(data=$data)"
    }
}

/**
 * 图
 */
open class Graph<T> {
    private val allowSelfRing = false

    class EdgePair<T>(var edgeOut: HashSet<GEdge<T>>, var edgeIn: HashSet<GEdge<T>>)

    val nodes = { adjList.keys }
    val adjList = HashMap<GNode<T>, EdgePair<T>>()

    /**
     * 将提供的数据加到图中
     * @param data 需要添加的数据
     * @return 返回添加的节点的[GNode]对象
     */
    open fun addNode(data: T): GNode<T> {
        val result = GNode(data)
        if (adjList.contains(result)) {
            return result
        }
        adjList[result] = EdgePair(HashSet(), HashSet())
        return result
    }

    open fun delNode(data: T) {
        val node = getNode(data)
        for (edge in adjList[node]!!.edgeOut) {
            adjList[edge.nodeTo]!!.edgeIn.remove(edge)
        }
        for (edge in adjList[node]!!.edgeIn) {
            adjList[edge.nodeFrom]!!.edgeOut.remove(edge)
        }
        adjList.remove(node)
    }

    /**
     * 根据指定的节点将提供的边加入图中
     * @param from 边到来的节点
     * @param to 边到达的节点
     * @throws [NodeNotInGraphException] 如果指定的节点不在图中
     * @return 返回添加的节点的[GEdge]对象
     */
    @kotlin.jvm.Throws(NodeNotInGraphException::class)
    @JvmOverloads
    open fun addEdge(from: GNode<T>, to: GNode<T>, length: Int = 1): GEdge<T> {
        checkNode(from)
        checkNode(to)
        val newEdge = GEdge(from, to, length)
        adjList[from]!!.edgeOut.add(newEdge)
        adjList[to]!!.edgeIn.add(newEdge)
//        from.edgeOut.add(newEdge)
//        to.edgeIn.add(newEdge)
//        edges.add(newEdge)
        return newEdge
    }

    fun getNode(nodeData: T): GNode<T>? {
        adjList.forEach {
            if (it.key.data == nodeData) {
                return it.key
            }
        }
        return null
    }

    @JvmOverloads
    open fun addEdge(from: T, to: T, length: Int = 1): GEdge<T> {
        if (!allowSelfRing && from == to) {
            return GEdge(GNode(from), GNode(to), length)//虽然返回正常的值，但是因为不允许自环，所以不加入到图中
        }
        val fromNode = getNode(from) ?: addNode(from)
        val toNode = getNode(to) ?: addNode(to)
        return addEdge(fromNode, toNode, length)
    }

    open fun clone(transpose: Boolean = false): Graph<T> {
        val result = Graph<T>()
        val newNodeMap = HashMap<GNode<T>, GNode<T>>()
        for (node in adjList.keys) {
            val newNode = GNode(node.data)
            newNodeMap[node] = newNode
            result.adjList[newNode] = EdgePair(HashSet(), HashSet())
        }
        for (edgePair in adjList.values) {
            //只需要检查一条边就行
            for (edgeOut in edgePair.edgeOut) {
                if (transpose) {
                    result.addEdge(edgeOut.nodeTo, edgeOut.nodeFrom, edgeOut.length)
                } else {
                    result.addEdge(edgeOut.nodeFrom, edgeOut.nodeTo, edgeOut.length)
                }
            }
        }
        return result
    }

    /**
     * 是否包含某个节点
     * @param node 需要检查的节点
     */
    fun containsNode(node: GNode<T>): Boolean {
        return adjList.contains(node)
    }

    /**
     * 检查是否包含某个节点
     * @param node 需要检查的节点
     * @throws NodeNotInGraphException 如果检查失败
     */
    @kotlin.jvm.Throws(NodeNotInGraphException::class)
    fun checkNode(node: GNode<T>) {
        if (!containsNode(node)) {
            throw NodeNotInGraphException("Node $node does not in graph")
        }
    }

    /**
     * 从指定的节点进行广度优先遍历，仅遍历节点所引出的连通分量
     * @param start 遍历起始的节点
     * @param visitor 遍历器
     */
    @kotlin.jvm.Throws(NodeNotInGraphException::class)
    inline fun bfsVisit(start: GNode<T>, visitor: (GNode<T>) -> Unit) {
        checkNode(start)
        val m = HashSet<GNode<T>>()
        val q: Queue<GNode<T>> = LinkedList()
        q.add(start)
        m.add(start)
        bfsVisit(q, visitor, m)
    }

    /**
     * 从临时序列开始广度优先遍历
     * @param q 还未遍历的队列
     * @param visitor 遍历器
     * @param m 已经遍历的节点集合
     */
    inline fun bfsVisit(q: Queue<GNode<T>>, visitor: (GNode<T>) -> Unit, m: HashSet<GNode<T>>) {
        while (!q.isEmpty()) {
            val now = q.poll()
            visitor(now)
            val edgeOut = adjList[now]?.edgeOut ?: return
            for (edge in edgeOut) {
                if (!m.contains(edge.nodeTo)) {
                    q.add(edge.nodeTo)
                    m.add(edge.nodeTo)
                }
            }
        }
    }

    /**
     * 对全图进行广度优先遍历，起始位置不定
     * @param visitor 遍历器
     */
    inline fun bfsVisit(visitor: (GNode<T>) -> Unit) {
        val m = HashSet<GNode<T>>()
        val q: Queue<GNode<T>> = LinkedList()
        while (true) {
            if (m.size == adjList.keys.size) {
                break
            }
            for (node in adjList.keys) {
                if (!m.contains(node)) {
                    q.add(node)
                    m.add(node)
                    break
                }
            }
            bfsVisit(q, visitor, m)
        }
    }

    /**
     * 从临时序列开始深度优先遍历
     * @param q 还未遍历的栈
     * @param visitor 遍历器
     * @param m 已经遍历的节点集合
     */
    inline fun dfsVisit(q: Stack<GNode<T>>, visitor: (GNode<T>) -> Unit, m: HashSet<GNode<T>>) {
        while (!q.isEmpty()) {
            val now = q.pop()
            visitor(now)
            val edgeOut = adjList[now]?.edgeOut ?: return
            for (edge in edgeOut) {
                if (!m.contains(edge.nodeTo)) {
                    q.push(edge.nodeTo)
                    m.add(edge.nodeTo)
                }
            }
        }
    }

    /**
     * 对全图进行深度优先遍历，起始位置不定
     * @param visitor 遍历器
     */
    inline fun dfsVisit(visitor: (GNode<T>) -> Unit) {
        val m = HashSet<GNode<T>>()
        val q = Stack<GNode<T>>()
        while (true) {
            if (m.size == adjList.keys.size) {
                break
            }
            for (node in adjList.keys) {
                if (!m.contains(node)) {
                    q.push(node)
                    m.add(node)
                    break
                }
            }
            dfsVisit(q, visitor, m)
        }
    }

    /**
     * 从指定的节点进行广度优先遍历，仅遍历节点所引出的连通分量
     * @param start 遍历起始的节点
     * @param visitor 遍历器
     */
    @kotlin.jvm.Throws(NodeNotInGraphException::class)
    inline fun dfsVisit(start: GNode<T>, visitor: (GNode<T>) -> Unit) {
        checkNode(start)
        val m = HashSet<GNode<T>>()
        val q = Stack<GNode<T>>()
        q.push(start)
        m.add(start)
        dfsVisit(q, visitor, m)
    }

    operator fun plusAssign(other: Graph<T>) {
        other.adjList.forEach { (gNode, edgePair) ->
            run {
                if (!adjList.containsKey(gNode)) {
                    adjList[gNode] = edgePair
                } else {
                    adjList[gNode]!!.edgeIn.addAll(edgePair.edgeIn)
                    adjList[gNode]!!.edgeOut.addAll(edgePair.edgeOut)
                }
            }
        }
    }

    override fun toString(): String {
        val result = StringBuilder()
        adjList.forEach { (gNode, edgePair) ->
            run {
                if (edgePair.edgeOut.isEmpty()) return@run
                result.append(gNode.data.toString())
                result.append(": \n\t")
                edgePair.edgeOut.forEach {
                    result.append("${it.nodeTo.data.toString()}, ")
                }
                result.append("\n")
            }
        }
        return result.toString()
    }

}
