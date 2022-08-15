package team.jlm.utils.uml

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.xyzboom.algorithm.graph.Graph
import team.jlm.utils.file.getSavePath
import java.io.File

fun buildJson(graph: Graph<String>): JsonObject {
    val sb = StringBuilder()
    sb.append("{node:[")
    for (i in graph.adjList.keys) {
        sb.append("{")
        sb.append("id:").append(i).append(",")
        sb.append("width:100,height:50,")
        sb.append("label:").append(i)
        sb.append("},")
    }
    sb.append("],")
    sb.append("edge:[")
    for (i in graph.adjList) {
        val inEdge = i.value.edgeIn
        for (edge in inEdge) {
            sb.append("{")
            sb.append("source:").append(edge.nodeFrom.data).append(",")
            sb.append("target:").append(edge.nodeTo.data)
            sb.append("},")
        }
    }
    sb.append("]")
    sb.append("}")
    return Gson().fromJson(sb.toString(),JsonObject().javaClass)
}

fun jsonToFile(json: JsonObject,baseProjectPath:String){
//    val file = File(getSavePath())
//    if(!file.exists() &&!file.isDirectory){
//        file.mkdir()
//    }

}