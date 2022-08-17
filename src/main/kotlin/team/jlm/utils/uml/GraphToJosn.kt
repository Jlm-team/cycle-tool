package team.jlm.utils.uml

import com.google.gson.Gson
import com.xyzboom.algorithm.graph.Graph
import team.jlm.utils.file.checkFilePath
import team.jlm.utils.file.getFileSeparator
import team.jlm.utils.file.getSavePath
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.Charset

private fun buildJson(graph: Graph<String>): String {
    val sb = StringBuilder()
    sb.append("{")
    sb.append("\"node\":[")
    for (i in graph.adjList.keys) {
        sb.append("{")
        sb.append("\"id\":").append(i).append(",")
        sb.append("\"width\":100,\"height\":50,")
        sb.append("\"label\":").append(i)
        sb.append("},")
    }
    sb.append("],")
    sb.append("\"edge\":[")
    for (i in graph.adjList) {
        val inEdge = i.value.edgeIn
        for (edge in inEdge) {
            sb.append("{")
            sb.append("\"source\":").append(edge.nodeFrom.data).append(",")
            sb.append("\"target\":").append(edge.nodeTo.data)
            sb.append("},")
        }
    }
    sb.append("]")
    sb.append("}")
    return sb.toString()
}

private fun deSerialization(obj: String): Graph<String> {
    return Gson().fromJson(obj, Graph<String>().javaClass)
}

fun jsonToMemory(graphsPath: String,projectBasePath:String) {
    try {
        val file = File(graphsPath)
        if (file.list() == null)
            throw IOException("No permissions!")
        if (!file.list()!!.isNotEmpty()) {
            throw IOException("There are no records of builds in the directory!")
        } else {
            for (i in file.listFiles(FileFilter { it.isFile && it.name.endsWith("json") })!!) {
                val savePath = getSavePath(null,"graph"+ getFileSeparator()+System.currentTimeMillis().toString(),projectBasePath)
                checkFilePath(savePath,i.name)
                val outputStream = FileOutputStream(File(savePath+ getFileSeparator()+i.name),true)
                val outputChannel = outputStream.channel
                val inputStream = FileInputStream(i)
                val inputChannel = inputStream.channel
                val inputbuffer = ByteBuffer.allocate(1024)
                var length = -1
                val sb = StringBuilder()
                while (inputChannel.read(inputbuffer).also { length = it } !== -1) {
                    inputbuffer.clear()
                    sb.append(String(inputbuffer.array(), 0, length))
                }
                inputChannel.close()
                inputStream.close()
                val graph = deSerialization(sb.toString())
                val json = buildJson(graph)
                val outputBuffer = Charset.forName("utf8").encode(json)
                while(outputChannel.write(outputBuffer)!=0){
                    continue
                }
                outputChannel.close()
                outputStream.close()
            }

        }
    } catch (e: IOException) {
        throw e
    }
}
