package team.jlm.entity

data class Node(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val shape:String
)

data class Edge(
    val source: String,
    val target: String,
    val shape:String
)


data class GraphBean(
    val id:String,
    val nodes: ArrayList<Node>,
    val edges: ArrayList<Edge>
)