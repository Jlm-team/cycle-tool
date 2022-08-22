package team.jlm.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import team.jlm.entity.GraphBean
import team.jlm.utils.packJson
import java.io.IOException

@Controller("/")
class GraphController {
    @Get("/{path}")
    fun showGraph(@PathVariable path: String): HttpResponse<ArrayList<GraphBean>> {
        var graphs = ArrayList<GraphBean>()
        try {
            graphs = packJson(path)
        } catch (e: IOException) {
            return HttpResponse.created(graphs)
        }
        return HttpResponse.created(graphs)
    }

    @Get("/")
    fun test(): HttpResponse<ArrayList<GraphBean>> {
        var graphs = ArrayList<GraphBean>()
        try {
            graphs = packJson("G:\\test")
        } catch (e: IOException) {
            return HttpResponse.created(graphs)
        }
        return HttpResponse.created(graphs)
    }
}
