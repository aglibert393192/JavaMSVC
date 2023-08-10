package utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MSVCTest {

    MSVC msvc;
    Vector<Vector<Integer>> graph;
    private HashMap<Integer[], Byte> colouring;

    private byte maxDegree;

    @BeforeEach
    void setUp() {
        msvc = new MSVC(42);
    }

    @Test
    void simpleGraph() {
        graph = buildSimpleGraph();
        colouring = msvc.edgeColouring(graph, (byte) 3);
        assertTrue(msvc.testColouring(colouring, graph), "The simple graph doesn't seem good");
    }

    @Test
    void MSVCGraph() {
        graph = buildMSVCGraphD3();
        colouring = msvc.edgeColouring(graph, (byte) 3);
        assertTrue(msvc.testColouring(colouring, graph));
    }

    @Test
    void randomGraph() {
        buildRandomBigGraph(1000, (byte) 5, );
        colouring = msvc.edgeColouring(graph, maxDegree);
        assertTrue(msvc.testColouring(colouring, graph));
    }

    @Test
    void longVizingChains() {
        build1BigVizingChainsD2();
        colouring = msvc.edgeColouring(graph, (byte) 3);
        assertTrue(msvc.testColouring(colouring, graph));
    }

    private void build1BigVizingChainsD2() {
        Vector<ArrayDeque<Vector<Integer>>> tmp = new Vector<>();
        maxDegree = 2;
        int pathSize = 3276802;
        int i;
        for (i = 0; i < 1; i++) {
            ArrayDeque<Vector<Integer>> graphAsAD = new ArrayDeque<>();
            Vector<Integer> adjacencyOf0 = new Vector<>(2);
            oneVizingChain(graphAsAD, adjacencyOf0, pathSize * i + 1, pathSize, maxDegree);
            tmp.add(graphAsAD);
        }
        ArrayDeque<Vector<Integer>> firstChain = tmp.firstElement();
        firstChain.getLast().remove(Integer.valueOf(pathSize + 1));
        graph = new Vector<>(firstChain);
        testGraphValidity(graph);

        System.out.println("wait for me");

    }

    @Test
    public void increasinglyLargerRandomGraphsBenchmark() {
        int seed = 42;
        for (int i = 100; i < 10000; i += 100) {
            buildRandomBigGraph(i, (byte) 5, seed);
            colouring = msvc.edgeColouring(graph, maxDegree);
            msvc.testColouring(colouring, graph);
        }
    }

    @SuppressWarnings("OverlyLongMethod")
    private static Vector<Vector<Integer>> buildSimpleGraph() {
        Vector<Vector<Integer>> graph = new Vector<>(12);
        Vector<Integer> adjacencyOf0 = new Vector<>();
        graph.add(adjacencyOf0);
        Vector<Integer> adjacencyOf1 = new Vector<>();
        adjacencyOf1.add(2);
        adjacencyOf1.add(8);
        graph.add(adjacencyOf1);
        Vector<Integer> adjacencyOf2 = new Vector<>();
        adjacencyOf2.add(1);
        adjacencyOf2.add(6);
        adjacencyOf2.add(7);
        graph.add(adjacencyOf2);
        Vector<Integer> adjacencyOf3 = new Vector<>();
        adjacencyOf3.add(5);
        adjacencyOf3.add(10);
        graph.add(adjacencyOf3);
        Vector<Integer> adjacencyOf4 = new Vector<>();
        adjacencyOf4.add(6);
        adjacencyOf4.add(11);
        graph.add(adjacencyOf4);
        Vector<Integer> adjacencyOf5 = new Vector<>();
        adjacencyOf5.add(3);
        adjacencyOf5.add(7);
        adjacencyOf5.add(11);
        graph.add(adjacencyOf5);
        Vector<Integer> adjacencyOf6 = new Vector<>();
        adjacencyOf6.add(2);
        adjacencyOf6.add(4);
        adjacencyOf6.add(11);
        graph.add(adjacencyOf6);
        Vector<Integer> adjacencyOf7 = new Vector<>();
        adjacencyOf7.add(2);
        adjacencyOf7.add(5);
        graph.add(adjacencyOf7);
        Vector<Integer> adjacencyOf8 = new Vector<>();
        adjacencyOf8.add(1);
        adjacencyOf8.add(9);
        graph.add(adjacencyOf8);
        Vector<Integer> adjacencyOf9 = new Vector<>();
        adjacencyOf9.add(8);
        adjacencyOf9.add(10);
        graph.add(adjacencyOf9);
        Vector<Integer> adjacencyOf10 = new Vector<>();
        adjacencyOf10.add(9);
        adjacencyOf10.add(3);

        graph.add(adjacencyOf10);
        Vector<Integer> adjacencyOf11 = new Vector<>();
        adjacencyOf11.add(4);
        adjacencyOf11.add(5);
        adjacencyOf11.add(6);
        graph.add(adjacencyOf11);
        testGraphValidity(graph);
        return graph;
    }

    private static Vector<Vector<Integer>> buildMSVCGraphD3() {
        ArrayDeque<Vector<Integer>> res = new ArrayDeque<>();
        int step = 15;
        int i;
        for (i = 0; i < 200; i += step) {
            Vector<Integer> adjacencyOfStartingPoint = new Vector<>(3);
            if (i != 0) {
                adjacencyOfStartingPoint.add(i);
            }
            oneVizingChain(res, adjacencyOfStartingPoint, i + 1, step, 3);

        }
        res.getLast().remove(1);
        Vector<Vector<Integer>> resAsVector = new Vector<>(res);
        testGraphValidity(resAsVector);
        return resAsVector;
    }

    private static void testGraphValidity(Vector<Vector<Integer>> resAsVector) {
        int i;
        boolean correct;
        i = 0;
        for (Vector<Integer> adjacency :
                resAsVector) {
            for (int neigh :
                    adjacency) {
                correct = resAsVector.get(neigh).contains(i);
                if (!correct)
                    throw new ExceptionInInitializerError("Graph doesn't seem to be undirected, at " + neigh + " " + adjacency);
            }
            i++;
        }
    }

    private static void oneVizingChain(ArrayDeque<Vector<Integer>> res, Vector<Integer> adjacencyOf0, int startingPoint, int step, int maxDegree) {
        int addToFan = startingPoint == 1 ? maxDegree : maxDegree - 1;
        res.addLast(adjacencyOf0);
        for (int i = startingPoint; i < startingPoint + addToFan; i++) {
            Vector<Integer> someAdjacency = new Vector<>();
            someAdjacency.add(startingPoint == 1 ? 0 : startingPoint);

            adjacencyOf0.add(i < 4 ? i : i + 1);
            res.addLast(someAdjacency);
        }
        res.getLast().add(startingPoint + maxDegree);
        for (int i = startingPoint + maxDegree; i < startingPoint + step; i++) {
            Vector<Integer> someAdjacency = new Vector<>();
            someAdjacency.add(i - 1);
            someAdjacency.add(i + 1);
            res.add(someAdjacency);
        }
    }

    private void buildRandomBigGraph(int numberOfVertices, byte edgeAddingLimit, int seed) {
        //TODO deal with the maxDegree problem somehow to be able to cover the rest of the code :-/
        Random localRng = new Random(4);
        Vector<Vector<Integer>> res = new Vector<>(numberOfVertices);
        for (int i = 0; i < numberOfVertices; i++) {
            Vector<Integer> adjacency = new Vector<>();
            res.add(adjacency);
        }
        byte realMaxDegree = 0;
        for (int i = 0; i < numberOfVertices; i++) {
            Vector<Integer> adjacencyOfI = res.get(i);
            byte numberOfAddedNeighbours = (byte) localRng.nextInt(edgeAddingLimit + 1);
            for (int j = 0; j < numberOfAddedNeighbours; j++) {
                int connectedTo;
                do {
                    connectedTo = localRng.nextInt(numberOfVertices);
                } while (connectedTo == i);
                if (!adjacencyOfI.contains(connectedTo)) {
                    adjacencyOfI.add(connectedTo);
                    Vector<Integer> adjacencyOfTo = res.get(connectedTo);
                    adjacencyOfTo.add(i);
                    realMaxDegree = (byte) Math.max(adjacencyOfTo.size(), realMaxDegree);
                }
            }
        }
        testGraphValidity(res);
        this.graph = res;
        this.maxDegree = realMaxDegree;
    }

    private void build1LongVizingChainsD3() {
        Vector<ArrayDeque<Vector<Integer>>> tmp = new Vector<>();
        maxDegree = 3;
        int pathSize = 47239201;
        for (int i = 0; i < 1; i++) {
            ArrayDeque<Vector<Integer>> graphAsAD = new ArrayDeque<>();
            Vector<Integer> adjacencyOf0 = new Vector<>(3);
            oneVizingChain(graphAsAD, adjacencyOf0, pathSize * i + 1, pathSize, maxDegree);
            tmp.add(graphAsAD);
        }
        ArrayDeque<Vector<Integer>> firstChain = tmp.firstElement();
        Iterator<Vector<Integer>> fCIt = firstChain.iterator();
        ArrayDeque<Vector<Integer>> secondChain = tmp.lastElement();
        Iterator<Vector<Integer>> sCIt = secondChain.iterator();
        tmp = null;
        secondChain.getFirst().add(pathSize - 1);
        firstChain.addAll(secondChain);
        secondChain = null;
        graph = new Vector<>(firstChain);
        testGraphValidity(graph);

        System.out.println("wait for me");

    }
}