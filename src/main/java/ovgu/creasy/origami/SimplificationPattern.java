package ovgu.creasy.origami;

import ovgu.creasy.geom.Vertex;

import java.util.*;
import java.util.stream.Collectors;

public class SimplificationPattern {
    private final List<Edge> pattern = new ArrayList<>();
    private final List<Edge> simplified = new ArrayList<>();
    private final Map<Integer, List<Edge>> patternOutgoingEdges = new HashMap<>();
    private final Map<Integer, List<Edge>> simplifiedOutgoingEdges = new HashMap<>();
    private final List<VertexType> vertexTypes;

    enum VertexType {
        INTERNAL, BORDER
    }
    class Edge {
        int start;
        int end;
        Crease.Type type;

        public Edge(int start, int end, Crease.Type type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return start == edge.start && end == edge.end && type == edge.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end, type);
        }
    }
    class Match {
        private Map<Integer, Vertex> vertices;
        private Map<Edge, ExtendedCrease> edges;

        public Match() {
            this.vertices = new HashMap<>();
            this.edges = new HashMap<>();
        }

        public Match(int vertexId, Vertex vertex) {
            this();
            addVertex(vertexId, vertex);
        }

        public Match(Match other) {
            this();
            this.vertices.putAll(other.vertices);
            this.edges.putAll(other.edges);
        }

        public Match merge(Match other) {
            Match newMatch = new Match(this);
            newMatch.edges.putAll(other.edges);
            newMatch.vertices.putAll(other.vertices);
            return newMatch;
        }

        static List<Match> mergeAll(List<Match> part1, List<Match> part2) {
            List<Match> merged = new ArrayList<>();
            for (Match match : part1) {
                for (Match match1 : part2) {
                    merged.add(match.merge(match1));
                }
            }
            return merged;
        }

        private void addVertex(int vertexId, Vertex vertex) {
            this.vertices.put(vertexId, vertex);
        }

        private void addEdge(Edge e, ExtendedCrease crease) {
            edges.put(e, crease);
        }

        private void removeEdge(Edge e, ExtendedCrease crease) {
            edges.remove(e, crease);
        }
    }

    public SimplificationPattern(VertexType... vertexTypes) {
        this.vertexTypes = Arrays.asList(vertexTypes);
    }

    public void addPatternEdge(int from, int to, Crease.Type type) {
        addEdge(pattern, patternOutgoingEdges, from, to, type);
    }

    public void addSimplifiedEdge(int from, int to, Crease.Type type) {
        addEdge(simplified, simplifiedOutgoingEdges, from, to, type);
    }

    private void addEdge(List<Edge> edgeList, Map<Integer, List<Edge>> outgoingEdges, int from, int to, Crease.Type type) {
        assert type == Crease.Type.VALLEY || type == Crease.Type.MOUNTAIN;
        Edge e = new Edge(from, to, type);
        Edge e2 = new Edge(to, from, type);
        edgeList.add(e);
        edgeList.add(e2);
        insertOutgoingEdge(outgoingEdges, from, e);
        insertOutgoingEdge(outgoingEdges, to, e2);
    }

    private void insertOutgoingEdge(Map<Integer, List<Edge>> outgoingEdges,int i, Edge e) {
        if (!outgoingEdges.containsKey(i)) {
            outgoingEdges.put(i, new ArrayList<>());
        }
        outgoingEdges.get(i).add(e);
    }

    public CreasePattern simplify(ExtendedCreasePattern ecp, Match match) {
        CreasePattern simplified = new CreasePattern();
        Set<Crease> omittedLines = new HashSet<>();
        int i = 0;
        for (Edge edge : pattern) {
            if (match.edges.containsKey(edge)) {
                omittedLines.addAll(match.edges.get(edge).getExtendedReflectionPath().getCreases());
                i++;
            }
        }
        System.out.println(i);
        for (Crease crease : ecp.toCreasePattern().getCreases()  ) {
            if (!omittedLines.contains(crease)){
                simplified.addCrease(crease);
            }
        }
        return simplified;
    }

    public List<Match> matches(ExtendedCreasePattern ecp) {
        List<Match> matches = new ArrayList<>();
        for (Vertex vertex : ecp.getVertices()) {
            Set<Edge> validating = new HashSet<>();
            Set<ExtendedCrease> validatingCreases = new HashSet<>();
            matches.addAll(findMatches(vertex, 0, validating, validatingCreases, ecp, false));
            validatingCreases.clear();
            validating.clear();
            matches.addAll(findMatches(vertex, 0, validating, validatingCreases, ecp, true));
        }
        return matches.stream().filter(match -> validateMatch(match, ecp)).collect(Collectors.toList());
    }

    private boolean validateMatch(Match match, ExtendedCreasePattern ecp) {
        Set<Vertex> vertices = new HashSet<>();
        for (Map.Entry<Integer, Vertex> vertexEntry : match.vertices.entrySet()) {
            Vertex v = vertexEntry.getValue();
            if (v.getType() == Vertex.Type.VIRTUAL) {
                List<ExtendedCrease> outgoingCreases = ecp.getAdjacencyLists().get(v);
                Vertex v1 = outgoingCreases.get(0).getEndVertex();
                Vertex v2 = outgoingCreases.get(1).getEndVertex();
                vertices.add(v1);
                vertices.add(v2);
            }
        }
        for (Integer id : match.vertices.keySet()) {
            if (this.vertexTypes.get(id) == VertexType.INTERNAL) {
                var outgoing = this.patternOutgoingEdges.get(id);
                Vertex originalVertex = match.vertices.get(id);
                Vertex reflectionVertex = null;
                for (Edge edge : outgoing) {
                    Vertex refl = match.vertices.get(edge.end);
                    if (refl.getType() == Vertex.Type.VIRTUAL) {
                        if (reflectionVertex != null && reflectionVertex != refl){
                            return false;
                        }
                        reflectionVertex = match.vertices.get(edge.end);
                    }
                }

            }
        }
        return true;
    }

    private List<Match> findMatches(Vertex vertex, int point, Set<Edge> validating,
                                                   Set<ExtendedCrease> validatingCreases, ExtendedCreasePattern ecp,
                                                   boolean inverted) {
        if (vertexTypes.get(point) == VertexType.BORDER) {
            if (vertex.getType() == Vertex.Type.BORDER || vertex.getType() == Vertex.Type.VIRTUAL) {
                return Collections.singletonList(new Match(point, vertex));
            } else {
                return Collections.emptyList();
            }
        } else {
            List<Match> mappings = new ArrayList<>();
            List<Edge> outgoingEdges = patternOutgoingEdges.get(point);
            List<ExtendedCrease> outgoingCreases = ecp.getAdjacencyLists().get(vertex);
            for (int i = 0; i < outgoingCreases.size(); i++) {
                boolean valid = true;
                ExtendedCrease outgoingCrease = outgoingCreases.get(i);
                if (validatingCreases.contains(outgoingCrease) || !checkMV(outgoingEdges.get(0), outgoingCrease, inverted)) {
                    continue;
                }
                Match currentMapping = new Match(point, vertex);
                List<Match> currentMappings = new ArrayList<>();
                currentMappings.add(currentMapping);
                Set<Edge> vEdges = new HashSet<>();
                Set<ExtendedCrease> vCreases = new HashSet<>();
                for (int j = 0; j < outgoingEdges.size(); j++) {
                    Edge currentEdge = outgoingEdges.get(j);
                    if (validating.contains(currentEdge)) {
                        continue;
                    }
                    ExtendedCrease currentCrease = outgoingCreases.get((i+j)%outgoingCreases.size());
                    if (!checkMV(currentEdge, currentCrease, inverted) || validatingCreases.contains(currentCrease)) {
                        valid = false;
                        break;
                    }
                    validating.add(currentEdge);
                    vEdges.add(currentEdge);
                    validatingCreases.add(currentCrease);
                    for (Match mapping : currentMappings) {
                        mapping.addEdge(currentEdge, currentCrease);
                    }
                    vCreases.add(currentCrease);
                    List<Match> newMappings = findMatches(currentCrease.getEndVertex(),
                            currentEdge.end, validating, validatingCreases, ecp, inverted);
                    currentMappings = Match.mergeAll(currentMappings, newMappings);
                }
                validating.remove(outgoingEdges.get(0));
                validatingCreases.remove(outgoingCrease);
                validating.removeAll(vEdges);
                validatingCreases.removeAll(vCreases);
                if (valid) {
                    mappings.addAll(currentMappings);
                }
            }
            return mappings;
        }
    }

    private boolean checkMV(Edge edge, ExtendedCrease crease, boolean inverted) {
        if (inverted) {
            return edge.type.opposite() == crease.getType();
        }
        return edge.type == crease.getType();
    }
}