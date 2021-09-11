package ovgu.creasy.origami;

import ovgu.creasy.geom.Point;

import java.util.*;

/**
 * Creates Reflection graphs, Explained in chapter 3.1 of the paper (pages 18 - 20)
 */
public class ReflectionGraphFactory {
    private static final double EPS = 0.0000001;
    private final Map<Crease, Collection<Crease>> reflectionCreases;
    private final CreasePattern cp;

    public ReflectionGraphFactory(CreasePattern cp) {
        this.cp = cp;
        this.reflectionCreases = new HashMap<>();
        for (Point point : cp.getPoints()) {
            findReflectionCreases(cp.getAdjacentCreases(point), point);
        }
    }

    /**
     * Reflection creases are explained in chapter 3.1.1
     * <p>
     * Finds all reflection crease pairs in adjacentCreases around commonPoint and
     * puts them into reflectionCreases
     * <p>
     * adjacentCreases is assumed to be sorted by angle
     */
    private void findReflectionCreases(List<Crease> adjacentCreases, Point commonPoint) {
        for (int i = 0; i < adjacentCreases.size(); i++) {
            if (adjacentCreases.get(i).getType() == Crease.Type.EDGE) {
                continue;
            }
            double alternatingAngle = 0;
            int j = (i + 1) % adjacentCreases.size();
            // iterate over all creases but adjacentCreases[i], starting at i+1 and wrapping
            // around to the beginning when the end of the array is reached
            while (j != i) {
                double angle = getAngle(
                        getPrevious(j, adjacentCreases),
                        adjacentCreases.get(j),
                        commonPoint);
                if (j % 2 == 0) {
                    alternatingAngle += angle;
                } else {
                    alternatingAngle -= angle;
                }
                // see 3.1.1 for definition of reflection creases
                if (Math.abs(alternatingAngle) <= EPS
                        && adjacentCreases.get(i).getType() == adjacentCreases.get(j).getType().opposite()) {
                    addReflectionCrease(adjacentCreases.get(i), adjacentCreases.get(j));
                }
                j++;
                j %= adjacentCreases.size();
            }
        }
    }

    /**
     *
     * @return A collection of all connected subgraphs of the Reflection graph
     */
    public Collection<ReflectionGraph> getAllReflectionGraphs() {
        Collection<ReflectionGraph> reflectionGraphs = new ArrayList<>();
        Set<Crease> done = new HashSet<>();

        // Iterate over all creases and fina all creases connected through reflection Creases
        for (Crease crease : cp.getCreases()) {
            // Edge creases are skipped because they do not represent an actual fold
            if (crease.getType()== Crease.Type.EDGE || done.contains(crease)) {
                continue;
            }
            ReflectionGraph graph = new ReflectionGraph(cp);
            graph.addCrease(crease);
            Set<Crease> newCreases = new HashSet<>();
            do {
                newCreases.clear();
                for (Crease connectedCrease : graph.getCreases()) {
                    if (done.contains(connectedCrease)) {
                        continue;
                    }
                    done.add(connectedCrease);
                    newCreases.addAll(getReflectionCreases(connectedCrease));
                }
                if (newCreases.isEmpty()) {
                    break;
                }
                graph.addAllCreases(newCreases);
            } while (!newCreases.isEmpty());
            reflectionGraphs.add(graph);
        }
        return reflectionGraphs;
    }

    /**
     * @return the (index-1)-th element of creases, wrapping around if index == 0
     */
    private Crease getPrevious(int index, List<Crease> creases) {
        return creases.get((index + creases.size() - 1) % creases.size());
    }

    private double getAngle(Crease crease1, Crease crease2, Point commonPoint) {
        Point p2 = crease1.getLine().getEnd().equals(commonPoint) ? crease1.getLine().getStart() : crease1.getLine().getEnd();
        Point p3 = crease2.getLine().getEnd().equals(commonPoint) ? crease2.getLine().getStart() : crease2.getLine().getEnd();
        double x1 = commonPoint.getX() - p2.getX();
        double y1 = commonPoint.getY() - p2.getY();
        double x2 = commonPoint.getX() - p3.getX();
        double y2 = commonPoint.getY() - p3.getY();
        return Math.acos(
                (x1 * x2 + y1 * y2) /
                        (Math.sqrt(x1 * x1 + y1 * y1) * Math.sqrt(x2 * x2 + y2 * y2))
        );
    }

    private void addReflectionCrease(Crease crease1, Crease crease2) {
        if (!reflectionCreases.containsKey(crease1)) {
            reflectionCreases.put(crease1, new HashSet<>());
        }
        reflectionCreases.get(crease1).add(crease2);
        if (!reflectionCreases.containsKey(crease2)) {
            reflectionCreases.put(crease2, new HashSet<>());
        }
        reflectionCreases.get(crease2).add(crease1);
    }

    private Collection<Crease> getReflectionCreases(Crease crease) {
        if (reflectionCreases.containsKey(crease)) {
            return reflectionCreases.get(crease);
        }
        return new HashSet<>();
    }
}