package algs4;

import java.util.*;
import java.util.stream.Collectors;

public class MSVC {
    private HashMap<Edge, Vector<Byte>> missingColoursOf;
    private final Random rng;
    private HashSet<Edge> edgeSet;
    private HashSet<Short> vertexSet;
    /**
     * The maximum degree of a node in our graph.
     * Denoted Delta in Bernshteyn's paper
     */
    private byte maxDegree; // currently limited to 4 because of the long size of Java
    /**
     * The maximum path length we are ready to accept, which will be 400*maxDegree^13.
     * Denoted l in Bernshteyn's paper
     */
    private int pathMaxLength; //using int, the limit before overflowing is Delta=4 :-/ Cannot use BigInteger because cannot address afterwards. Might be interesting to switch to lower level language to make it work ? Cannot use long because cannot address arrays in long :-/

    public MSVC() {
        rng = new Random();
    }

    public MSVC(long seed) {
        rng = new Random(seed);
    }

    public HashMap<Edge, Byte> edgeColouring(Vector<Vector<Short>> graph, byte maxDegree) { //TODO must be dealt with, needs a conversion
        Edge.setMaxDegree(maxDegree);
        this.maxDegree = maxDegree;
        edgeSet = new RandomHashSet<>(rng);
        RandomHashSet<Edge> uncolouredEdges = createFields(graph);

        HashMap<Edge, Byte> colouring = new HashMap<>(edgeSet.size());

        Iterator<Edge> itU;
        do {
            itU = uncolouredEdges.iterator();
            Edge edge = itU.next();
            Vector<Edge> vizingChain = MSVA(colouring, edge, edge.x);
            colouring = augmentWith(colouring, vizingChain);
            uncolouredEdges.remove(edge);
        } while (itU.hasNext());

        return colouring;
    }

    private RandomHashSet<Edge> createFields(Vector<Vector<Short>> graph) {
        RandomHashSet<Edge> uncolouredEdges = new RandomHashSet<>(rng);
        for (short i = 0; i < graph.size(); i++) {
            for (short j :
                    graph.get(i)) {
                Edge currentEdge = new Edge(i, j);
                edgeSet.add(currentEdge);

                uncolouredEdges.add(currentEdge);

                Vector<Byte> missingColours = new Vector<>(maxDegree);
                for (byte k = 1; k <= maxDegree; k++) {
                    missingColours.add(k);
                }

                missingColoursOf.put(currentEdge, missingColours);
            }
            vertexSet.add(i);
        }
        pathMaxLength = maxDegree; // square and multiply with 13, our exponent so      1 : S&M
        pathMaxLength *= pathMaxLength * maxDegree; //(13 is represented top to bottom  1 : S&M
        pathMaxLength *= pathMaxLength; // from highest bit to lowest bit)              0 : S
        pathMaxLength *= pathMaxLength * maxDegree;//                                   1 : S&M
        pathMaxLength *= 400;

        return uncolouredEdges;
    }

    private Vector<Edge> MSVA(HashMap<Edge, Byte> colouring, Edge edge, int x) {
        HashMap<Edge, Boolean> visitedEdges = new HashMap<>(edgeSet.size());
        HashMap<Short, Boolean> visitedVertices = new HashMap<>(vertexSet.size());
        for (Edge e :
                edgeSet) {
            visitedEdges.put(e, false);
        }

        for (short i = 0; i < vertexSet.size(); i++) {
            visitedVertices.put(i, false);
        }

        Vector<Object> firstChainReturn = FirstChain(colouring, edge, x);
        Vector<Edge> firstFan = (Vector<Edge>) firstChainReturn.get(0);
        Vector<Edge> firstPath = (Vector<Edge>) firstChainReturn.get(1);
        Vector<Vector<Edge>> chainAsConcat = new Vector<>();
        Vector<Edge> firstEdge = new Vector<>();
        firstEdge.add(edge);
        chainAsConcat.add(firstEdge);

        HashMap<Edge, Byte> localColouring;
        localColouring = (HashMap<Edge, Byte>) colouring.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)); // shamelessly adapted from sprinter's code on Stack Overflow
        int k = 0;
        boolean shortEnough = false;

        int comparator = pathMaxLength * 2;
        while (!shortEnough) {
            if (firstPath.size() <= comparator) {
                shortEnough = successfulChain(chainAsConcat, firstFan, firstPath);
            } else {
                //    v|v Denoted l^prime in Bernshteyn's paper
                int shorteningDistance = rng.nextInt(pathMaxLength, comparator); // since excludes the bound, -1 isn't needed
                Vector<Edge> currentFan = firstFan;
                Vector<Edge> currentPath = (Vector<Edge>) firstPath.subList(0, shorteningDistance + 1);
                updateVisited(currentFan, visitedVertices, currentPath, visitedEdges);
                Edge lastOfPath = currentPath.lastElement(); // Denoted uv in Bernshteyn's
                short lastVOfLastP = lastOfPath.y; // Denoted v in Bernshteyn's
                Vector<Edge> chainToShift = new Vector<>(currentFan);
                chainToShift.addAll(currentPath.subList(1, currentPath.size()));


                byte lastColour = localColouring.get(currentPath.lastElement()); // denoted beta in Bernsheyn's
                byte secondToLastColour = localColouring.get(currentPath.get(currentPath.size() - 2)); // denoted alpha in Bernshteyn's

                localColouring = shift(localColouring, chainToShift, true);
                Vector<Object> nextChainReturn = nextChain(localColouring, lastOfPath, lastVOfLastP, secondToLastColour, lastColour);
                Vector<Edge> FTilde = (Vector<Edge>) nextChainReturn.get(0);
                Vector<Edge> PTilde = (Vector<Edge>) nextChainReturn.get(1);
                int[] visitedResult = alreadyVisited(FTilde, PTilde);
                boolean intersection = visitedResult[0] != 0;
                int intersectionPosition = visitedResult[1]; // Denoted j in Bernshteyn's
                if (intersection) {
                    Vector<Edge> toRevert = new Vector<>();
                    toRevert.addAll(chainAsConcat.get(intersectionPosition));
                    for (int i = intersectionPosition + 1; i < 2 * k; i++) {
                        Vector<Edge> chainToAdd = chainAsConcat.get(i);
                        toRevert.addAll(chainToAdd.subList(0, chainToAdd.size()));
                    }
                    localColouring = shift(localColouring, toRevert, false);
                    updateRemoved(intersectionPosition, k, chainAsConcat, visitedVertices, visitedEdges);
                    firstFan = chainAsConcat.get(intersectionPosition);
                    firstPath = chainAsConcat.get(intersectionPosition + 1);// TODO how to get P' ? might have to store it. Don't wanna think about it right now...
                    chainAsConcat = new Vector<>(chainAsConcat.subList(0, intersectionPosition));
                    k = intersectionPosition / 2; //intersectionPosition should be even :-/
                } else {
                    chainAsConcat.add(currentFan);
                    chainAsConcat.add(new Vector<>(currentPath.subList(1, currentPath.size())));
                    firstFan = FTilde;
                    firstPath = PTilde;
                    k++;
                }
                // cannot use the "sublist" function of Vector because "only" uses int, which would mean Delta limited to 4.
            } // cannot use BigInteger nor long because won't work, as Java "only" addresses in int:-/
        } // limitation
        Vector<Edge> chain = new Vector<>();
        for (Vector<Edge> innerChain :
                chainAsConcat) {
            chain.addAll(innerChain);
        }
        return chain;
    }

    //TODO change the ugly uses of Vector<Object> as return to records. If you have time of course :D
    private static boolean successfulChain(Vector<Vector<Edge>> chainAsConcat, Vector<Edge> firstFan, Vector<Edge> firstPath) {
        boolean shortEnough;
        shortEnough = true;
        chainAsConcat.add(firstFan);
        chainAsConcat.add(firstPath);
        return shortEnough;
    }

    private static void updateVisited(Vector<Edge> currentFan, HashMap<Short, Boolean> visitedVertices, Vector<Edge> currentPath, HashMap<Edge, Boolean> visitedEdges) {
        for (Edge visitedEdge :
                currentFan) {
            visitedVertices.put(visitedEdge.x, true);
            visitedVertices.put(visitedEdge.y, true);
        }
        for (int i = 1; i < currentPath.size() - 1; i++) {
            visitedEdges.put(currentPath.get(i), true);
        }
    }

    private static void updateRemoved(int intersectionPosition, int k, Vector<Vector<Edge>> chainAsConcat, HashMap<Short, Boolean> visitedVertices, HashMap<Edge, Boolean> visitedEdges) {
        for (int i = intersectionPosition; i < 2 * k; i += 2) {
            Vector<Edge> removedFan = chainAsConcat.get(i);
            for (Edge removedEdge :
                    removedFan) {
                visitedVertices.put(removedEdge.x, false);
                visitedVertices.put(removedEdge.y, false);
            }
        }
        for (int i = intersectionPosition + 1; i < 2 * k; i += 2) {
            Vector<Edge> removedPath = chainAsConcat.get(i);
            for (Edge removedEdge :
                    removedPath.subList(1, removedPath.size() - 1)) {
                visitedEdges.put(removedEdge, false);
            }
        }
    }

    private int[] alreadyVisited(Vector<Edge> fTilde, Vector<Edge> pTilde) {
        //TODO
        return null;
    }

    private Vector<Object> nextChain(HashMap<Edge, Byte> localColouring, Edge lastOfPath, short lastVOfLastP, byte secondToLastColour, byte lastColour) {
        //Todo
        return null;
    }

    private HashMap<Edge, Byte> shift(HashMap<Edge, Byte> localColouring, Vector<Edge> chainToPass, boolean ascending) {
        //TODO
        return null;
    }

    private Vector<Object> FirstChain(HashMap<Edge, Byte> colouring, Edge edge, int x) {
        //TODO
        return null;
    }

    private Vector<Object> createFirstFan(HashMap<Edge, Integer> colouring, Edge edge, int edgeX) {
        //TODO
        return null;
    }

    private HashMap<Edge, Byte> augmentWith(HashMap<Edge, Byte> colouring, Vector<Edge> vizingChain) {
        //TODO
        return null;
    }

    class Edge {
        private static byte maxDegree;
        private final short x, y; // size because if goes higher, then might need 64GB of memory. Currently needs max 8 for those two shorts :-/


        public Edge(short x, short y) {
            if (x > y) {
                this.x = x;
                this.y = y;
            } else {
                this.x = y;
                this.y = x;
            }
        }

        public static void setMaxDegree(byte maxDegree) {
            Edge.maxDegree = maxDegree;
        }

        @Override
        public int hashCode() {
            return maxDegree * x + y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return x == edge.x && y == edge.y;
        }

    }

}


