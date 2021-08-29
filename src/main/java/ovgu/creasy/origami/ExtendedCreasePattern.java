package ovgu.creasy.origami;

import ovgu.creasy.geom.Line;
import ovgu.creasy.geom.Point;
import ovgu.creasy.geom.Vertex;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Extended Crease Pattern, adds information about Reflection Creases
 * to a crease Pattern and is used for the step generation
 * Explained in chapter 4.2 of the paper (pages 28 - 36)
 */
public class ExtendedCreasePattern {
    private Set<Vertex> xV;
    private Set<ExtendedCrease> xC;
    private Map<Vertex, List<ExtendedCrease>> xL;

    /**
     * @param xV is the set of extended vertices
     * @param xC is the set of directed edges of the extended graph. Each xC ist called extended creases
     * @param xL is a set of ordered, circular lists of edges parting from each vertex xV
     */
    public ExtendedCreasePattern(Set<Vertex> xV, Set<ExtendedCrease> xC, Map<Vertex, List<ExtendedCrease>> xL) {
        this.xV = xV;
        this.xC = xC;
        this.xL = xL;
    }

    public ExtendedCreasePattern() {
    }

    public CreasePattern toCreasePattern() {
        Set<Line> addedLines = new HashSet<>();
        CreasePattern cp = new CreasePattern();
        for (ExtendedCrease extendedCrease : xC) {
            if (extendedCrease.getStartVertex().getType() == Vertex.Type.VIRTUAL
                || extendedCrease.getEndVertex().getType() == Vertex.Type.VIRTUAL) {
                continue;
            }
            Line line = new Line(
                extendedCrease.getStartVertex().getPoint(),
                extendedCrease.getEndVertex().getPoint());
            if (addedLines.contains(new Line(line.getEnd(), line.getStart()))) { // extended Creases can go in both directions,
                continue;                                                        // but we only need one
            }
            addedLines.add(line);
            cp.addCrease(new Crease(
                line,
                extendedCrease.getType()));
        }
        return cp;
    }

    public Set<Vertex> getVertices() {
        return Collections.unmodifiableSet(xV);
    }

    public Set<ExtendedCrease> getCreases() {
        return Collections.unmodifiableSet(xC);
    }

    public Map<Vertex, List<ExtendedCrease>> getAdjacencyLists() { return Collections.unmodifiableMap(xL); }

    @Override
    public String toString() {
        return "ExtendedCreasePattern{" +
            "xC=" + xC +
            '}';
    }

    public Collection<DiagramStep> possibleSteps() {
        List<DiagramStep> steps = new ArrayList<>();
        List<List<ExtendedCrease>> removableCreases = new ArrayList<>();
        Map<Vertex, List<ExtendedCrease>> possiblyRemovableCreases = new HashMap<>();
        for (Vertex vertex : xV) {
            if (vertex.getType() == Vertex.Type.BORDER) {
                List<ExtendedCrease> outgoing = this.xL.get(vertex);
                for (ExtendedCrease outgoingCrease : outgoing) {
                    if (outgoingCrease.getType() == Crease.Type.EDGE) {
                        continue;
                    }
                    if (outgoingCrease.getEndVertex().getType() == Vertex.Type.BORDER) {
                        removableCreases.add(Collections.singletonList(outgoingCrease));
                    } else if (outgoingCrease.getEndVertex().getType() == Vertex.Type.VIRTUAL) {
                        List<ExtendedCrease> creases = new ArrayList<>();
                        creases.add(outgoingCrease);
                        ExtendedCrease currentCrease = outgoingCrease;
                        while (xL.containsKey(currentCrease.getEndVertex())) {
                            var next = xL.get(currentCrease.getEndVertex());
                            currentCrease = next.get(next.size()-1);
                        }
                        Vertex middle = currentCrease.getEndVertex();
                        if (possiblyRemovableCreases.containsKey(middle)) {
                            possiblyRemovableCreases.get(middle).addAll(creases);
                            removableCreases.add(possiblyRemovableCreases.get(middle));
                        } else {
                            possiblyRemovableCreases.put(middle, creases);
                        }
                    }
                }
            }
        }
        for (List<ExtendedCrease> removableCreaseList : removableCreases) {
            ExtendedCreasePattern next = new ExtendedCreasePattern(new HashSet<>(xV), new HashSet<>(xC), xL);
            removableCreaseList.forEach(next.xC::remove);
            removableCreaseList.stream().map(ExtendedCrease::getOpposite).toList().forEach(next.xC::remove);
            next = new ExtendedCreasePatternFactory().createExtendedCreasePattern(next.toCreasePattern());
            steps.add(new DiagramStep(this, next));
        }
        return steps;
    }
}
