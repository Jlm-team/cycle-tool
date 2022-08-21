package team.jlm.entity

data class Node(
    val id: Int,
    val label: String,
    val width: Int,
    val height: Int
)

data class Edge(
    val source: String,
    val target: String
)

data class GraphBean(
    val id:String,
    val node: ArrayList<Node>,
    val edge: ArrayList<Edge>
)