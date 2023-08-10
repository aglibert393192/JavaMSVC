package princeton;

import edu.princeton.cs.algs4.Graph;
import utils.MSVC;

import java.util.HashMap;

import java.util.Vector;

public class algs4Colourer {
    Graph graph;
    public algs4Colourer(Graph graph) {
        this.graph = graph;
    }

    public HashMap<Integer[], Byte> colour() {
        Vector<Vector<Integer>> graphAsVecVec = new Vector<>();
        byte maxDegree = 0;
        for (int i = 0; i < graph.V(); i++) {
            Vector<Integer> adjacency = new Vector<>();
            for (int neigh :
                    graph.adj(i)) {
                adjacency.add(neigh);
            }
            maxDegree = (byte) Math.max(maxDegree, adjacency.size());
            graphAsVecVec.add(adjacency);
        }
        graph = null; // trying to clear as much memory as possible :-/
        MSVC msvc = new MSVC();
        return msvc.edgeColouring(graphAsVecVec, maxDegree);
    }
}
