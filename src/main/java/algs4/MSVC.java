package algs4;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MSVC {
    /**
     * Contains all the colours for each of the edges. Colours used are marked as "0" in their
     */
    private HashMap<Edge, HashSet<Byte>> missingColoursOf;
    private final Random rng;
    private HashSet<Edge> edgeSet;
    private HashSet<Integer> vertexSet;
    //2147483647

    /**
     * The maximum degree of a node in our graph.
     * Denoted Delta in Bernshteyn's paper
     */
    private byte maxDegree; // currently limited to 4 because of the long size of Java
    /**
     * The maximum path length we are ready to accept, which will be 400*maxDegree^13.
     * Denoted l in Bernshteyn's paper
     */
    private int pathMaxLength; //using int, the limit before overflowing is Delta=3 :-/
    // Cannot use BigInteger because cannot address afterwards. Cannot use long because cannot address arrays in long :-/

    // Might be interesting to switch to lower level language to make it work ? Only with enough memory,
    // which if we used 2 concatenated shorts (to reduce usage) would amount to

    private Vector<Vector<Integer>> graph;

    public MSVC() {
        rng = new Random();
    }

    public MSVC(long seed) {
        rng = new Random(seed);
    }

    public HashMap<Edge, Byte> edgeColouring(Vector<Vector<Integer>> graph, byte maxDegree) { //TODO must be dealt with, needs a conversion
        Edge.setNumberOfVertices(graph.size());
        this.maxDegree = maxDegree;
        edgeSet = new RandomHashSet<>(rng);
        this.graph = graph;
        HashMap<Edge, Byte> colouring = new HashMap<>(edgeSet.size());
        RandomHashSet<Edge> uncolouredEdges = createFields(graph, colouring);

        Iterator<Edge> itU;
        itU = uncolouredEdges.iterator();
        do {
            Edge edge = itU.next();
            Vector<Edge> vizingChain = MSVA(colouring, edge, edge.x);
            colouring = augmentWith(colouring, vizingChain);
            uncolouredEdges.remove(edge);
            itU = uncolouredEdges.iterator();
        } while (itU.hasNext());

        return colouring;
    }

    private RandomHashSet<Edge> createFields(Vector<Vector<Integer>> graph, HashMap<Edge, Byte> colouring) {
        RandomHashSet<Edge> uncolouredEdges = new RandomHashSet<>(rng);
        missingColoursOf = new HashMap<>();
        vertexSet = new HashSet<>();

        for (int i = 0; i < graph.size(); i++) {
            for (int j :
                    graph.get(i)) {
                Edge currentEdge = new Edge(i, j);
                edgeSet.add(currentEdge);

                uncolouredEdges.add(currentEdge);

                HashSet<Byte> missingColours = new HashSet<>(maxDegree);
                for (byte k = 1; k <= maxDegree + 1; k++) {
                    missingColours.add(k);
                }

                missingColoursOf.put(currentEdge, missingColours);
                colouring.put(currentEdge, (byte) 0);
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

    private Vector<Edge> MSVA(@NotNull HashMap<Edge, Byte> colouring, Edge edge, int x) {
        HashMap<Edge, Boolean> visitedEdges = new HashMap<>(edgeSet.size());
        HashMap<Integer, Boolean> visitedVertices = new HashMap<>(vertexSet.size());
        for (Edge e :
                edgeSet) {
            visitedEdges.put(e, false);
        }

        for (Integer i = 0; i < vertexSet.size(); i++) {
            visitedVertices.put(i, false);
        }

        FirstChainResult firstChainResult = firstChain(colouring, edge, x);
        Vector<Edge> firstFan = firstChainResult.fan;
        Vector<Edge> firstPath = firstChainResult.path;
        Vector<Vector<Edge>> chainAsConcat = new Vector<>();
        Vector<Edge> firstEdge = new Vector<>();
        firstEdge.add(edge);
        chainAsConcat.add(firstEdge);

        HashMap<Edge, Byte> localColouring;
        localColouring = deepCopyColouring(colouring); // shamelessly adapted from sprinter's code on Stack Overflow
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
                int lastVOfLastP = lastOfPath.y; // Denoted v in Bernshteyn's
                Vector<Edge> chainToShift = new Vector<>(currentFan);
                chainToShift.addAll(currentPath.subList(1, currentPath.size()));


                byte lastColour = localColouring.get(currentPath.lastElement()); // denoted beta in Bernsheyn's
                byte secondToLastColour = localColouring.get(currentPath.get(currentPath.size() - 2)); // denoted alpha in Bernshteyn's

                localColouring = chainShift(localColouring, new LinkedList<>(chainToShift), true);
                NextChainResult nextChainResult = nextChain(localColouring, lastOfPath, lastVOfLastP, secondToLastColour, lastColour);
                Vector<Edge> FTilde = nextChainResult.f;
                Vector<Edge> PTilde = nextChainResult.p;
                VisitedResult visitedResult = alreadyVisited(FTilde, PTilde);
                boolean intersection = visitedResult.visited;
                int intersectionPosition = visitedResult.j; // Denoted j in Bernshteyn's
                if (intersection) {
                    Vector<Edge> toRevert = new Vector<>(chainAsConcat.get(intersectionPosition));
                    for (int i = intersectionPosition + 1; i < 2 * k; i++) {
                        Vector<Edge> chainToAdd = chainAsConcat.get(i);
                        toRevert.addAll(chainToAdd.subList(0, chainToAdd.size()));
                    }
                    localColouring = chainShift(localColouring, new LinkedList<>(toRevert), false);
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

    private static void updateVisited(Vector<Edge> currentFan, HashMap<Integer, Boolean> visitedVertices, Vector<Edge> currentPath, HashMap<Edge, Boolean> visitedEdges) {
        for (Edge visitedEdge :
                currentFan) {
            visitedVertices.put(visitedEdge.x, true);
            visitedVertices.put(visitedEdge.y, true);
        }
        for (int i = 1; i < currentPath.size() - 1; i++) {
            visitedEdges.put(currentPath.get(i), true);
        }
    }

    private static void updateRemoved(int intersectionPosition, int k, Vector<Vector<Edge>> chainAsConcat, HashMap<Integer, Boolean> visitedVertices, HashMap<Edge, Boolean> visitedEdges) {
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

    private @NotNull VisitedResult alreadyVisited(Vector<Edge> fTilde, Vector<Edge> pTilde) {
        //TODO
        return null;
    }

    private record VisitedResult(boolean visited, int j) {
    }

    ;

    private @NotNull NextChainResult nextChain(HashMap<Edge, Byte> localColouring, Edge lastOfPath, int lastVOfLastP, byte secondToLastColour, byte lastColour) {
        //Todo
        return null;
    }

    private record NextChainResult(Vector<Edge> f, Vector<Edge> p) {
    }

    private @NotNull HashMap<Edge, Byte> chainShift(HashMap<Edge, Byte> colouring, LinkedList<Edge> chainToShift, boolean ascending) {
        HashMap<Edge, Byte> res = deepCopyColouring(colouring);
        Iterator<Edge> iterator;

        if (ascending)
            iterator = chainToShift.iterator();
        else
            iterator = chainToShift.descendingIterator();

        Edge e0 = iterator.next();
        Edge e1;
        while (iterator.hasNext()) {
            e1 = iterator.next();
            byte e1Colour = res.get(e1);
            res.put(e0, e1Colour);
            removeFromNeighboursOf(e0, e1Colour);
            // TODO update of the missing colours. For the current edge changes nothing,
            //  but for all the neighbours, now they might have one less colour
            res.put(e1, (byte) 0);
            int sharedNode = e0.sharedNode(e1);
            addToNeighboursOf(e1, e1Colour, sharedNode, colouring);
            // problem when making it become blank
            e0 = e1;
        }

        return res;
    }

    private void addToNeighboursOf(Edge edge, byte previousColour, int sharedNode, HashMap<Edge, Byte> colouring) {
        int nodeToInspect = edge.x == sharedNode ? edge.y : edge.x; // we know the shared node is still coloured by our previousColour so cannot be updated
        for (int neigh :
                graph.get(nodeToInspect)) {
            Edge edgeToUpdate = new Edge(nodeToInspect, neigh);
            int nodeToIgnore = edgeToUpdate.sharedNode(edge);
            int nodeToInspect2 = edgeToUpdate.x == nodeToIgnore ? edgeToUpdate.y : edgeToUpdate.x;
            boolean mustUpdate = true;
            for (int neigh2 :
                    graph.get(nodeToInspect2)) {
                mustUpdate = colouring.get(neigh2) != previousColour && mustUpdate;
            }
            if (mustUpdate) {
                missingColoursOf.get(edgeToUpdate).add(previousColour);
            }
        }
    }

    @NotNull
    private static HashMap<Edge, Byte> deepCopyColouring(HashMap<Edge, Byte> localColouring) {
        return (@NotNull HashMap<Edge, Byte>) localColouring.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private @NotNull FirstChainResult firstChain(HashMap<Edge, Byte> colouring, Edge edge, int x) {
        FirstFanResult firstFanResult = firstFan(colouring, edge, x);
        Vector<Edge> fan = firstFanResult.fan;
        byte beta = firstFanResult.color;
        int j = firstFanResult.j;
        FirstChainResult res;
        HashSet<Byte> coloursOfEdge = missingColoursOf.get(edge);
        if (coloursOfEdge.contains(beta)) {
            Vector<Edge> path = new Vector<>();
            path.add(fan.lastElement());
            res = new FirstChainResult(fan, path);
        } else {
            byte alpha = coloursOfEdge.iterator().next();
            // TODO is there an update here ?
            Vector<Edge> fPrime = new Vector<>(fan.subList(0, j));
            Vector<Edge> p = createPath(fan.lastElement(), chainShift(colouring, new LinkedList<>(fan), true), alpha, beta, 2 * pathMaxLength);
            Vector<Edge> pPrime = createPath(fPrime.lastElement(), chainShift(colouring, new LinkedList<>(fPrime), true), alpha, beta, 2 * pathMaxLength);
            if (p.size() > 2 * pathMaxLength || p.lastElement().y != x) {
                Vector<Edge> truncatedPath = new Vector<>(p.subList(0, 2 * pathMaxLength));
                res = new FirstChainResult(fan, truncatedPath);
            } else {
                Vector<Edge> truncatedPath = new Vector<>(pPrime.subList(0, 2 * pathMaxLength));
                res = new FirstChainResult(fPrime, truncatedPath);
            }
        }
        return res;
    }
    // TODO there are probably some places where we can "just" setSize on chains :)
    // most possibly because we know that we have more than the number of edges by pre-conditions

    private @NotNull Vector<Edge> createPath(Edge edge, HashMap<Edge, Byte> shift, byte alpha, byte beta, int limit) {
        //TODO p14
        return null;
    }

    private record FirstChainResult(Vector<Edge> fan, Vector<Edge> path) {
    }

    private @NotNull FirstFanResult firstFan(HashMap<Edge, Byte> colouring, Edge edge, int edgeX) {
        //TODO
        return null;
    }

    private record FirstFanResult(Vector<Edge> fan, byte color, int j) {
    }

    //todo change every chain to a linked list ?
    private HashMap<Edge, Byte> augmentWith(@NotNull HashMap<Edge, Byte> colouring, Vector<Edge> msvc) {
        colouring = chainShift(colouring, new LinkedList<>(msvc), true);
        Edge edgeOfInterest = msvc.lastElement();
        byte validColour;
        HashSet<Byte> coloursOfInterest = missingColoursOf.get(edgeOfInterest);
        validColour = coloursOfInterest.iterator().next();
        removeFromNeighboursOf(edgeOfInterest, validColour);
        colouring.put(edgeOfInterest, validColour);
        return colouring;
    }

    private void removeFromNeighboursOf(Edge colouredEdge, byte chosenColour) {
        int u = colouredEdge.x;
        int v = colouredEdge.y;
        for (int neigh :
                graph.get(u)) {
            missingColoursOf.get(new Edge(u, neigh)).remove(chosenColour);
        }
        for (int neigh :
                graph.get(v)) {
            missingColoursOf.get(new Edge(v, neigh)).remove(chosenColour);
        }
    }

    class Edge {
        private static int numberOfVertices;
        private final int x;
        private final int y;


        public Edge(int x, int y) {
            if (x < y) {
                this.x = x;
                this.y = y;
            } else {
                this.x = y;
                this.y = x;
            }
        }

        public static void setNumberOfVertices(int numberOfVertices) {
            Edge.numberOfVertices = numberOfVertices;
        }

        public int sharedNode(Edge other) {
            int res = -1;
            if (this.x == other.x || this.x == other.y) res = this.x;
            else if (this.y == other.x || this.y == other.y) res = this.x;
            return res;
        }

        @Override
        public int hashCode() {
            return numberOfVertices * x + y;
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


