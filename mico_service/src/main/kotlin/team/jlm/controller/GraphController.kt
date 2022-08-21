package team.jlm.controller

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.PathVariable
import team.jlm.entity.GraphBean
import team.jlm.utils.packJson

@Controller("/")
class GraphController {
    @Get("/{path}")
    fun showGraph(@PathVariable path: String):HttpResponse<ArrayList<GraphBean>>{
        val graphs = packJson(path)
        return HttpResponse.created(graphs)
    }
}