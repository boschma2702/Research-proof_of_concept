package main.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EvolutionModel {

    private BasicGraph<String> evolutionGraph;
    private Map<BasicNode<String>, BasicGraph<String>> snapshotGraphs;

    private Set<ParameterizedEdge<String, Integer>> transitionEdges;
    private HashMap<BasicNode<String>, Set<ParameterizedEdge<String, Integer>>> inTransitionEdgesOfVersion;
    private HashMap<BasicNode<String>, Set<ParameterizedEdge<String, Integer>>> outTransitionEdgesOfVersion;

    public EvolutionModel(){
        evolutionGraph = new BasicGraph<>();
        snapshotGraphs = new HashMap<>();
        transitionEdges = new HashSet<>();
        inTransitionEdgesOfVersion = new HashMap<>();
        outTransitionEdgesOfVersion = new HashMap<>();
    }
    
    public void addEvolutionNode(BasicNode<String> node){
        evolutionGraph.addNode(node);
    }
    
    public void addEvolutionEdge(BasicNode<String> from, BasicNode<String> to, String committerId){
        ParameterizedEdge<String, String> edge = new ParameterizedEdge<>(from, to, committerId);
        evolutionGraph.addEdge(edge);
    }

    public BasicNode evolutionLookup(String s){
        return evolutionGraph.getNode(s);
    }

    public BasicGraph<String> getEvolutionGraph() {
        return evolutionGraph;
    }

    public BasicGraph<String> getSnapshotGraphOfVersion(BasicNode<String> node){
        return snapshotGraphs.get(node);
    }

    public Map<BasicNode<String>, BasicGraph<String>> getSnapshotGraphs() {
        return snapshotGraphs;
    }

    public Set<ParameterizedEdge<String, Integer>> getTransitionEdges() {
        return transitionEdges;
    }

    public void addTransitionEdge(BasicNode<String> versionFrom, BasicNode<String> versionTo, BasicNode<String> from, BasicNode<String> to, int linesChanged){
        ParameterizedEdge<String, Integer> transitionEdge = new ParameterizedEdge<>(from, to, linesChanged);
        transitionEdges.add(transitionEdge);
        from.addTransitionOut(new Tuple<>(to, linesChanged));
        to.addTransitionIn(new Tuple<>(from, linesChanged));

        addTransistionEdgeToMap(versionFrom, transitionEdge, outTransitionEdgesOfVersion);
        addTransistionEdgeToMap(versionTo, transitionEdge, inTransitionEdgesOfVersion);
    }

    private void addTransistionEdgeToMap(BasicNode node, ParameterizedEdge edge, HashMap<BasicNode<String>, Set<ParameterizedEdge<String, Integer>>> map){
        Set<ParameterizedEdge<String, Integer>> set = map.computeIfAbsent(node, k -> new HashSet<>());
        set.add(edge);
    }

    public HashMap<BasicNode<String>, Set<ParameterizedEdge<String, Integer>>> getInTransitionEdgesOfVersion() {
        return inTransitionEdgesOfVersion;
    }

    public HashMap<BasicNode<String>, Set<ParameterizedEdge<String, Integer>>> getOutTransitionEdgesOfVersion() {
        return outTransitionEdgesOfVersion;
    }
}

