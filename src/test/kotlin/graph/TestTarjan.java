package graph;

import com.xyzboom.algorithm.graph.GNode;
import com.xyzboom.algorithm.graph.Graph;
import com.xyzboom.algorithm.graph.NodeNotInGraphException;
import com.xyzboom.algorithm.graph.Tarjan;
import org.junit.Test;

import java.util.ArrayList;

public class TestTarjan {
    @Test
    public void testTarjan() throws NodeNotInGraphException {
        Graph<String> graph = new Graph<String>();
        ArrayList<GNode<String>> nodes = new ArrayList<>();
        for (int i = 0; i < 8; ++i) {
            nodes.add(graph.addNode(String.valueOf((char) ('A' + (char) i))));
        }
        graph.addEdge(nodes.get(0), nodes.get(1));//A -> B
        graph.addEdge(nodes.get(0), nodes.get(2));//A -> C
        graph.addEdge(nodes.get(1), nodes.get(3));//B -> D
        graph.addEdge(nodes.get(3), nodes.get(0));//D -> A
        graph.addEdge(nodes.get(2), nodes.get(3));//C -> D
        graph.addEdge(nodes.get(2), nodes.get(4));//C -> E
        graph.addEdge(nodes.get(3), nodes.get(5));//D -> F
        graph.addEdge(nodes.get(4), nodes.get(5));//E -> F
        graph.addEdge(nodes.get(6), nodes.get(7));//G -> H
        graph.addEdge(nodes.get(7), nodes.get(6));//H -> G
        Tarjan<String> tarjan = new Tarjan<>(graph);
        ArrayList<ArrayList<GNode<String>>> result = tarjan.getResult();
        for (int i = 0; i < result.size(); i++) {
            System.out.println("scc " + i + " :");
            for (int j = 0; j < result.get(i).size(); j++) {
                System.out.print(result.get(i).get(j).getData() + " ");
            }
            System.out.println();
        }
    }
}
