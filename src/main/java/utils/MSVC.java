package utils;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MSVC {
    private HashMap<Integer, HashSet<Byte>> missingColoursOfV;
    private final Random rng;
    private HashSet<Edge> edgeSet; // should reduce slightly the memory usage, as every edge is now unique
    private HashSet<Integer> vertexSet;
    //2147483647

    /**
     * The maximum degree of a node in our graph.
     * Denoted Delta in Bernshteyn's paper
     */
    private byte maxDegree; // currently limited to 4 because of the addressing size of Java
    /**
     * The maximum path length we are ready to accept, which will be 400*maxDegree^13.
     * Denoted l in Bernshteyn's paper
     */
    private int pathMaxLength; //using int, the limit before overflowing is Delta=3 :-/
    // Cannot use BigInteger because cannot address afterwards. Cannot use long because cannot address arrays in long :-/

    // Might be interesting to switch to lower level language to make it work ? Only with enough memory,
    // which, if we used 2 concatenated shorts (to reduce usage) would amount to

    private Vector<Vector<Integer>> graph;

    public MSVC() {
        rng = new Random();
    }

    public MSVC(long seed) {
        rng = new Random(seed);
    }

    public HashMap<Integer[], Byte> edgeColouring(Vector<Vector<Integer>> graph, byte maxDegree) throws IllegalArgumentException {
        HashMap<Integer[], Byte> result;
//        if (maxDegree <= 3) { // TODO might be a bit finer than that, like the only possibility for 3
        //  being a problem is if I have more than 2^32-1 edges, which would not make sense.since integer
        Edge.setNumberOfVertices(graph.size());
        this.maxDegree = maxDegree;
        edgeSet = new HashSet<>(graph.size());
        this.graph = graph;
        HashMap<Edge, Byte> colouring = new HashMap<>(graph.size() * maxDegree / 2);
        RandomHashSet<Edge> uncolouredEdges = createFields(graph, colouring);

        Iterator<Edge> itU;
        itU = uncolouredEdges.iterator();
        int printInterval = Math.max(5 * graph.size() / 100, 200);
        double precPrint = -printInterval;
        do {
            Edge edge = itU.next();
            Vector<Edge> vizingChain = MSVA(colouring, edge, edge.x);
            colouring = augmentWith(colouring, vizingChain);
            uncolouredEdges.remove(edge);
            itU = uncolouredEdges.iterator();
            /*double qTreated = edgeSet.size() - uncolouredEdges.size();
            if (precPrint + printInterval <= qTreated) {
                precPrint = qTreated;
                System.out.format("Has treated %04d/%04d (%2.2f)\n",
                        edgeSet.size() - uncolouredEdges.size(),
                        edgeSet.size(),
                        100 * qTreated / edgeSet.size());

            }*/
        } while (itU.hasNext());

        result = colouringToArray(colouring);
//        } else throw new IllegalArgumentException("Delta cannot be higher than 3");
// TODO if addressing and vertex space are the same, since we need n + 1 vertices to make a path of size n,
//  we cannot get a path which would be higher than the addressing space.
//  Resolves just about all of my problems, makes it that Delta can be arbitrarily long,
//  "simply" the shortening of the paths can never happen if Delta > the limit.
//  ...
//  That's a weird consequence though
        return result;
    }

    public boolean testColouring(HashMap<Integer[], Byte> colouring, Vector<Vector<Integer>> graph) {
        HashMap<Edge, Byte> internalColouring = new HashMap<>();
        for (Integer[] key :
                colouring.keySet()) {
            Edge edge = new Edge(key[0], key[1]);
            internalColouring.put(edge, colouring.get(key));
        }
        int current = 0;
        boolean correct = true; //todo test when edgeSize = 8528
        int printInterval = Math.max(5 * graph.size() / 100, 200);
        double precPrint = -printInterval;
        for (Vector<Integer> adjacency :
                graph) {
            HashSet<Byte> currentUsedColours = new HashSet<>();
            for (int neigh :
                    adjacency) {
                Edge edge = new Edge(current, neigh);
                byte edgeColour = internalColouring.get(edge);
                correct &= !currentUsedColours.contains(edgeColour);
                currentUsedColours.add(edgeColour);
            }

            double propTreated = current;
            if (precPrint + printInterval <= propTreated) {
                precPrint = propTreated;
                System.out.format("Has checked %04d/%04d (%2.2f)\n",
                        current,
                        edgeSet.size(),
                        100 * propTreated / edgeSet.size());

            }
            current++;
        }
        return correct;
    }

    private HashMap<Integer[], Byte> colouringToArray(HashMap<Edge, Byte> colouring) {
        HashMap<Integer[], Byte> res = new HashMap<>();
        for (Edge key :
                colouring.keySet()) {
            res.put(key.toArray(), colouring.get(key));
        }
        return res;
    }

    public int getNumberOfEdges() {
        return edgeSet.size();
    }

    private RandomHashSet<Edge> createFields(Vector<Vector<Integer>> graph, HashMap<Edge, Byte> colouring) {
        RandomHashSet<Edge> uncolouredEdges = new RandomHashSet<>(rng);
        missingColoursOfV = new HashMap<>();
        vertexSet = new HashSet<>();

        for (int i = 0; i < graph.size(); i++) {
            for (int j :
                    graph.get(i)) {
                final Edge currentEdge = new Edge(i, j);
                edgeSet.add(currentEdge);

                uncolouredEdges.add(currentEdge);

                colouring.put(currentEdge, (byte) 0);
            }
            HashSet<Byte> missingColours = new HashSet<>(maxDegree);
            for (byte k = 1; k <= maxDegree + 1; k++) {
                missingColours.add(k);
            }

            missingColoursOfV.put(i, missingColours);
            vertexSet.add(i);
        }


        // TODO check where we shorten paths because if Delta too high, then its is nonsensical
        //  And if we have to high a value, we "just" replace it with the max value for int.
        //  THIS PART IS DANGEROUS, WE ARE REDUCING A LARGER NUMBER >-<

        if (maxDegree <= 3) {
            pathMaxLength = maxDegree; // square and multiply with 13, our exponent so      1 : S&M
            pathMaxLength *= pathMaxLength * maxDegree; //(13 is represented top to bottom  1 : S&M
            pathMaxLength *= pathMaxLength; // from highest bit to lowest bit)              0 : S
            pathMaxLength *= pathMaxLength * maxDegree;//                                   1 : S&M
            pathMaxLength *= 400;
        } else {
            pathMaxLength = Integer.MAX_VALUE / 2;
        }
        return uncolouredEdges;
    }

    private Vector<Edge> MSVA(@NotNull HashMap<Edge, Byte> colouring, Edge edge, int x) {
        FillVisitedResult fillVisitedResult = fillVisited();
        HashMap<Integer, Boolean> visitedVertices = fillVisitedResult.visitedVertices();
        HashMap<Edge, Boolean> visitedEdges = fillVisitedResult.visitedEdges();

        FirstChainResult firstChainResult = firstChain(colouring, edge, x);
        Vector<Edge> firstFan = firstChainResult.fan;
        Vector<Edge> firstPath = firstChainResult.path;
        Vector<Vector<Edge>> chainAsConcat = new Vector<>();
        Vector<Edge> firstEdge = new Vector<>();
        firstEdge.add(edge);
        chainAsConcat.add(firstEdge);

        HashMap<Edge, Byte> localColouring;
        localColouring = deepCopyColouring(colouring); // shamelessly adapted from sprinter's code on Stack Overflow

        boolean shortEnough = false;

        int comparator = pathMaxLength * 2;
        chainAsConcat = mainMSVALoop(shortEnough, firstPath, comparator, chainAsConcat, firstFan, visitedVertices, visitedEdges, localColouring);
        Vector<Edge> chain = new Vector<>();
        int startPoint = 0;
        for (Vector<Edge> innerChain :
                chainAsConcat) {
            chain.addAll(innerChain.subList(startPoint, innerChain.size()));
            startPoint = 1;
        }
        return chain;
    }

    private Vector<Vector<Edge>> mainMSVALoop(boolean shortEnough, Vector<Edge> firstPath, int comparator, Vector<Vector<Edge>> chainAsConcat, Vector<Edge> firstFan, HashMap<Integer, Boolean> visitedVertices, HashMap<Edge, Boolean> visitedEdges, HashMap<Edge, Byte> localColouring) {

        int k = 0;
        while (!shortEnough) {
            if (firstPath.size() <= comparator) {
                shortEnough = successfulChain(chainAsConcat, firstFan, firstPath);
            } else {
                MSVAPreparationResult preparationResult = MSVAPreparation(firstFan, firstPath, visitedVertices, visitedEdges);
                byte lastColour = localColouring.get(preparationResult.currentPath().lastElement()); // denoted beta in Bernshteyn's
                byte secondToLastColour = localColouring.get(preparationResult.currentPath().get(preparationResult.currentPath().size() - 2)); // denoted alpha in Bernshteyn's

                localColouring = shimano(localColouring, new LinkedList<>(preparationResult.chainToShift()), true, missingColoursOfV);
                NextChainResult nextChainResult = nextChain(localColouring, preparationResult.lastOfPath(), preparationResult.lastVOfLastP(), secondToLastColour, lastColour);
                Vector<Edge> FTilde = nextChainResult.f;
                Vector<Edge> PTilde = nextChainResult.p;
                VisitedResult visitedResult = alreadyVisited(FTilde, PTilde, chainAsConcat, visitedVertices, visitedEdges);
                boolean intersection = visitedResult.visited;
                int intersectionPosition = visitedResult.j; // Denoted j in Bernshteyn's
                if (intersection) {
                    Vector<Edge> toRevert = new Vector<>(chainAsConcat.get(intersectionPosition));
                    buildRevertedChain(chainAsConcat, k, intersectionPosition, toRevert);
                    localColouring = shimano(localColouring, new LinkedList<>(toRevert), false, missingColoursOfV);
                    updateRemoved(intersectionPosition, k, chainAsConcat, visitedVertices, visitedEdges);
                    firstFan = chainAsConcat.get(intersectionPosition);
                    Vector<Edge> chainToElongate = chainAsConcat.get(intersectionPosition + 1);
                    elongate(chainToElongate, localColouring);
                    firstPath = chainToElongate;
                    chainAsConcat = new Vector<>(chainAsConcat.subList(0, intersectionPosition));
                    k = intersectionPosition / 2; //intersectionPosition should be even :-/
                } else {
                    chainAsConcat.add(preparationResult.currentFan());
                    chainAsConcat.add(new Vector<>(preparationResult.currentPath().subList(1, preparationResult.currentPath().size())));
                    firstFan = FTilde;
                    firstPath = PTilde;
                    k++;
                }
            }
        }
        return chainAsConcat;
    }

    private void elongate(Vector<Edge> chainToElongate, HashMap<Edge, Byte> localColouring) {
        Edge lastE = chainToElongate.lastElement();
        byte lastColour = localColouring.get(lastE);
        byte secondToLastColour = localColouring.get(new Vector<>(chainToElongate.subList(0, chainToElongate.size() - 1)).lastElement());

        elongatePathUntil(localColouring, secondToLastColour, lastColour, lastE.y, lastE.x, chainToElongate, -1);

    }

    private static void buildRevertedChain(Vector<Vector<Edge>> chainAsConcat, int k, int intersectionPosition, Vector<Edge> toRevert) {
        for (int i = intersectionPosition + 1; i < 2 * k; i++) {
            Vector<Edge> chainToAdd = chainAsConcat.get(i);
            toRevert.addAll(chainToAdd.subList(0, chainToAdd.size()));
        }
    }

    @NotNull
    private FillVisitedResult fillVisited() {
        HashMap<Edge, Boolean> visitedEdges = new HashMap<>(edgeSet.size());
        HashMap<Integer, Boolean> visitedVertices = new HashMap<>(vertexSet.size());
        for (Edge e :
                edgeSet) {
            visitedEdges.put(e, false);
        }

        for (int i = 0; i < vertexSet.size(); i++) {
            visitedVertices.put(i, false);
        }
        return new FillVisitedResult(visitedEdges, visitedVertices);
    }

    private record FillVisitedResult(HashMap<Edge, Boolean> visitedEdges, HashMap<Integer, Boolean> visitedVertices) {

    }

    private MSVAPreparationResult MSVAPreparation(Vector<Edge> firstFan, Vector<Edge> firstPath, HashMap<Integer, Boolean> visitedVertices, HashMap<Edge, Boolean> visitedEdges) {
        int shorteningDistance = pathMaxLength + rng.nextInt(pathMaxLength); // since excludes the bound, -1 isn't needed
        Vector<Edge> currentPath = (Vector<Edge>) firstPath.subList(0, shorteningDistance + 1);
        updateVisited(firstFan, visitedVertices, currentPath, visitedEdges);
        Edge lastOfPath = currentPath.lastElement(); // Denoted uv in Bernshteyn's
        int lastVOfLastP = lastOfPath.y; // Denoted v in Bernshteyn's
        Vector<Edge> chainToShift = new Vector<>(firstFan);
        chainToShift.addAll(currentPath.subList(1, currentPath.size()));
        return new MSVAPreparationResult(firstFan, currentPath, lastOfPath, lastVOfLastP, chainToShift);
    }

    private record MSVAPreparationResult(Vector<Edge> currentFan, Vector<Edge> currentPath, Edge lastOfPath,
                                         int lastVOfLastP, Vector<Edge> chainToShift) {
    }

    private static boolean successfulChain(Vector<Vector<Edge>> chainAsConcat, Vector<Edge> firstFan, Vector<Edge> firstPath) {
        chainAsConcat.add(firstFan);
        chainAsConcat.add(firstPath);
        return true;
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

    private @NotNull VisitedResult alreadyVisited(Vector<Edge> fTilde, Vector<Edge> pTilde, Vector<Vector<Edge>> chainAsConcat, HashMap<Integer, Boolean> visitedVertices, HashMap<Edge, Boolean> visitedEdges) {
        int j = 0;
        boolean alreadyVisited = false;
        Vector<Edge> concat = new Vector<>(fTilde);
        concat.addAll(pTilde);
        for (Edge edge :
                concat) {
            alreadyVisited |= visitedEdges.get(edge) || visitedVertices.get(edge.x) || visitedVertices.get(edge.y);
        }
        if (alreadyVisited) {
            for (int i = 0; i < chainAsConcat.size(); i += 2) {
                Vector<Edge> currentFan = chainAsConcat.get(i);
                Vector<Edge> currentPath = chainAsConcat.get(i + 1);
                Vector<Edge> chainConcat = new Vector<>(currentFan);
                chainConcat.addAll(currentPath);
                for (Edge edge :
                        concat) {
                    if (chainConcat.contains(edge))
                        break; // n+1/2 loop, not magical but I don't wanna do it otherwise :P
                }
                j++;
            }
        }
        return new VisitedResult(alreadyVisited, j);
    }

    private record VisitedResult(boolean visited, int j) {
    }

    private @NotNull NextChainResult nextChain(HashMap<Edge, Byte> localColouring, Edge e, int x, byte alpha, byte beta) {
        NextChainResult res;
        NextFanResult nextFanResult = NextFan(localColouring, e, x, beta);
        Vector<Edge> p;
        byte delta = nextFanResult.delta;
        Vector<Edge> f = nextFanResult.fan;
        if (missingColoursOfV.get(x).contains(delta)) {
            p = new Vector<>();
            p.add(f.lastElement());
            res = new NextChainResult(f, p);
        } else {
            if (delta == beta) {
                p = createPath(f.lastElement(),
                        shimano(localColouring, new LinkedList<>(f), true, missingColoursOfV),
                        alpha, beta, 2 * pathMaxLength);
                res = new NextChainResult(f, p);
            } else {
                byte gamma;
                Iterator<Byte> iterator = missingColoursOfV.get(x).iterator();
                do {
                    gamma = iterator.next();
                } while (gamma == alpha);
                Vector<Edge> fPrime = new Vector<>(f.subList(0, nextFanResult.j));
                Vector<Edge> pPrime;
                p = createPath(f.lastElement(),
                        shimano(localColouring, new LinkedList<>(f), true, missingColoursOfV),
                        gamma, delta, 2 * pathMaxLength);
                pPrime = createPath(fPrime.lastElement(),
                        shimano(localColouring, new LinkedList<>(f), true, missingColoursOfV),// todo is this correct ?
                        gamma, delta, 2 * pathMaxLength);
                if (p.size() > 2 * pathMaxLength || p.lastElement().y != x) {
                    res = new NextChainResult(f, p);
                } else res = new NextChainResult(fPrime, pPrime);
            }
        }
        return res;
    }

    private record NextChainResult(Vector<Edge> f, Vector<Edge> p) {
    }

    private NextFanResult NextFan(HashMap<Edge, Byte> localColouring, Edge e, int x, byte beta) {
        NextFanPreparationResult nextFanPreparationResult = nextFanPreparation(localColouring, e, x, beta);
        int k = 0;
        int z = nextFanPreparationResult.y();
        HashMap<Integer, Integer> indexOf = nextFanPreparationResult.indexOf();
        Vector<Edge> f = nextFanPreparationResult.f();
        indexOf.put(z, k);

        NextFanResult res = null;
        boolean oneMore = true;
        while (k < graph.get(x).size() && oneMore) {
            byte eta = nextFanPreparationResult.delta().get(z);
            if (missingColoursOfV.get(x).contains(eta) || eta == beta) {
                res = new NextFanResult(f, eta, k + 1);
                oneMore = false;
            } else {
                z = nextFanPreparationResult.neighbouringColour().get(eta);
                if (indexOf.containsKey(z)) {
                    res = new NextFanResult(f, eta, indexOf.get(z));
                    oneMore = false;
                } else {
                    k++;
                    indexOf.put(z, k);
                    f.add(new Edge(x, z));
                }
            }
        }
        return res; //Currently can return null. Might wanna check into this :-/
    }

    private record NextFanResult(Vector<Edge> fan, byte delta, int j) {

    }

    @NotNull
    private NextFanPreparationResult nextFanPreparation(HashMap<Edge, Byte> localColouring, Edge e, int x, byte beta) {
        HashMap<Byte, Integer> neighbouringColour = new HashMap<>(maxDegree + 1);
        int y = e.getOther(x);
        Vector<Integer> neighXSansY = new Vector<>(graph.get(x));
        HashMap<Integer, Integer> indexOf = new HashMap<>(neighXSansY.size());
        neighXSansY.remove(y);

        HashMap<Integer, Byte> delta = new HashMap<>(neighXSansY.size());
        Iterator<Byte> iterator = missingColoursOfV.get(y).iterator();
        Byte colour;
        do {
            colour = iterator.next();
            delta.put(y, colour);
        } while (iterator.hasNext() || colour == beta);
        for (int z :
                neighXSansY) {
            delta.put(z, missingColoursOfV.get(z).iterator().next());
            neighbouringColour.put(localColouring.get(new Edge(x, z)), z);
        }
        Vector<Edge> f = new Vector<>();
        f.add(e);
        return new NextFanPreparationResult(neighbouringColour, y, indexOf, delta, f);
    }

    private record NextFanPreparationResult(HashMap<Byte, Integer> neighbouringColour, int y,
                                            HashMap<Integer, Integer> indexOf, HashMap<Integer, Byte> delta,
                                            Vector<Edge> f) {

    }

    private static @NotNull HashMap<Edge, Byte> shimano(HashMap<Edge, Byte> colouring, LinkedList<Edge> chainToShift, boolean ascending, HashMap<Integer, HashSet<Byte>> missingColoursOfV) {
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
            removeFromMissingOf(e0, e1Colour, missingColoursOfV);

            res.put(e1, (byte) 0);
            int sharedNode = e0.sharedNode(e1);
            addToMissingOf(e1, e1Colour, sharedNode, missingColoursOfV);
            // problem when making it become blank
            e0 = e1;
        }

        return res;
    }

    private static void addToMissingOf(Edge edge, byte previousColour, int sharedNode, HashMap<Integer, HashSet<Byte>> missingColoursOfV) {
        int nodeToUpdate = edge.x == sharedNode ? edge.y : edge.x;
        if (previousColour != 0)
            missingColoursOfV.get(nodeToUpdate).add(previousColour);
    }

    private static void removeFromMissingOf(Edge colouredEdge, byte chosenColour, HashMap<Integer, HashSet<Byte>> missingColoursOfV) {
        missingColoursOfV.get(colouredEdge.x).remove(chosenColour);
        missingColoursOfV.get(colouredEdge.y).remove(chosenColour);
    }

    @NotNull
    private static HashMap<Edge, Byte> deepCopyColouring(HashMap<Edge, Byte> localColouring) {
        return (@NotNull HashMap<Edge, Byte>) localColouring.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private @NotNull FirstChainResult firstChain(HashMap<Edge, Byte> colouring, Edge edge, int x) {
        FirstFanResult firstFanResult = firstFan(colouring, edge, x);
        Vector<Edge> fan = firstFanResult.fan;
        byte beta = firstFanResult.colour;
        int j = firstFanResult.j;
        FirstChainResult res;
        HashSet<Byte> coloursOfVertex = missingColoursOfV.get(x);
        if (coloursOfVertex.contains(beta)) {
            Vector<Edge> path = new Vector<>();
            path.add(fan.lastElement());
            res = new FirstChainResult(fan, path);
        } else {
            byte alpha = coloursOfVertex.iterator().next();
            Vector<Edge> fPrime = new Vector<>(fan.subList(0, j));
            Vector<Edge> p = createPath(fan.lastElement(), shimano(colouring, new LinkedList<>(fan), true, missingColoursOfV), alpha, beta, 2 * pathMaxLength);
            Vector<Edge> pPrime = createPath(fPrime.lastElement(), shimano(colouring, new LinkedList<>(fPrime), true, missingColoursOfV), alpha, beta, 2 * pathMaxLength);
            if (p.size() > 2 * pathMaxLength || p.lastElement().y != x) {
                res = new FirstChainResult(fan, p);
            } else {
                res = new FirstChainResult(fPrime, pPrime);
            }
        }
        return res;
    }
    // TODO there are probably some places where we can "just" setSize on chains :)

    // most possibly because we know that we have more than the number of edges by pre-conditions

    private record FirstChainResult(Vector<Edge> fan, Vector<Edge> path) {

    }

    private @NotNull Vector<Edge> createPath(Edge edge, HashMap<Edge, Byte> colouring, byte alpha, byte beta, int limit) {
        Vector<Edge> path = new Vector<>();
        path.add(edge);

        int u = edge.x;
        int v = edge.y;
        int x, y;
        if (missingColoursOfV.get(u).contains(alpha)) {
            x = u;
            y = v;
        } else {
            x = v;
            y = u;
        }
//        path.add(new Edge(x, y));

        elongatePathUntil(colouring, alpha, beta, y, x, path, limit);
        return path;
    }

    private void elongatePathUntil(HashMap<Edge, Byte> colouring, byte alpha, byte beta, int y, int x, Vector<Edge> path, int limit) {
        boolean takeAlpha = true;
        HashSet<Integer> visitedVertices = new HashSet<>();
        byte currentColour = alpha;
        int next = y;
        visitedVertices.add(x);
        while (!visitedVertices.contains(next) && (limit == -1 || path.size() < limit)) {
            visitedVertices.add(next);
            for (int neigh :
                    graph.get(next)) {
                Byte linkColour = colouring.get(new Edge(neigh, next));
                if (linkColour == currentColour) {
                    takeAlpha = !takeAlpha;
                    currentColour = takeAlpha ? alpha : beta;
                    path.add(new Edge(neigh, next));
                    next = neigh;
                    break;
                }
            }
        }
    }

    private @NotNull FirstFanResult firstFan(HashMap<Edge, Byte> colouring, Edge e, int x) {
        HashMap<Byte, Integer> nbr = new HashMap<>(maxDegree + 1);
        Vector<Integer> neighOfX = graph.get(x);
        HashMap<Integer, Byte> beta = new HashMap<>(neighOfX.size());
        for (int neigh :
                neighOfX) {
            byte proposedColour = missingColoursOfV.get(neigh).iterator().next();
            beta.put(neigh, proposedColour);
            byte colourOfEdge = colouring.get(new Edge(x, neigh));
            nbr.put(colourOfEdge, neigh);
        } // // edgeSet.stream().filter( k -> k.contains(neigh)).collect()
        HashMap<Integer, Integer> indexOf = new HashMap<>(neighOfX.size());
        Vector<Edge> f = new Vector<>();
        f.add(e);
        int k = 0;
        int z = e.x == x ? e.y : e.x;
        indexOf.put(z, k);
        FirstFanResult res = null;
        boolean oneMore = true;
        while (k < neighOfX.size() && oneMore) {
            byte eta = beta.get(z);
            if (missingColoursOfV.get(x).contains(eta)) {
                res = new FirstFanResult(f, eta, k + 1);
                oneMore = false;
            } else {
                z = nbr.get(eta);
                if (indexOf.containsKey(z)) {
                    res = new FirstFanResult(f, eta, indexOf.get(z));
                    oneMore = false;
                } else {
                    k++;
                    indexOf.put(z, k);
                    f.add(new Edge(x, z));
                }
            }
        }
        return res;
    }

    private record FirstFanResult(Vector<Edge> fan, byte colour, int j) {

    }

    private HashMap<Edge, Byte> augmentWith(@NotNull HashMap<Edge, Byte> colouring, Vector<Edge> msvc) {
        colouring = shimano(colouring, new LinkedList<>(msvc), true, missingColoursOfV);
        Edge edgeOfInterest = msvc.lastElement();
        byte validColour;
        HashSet<Byte> coloursOfx = missingColoursOfV.get(edgeOfInterest.x);
        HashSet<Byte> coloursOfy = missingColoursOfV.get(edgeOfInterest.y);
        Iterator<Byte> coloursOfXIt = coloursOfx.iterator();
        do {
            validColour = coloursOfXIt.next();
        } while (!coloursOfy.contains(validColour));
        removeFromMissingOf(edgeOfInterest, validColour, missingColoursOfV);
        colouring.put(edgeOfInterest, validColour);
        return colouring;
    }

    static class Edge {
        private static int numberOfVertices;
        private final int x, y, max, min;

        public Edge(int x, int y) {
            this.x = x;
            this.y = y;
            max = Math.max(x, y);
            min = Math.min(y, x);
        }

        public boolean contains(int v) {
            return x == v || y == v;
        }

        public int getOther(int v) {
            return v == x ? y : x;
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

        public Integer[] toArray() {
            Integer[] res = new Integer[2];
            res[0] = min;
            res[1] = max;
            return res;
        }

        @Override
        public int hashCode() {
            return numberOfVertices * max + min;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return this.min == edge.min && this.max == edge.max;
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "x=" + min +
                    ", y=" + max +
                    '}';
        }
    }

}


