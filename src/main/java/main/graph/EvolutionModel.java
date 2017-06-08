package main.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EvolutionModel {

    private BasicGraph<String> evolutionGraph;
    private Map<BasicNode<String>, BasicGraph<String>> snapshotGraphs;
    private Set<ParameterizedEdge<String, Integer>> transitionEdges;
    
    public EvolutionModel(){
        evolutionGraph = new BasicGraph<>();
        snapshotGraphs = new HashMap<>();
        transitionEdges = new HashSet<>();
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


}

