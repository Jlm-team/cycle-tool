package graph;

import team.jlm.utils.graph.GNode;
import team.jlm.utils.graph.Graph;
import team.jlm.utils.graph.GraphUtilsKt;
import team.jlm.utils.graph.NodeNotInGraphException;
import org.junit.Test;

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
        graph.addEdge(nodes.get(0), nodes.get(1));//A -> B
        assert graph.getAdjList().get(new GNode<>("A")).getEdgeOut().size() == 1;
        graph.addEdge(nodes.get(0), nodes.get(2));//A -> C
        graph.addEdge(nodes.get(1), nodes.get(3));//B -> D
        graph.addEdge(nodes.get(1), nodes.get(4));//B -> E
        graph.addEdge(nodes.get(2), nodes.get(3));//C -> D
        graph.addEdge(nodes.get(3), nodes.get(4));//D -> E
        graph.addEdge(nodes.get(3), nodes.get(5));//D -> F
        String jsonGraph = GraphUtilsKt.toJson(graph);
        System.out.println(jsonGraph);
        Graph<String> graph1 = GraphUtilsKt.graphFromJson(jsonGraph);
        assert graph.equals(graph1);
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
        graph.delNode("B");
        System.out.println();
        graph.dfsVisit(stringGNode -> {
            System.out.println(stringGNode.getData());
            return null;
        });
        graph.plusAssign(graph);
        graph.dfsVisit(stringGNode -> {
            System.out.println(stringGNode.getData());
            return null;
        });
    }
}
