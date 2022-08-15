package team.jlm.utils.uml

import com.xyzboom.algorithm.graph.Graph
import team.jlm.utils.file.getFileSeparator
import team.jlm.utils.file.getSavePath
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Files;
import java.nio.file.Paths;

fun buildJson(graph: Graph<String>,label: String): String {
    val sb = StringBuilder()
    sb.append("[label:").append(label).append("{")
    sb.append("node:[")
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
    sb.append("}").append("],")
    return sb.toString()
}

private fun jsonToFile(json: String,fileWirter:FileChannel){
    val buffer  = ByteBuffer.wrap(json.toByteArray(Charsets.UTF_8))
    fileWirter.write(buffer)
//    val fileName = System.currentTimeMillis().toString()
//    val suffix = "Logs"+getFileSeparator()+ fileName
//    val path = getSavePath(null,suffix,filePath, "$fileName.json")

//    val file = File(getSavePath())
//    if(!file.exists() &&!file.isDirectory){
//        file.mkdir()
//    }

}

fun jsonToFile(graphsPath:String){

}