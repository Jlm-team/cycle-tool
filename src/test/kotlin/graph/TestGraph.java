package graph;

import com.xyzboom.algorithm.graph.GNode;
import com.xyzboom.algorithm.graph.Graph;
import com.xyzboom.algorithm.graph.NodeNotInGraphException;
import org.junit.Test;
import team.jlm.coderefactor.util.PluginUtilsKt;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class TestGraph {
    @Test
    public void testGraph() throws NodeNotInGraphException {
        Graph<String> graph = new Graph<String>();
        ArrayList<GNode<String>> nodes = new ArrayList<>();
        for (int i = 0; i < 6; ++i) {
            nodes.add(graph.addNode(String.valueOf((char) ('A' + (char) i))));
        }
        graph.addEdge(nodes.get(0), nodes.get(1));//A -> B
        graph.addEdge(nodes.get(0), nodes.get(2));//A -> C
        graph.addEdge(nodes.get(1), nodes.get(3));//B -> D
        graph.addEdge(nodes.get(1), nodes.get(4));//B -> E
        graph.addEdge(nodes.get(2), nodes.get(3));//C -> D
        graph.addEdge(nodes.get(3), nodes.get(4));//D -> E
        graph.addEdge(nodes.get(3), nodes.get(5));//D -> F
        graph.bfsVisit(stringGNode -> {
            System.out.println(stringGNode.getData());
            return null;
        });
        System.out.println();
        graph.dfsVisit(stringGNode -> {
            System.out.println(stringGNode.getData());
            return null;
        });
        System.out.println();
        Graph<String> newG = graph.clone(true);
        newG.bfsVisit(stringGNode -> {
            System.out.println(stringGNode.getData());
            return null;
        });
    }
}
