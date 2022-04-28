import com.intellij.psi.search.SearchScope
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory
import java.io.File

fun main() {
    val g = Factory.graph()
        .directed().with(Factory.node("a").link(Factory.node("b")))
    val img = Graphviz.fromGraph(g).render(Format.PNG).toFile(
         File("a.png")
    )
}