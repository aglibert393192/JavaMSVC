package algs4;

import java.util.*;

public class MSVC {
    private boolean[][] missing;

    Vector<Vector<Integer>> graph;
    Random rng;

    public MSVC() {
        rng = new Random();
    }

    public MSVC(long seed) {
        rng = new Random(seed);
    }

    public Vector<Vector<Integer>> edgeColouring(Vector<Vector<Integer>> graph, int maxDegree) {
        RandomHashSet<Edge> edges = new RandomHashSet<>(rng);
        RandomHashSet<Edge> uncolouredEdges = createUncoloured(graph, edges);

        Vector<Vector<Integer>> colouring = new Vector<>();

        missing = new boolean[uncolouredEdges.size()][maxDegree];

        while (!uncolouredEdges.isEmpty()) {
            /*int edgeIndex = rng.nextInt(uncolouredEdges.size());
            int[] edge = uncolouredEdges.get(edgeIndex);
            int edgeX = edge[0];
            int edgeY = edge[1];

            Vector<Vector<Integer>> vizingChain = createVizingChain(colouring, edgeY, edgeX);
            colouring = augmentWith(colouring, vizingChain);
            uncolouredEdges.remove(edge);*/
        }
        return colouring;
    }

    private RandomHashSet<Edge> createUncoloured(Vector<Vector<Integer>> graph, RandomHashSet<Edge> edgeSet) {
        RandomHashSet<Edge> uncolouredEdges = new RandomHashSet<>(rng);
        for (int i = 0; i < graph.size(); i++) {
            for (int j :
                    graph.get(i)) {
                Edge currentEdge = new Edge(i, j);
                edgeSet.add(currentEdge);


                Edge currentUEdge = new Edge(i, j);
                uncolouredEdges.add(currentUEdge);
            } // TODO must rethink about it
        }
        return uncolouredEdges;
    }

    private Vector<Vector<Integer>> createVizingChain(Vector<Vector<Integer>> colouring, int edgeY, int edgeX) {
        Vector<Object> firstFanReturn = createFirstFan(colouring, edgeY, edgeX);
        Vector<int[]> firstFan = (Vector<int[]>) firstFanReturn.get(0);
        int beta = (int) firstFanReturn.get(1);
        int j = (int) firstFanReturn.get(2);
//        missingOfEdge = missing[]

        return null;
    }

    private Vector<Object> createFirstFan(Vector<Vector<Integer>> colouring, int edgeY, int edgeX) {
        //TODO
        return null;
    }

    private Vector<Vector<Integer>> augmentWith(Vector<Vector<Integer>> colouring, Vector<Vector<Integer>> vizingChain) {
        //TODO
        return null;
    }

    private class Edge {
        long x, y;

        public Edge(long x, long y) {
            this.x = x;
            this.y = y;
        }
    }

}


