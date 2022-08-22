package team.jlm.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import team.jlm.entity.GraphBean
import team.jlm.utils.packJson
import team.jlm.utils.testGraph
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

    @Get("/api/test")
    fun test(): HttpResponse<GraphBean> {
        return HttpResponse.created(testGraph())
    }
}
