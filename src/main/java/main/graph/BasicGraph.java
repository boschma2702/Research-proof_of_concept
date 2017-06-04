package main.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BasicGraph<T> {


    private Set<BasicNode<T>> nodes;
    private Set<BasicEdge<T>> edges;

    private Map<T, BasicNode<T>> lookup;

    public BasicGraph(){
        nodes = new HashSet<>();
        edges = new HashSet<>();
        lookup = new HashMap<>();
    }

    public void addNode(BasicNode<T> node){
        nodes.add(node);
        lookup.put(node.getObject(), node);
    }

    /**
     * Adds an edge to the main.graph. This also adds the in and out edge to the nodes in the edge
     * @param edge
     */
    public void addEdge(BasicEdge<T> edge){
        edges.add(edge);
        edge.getFrom().addOut(edge.getTo());
        edge.getTo().addIn(edge.getFrom());
    }

    /**
     * Adds two nodes as an edge to the main.graph. This also adds the in and out edge to the nodes
     * @param from starting point of the edge
     * @param to ending of the edge
     */
    public void addEdge(BasicNode<T> from, BasicNode<T> to){
        edges.add(new BasicEdge<>(from, to));
        from.addOut(to);
        to.addIn(from);
    }

    public void addEdge(T from, T to){
        BasicNode<T> fromNode = lookup.get(from);
        BasicNode<T> toNode = lookup.get(to);
        if(fromNode==null || toNode==null){
            throw new RuntimeException(String.format("identifier not present as node: %s. %s", from.toString(), to.toString()));
        }
        edges.add(new BasicEdge<>(fromNode, toNode));
        fromNode.addOut(toNode);
        toNode.addIn(fromNode);
    }

    /**
     * This function only works if the T are unique
     * @param object
     * @return the node object in the main.graph, null if the node is not present
     */
    public BasicNode<T> getNode(T object){
        return lookup.get(object);
    }

    public Set<BasicNode<T>> getNodes() {
        return nodes;
    }

    public Set<BasicEdge<T>> getEdges() {
        return edges;
    }

    @Override
    public String toString() {
        String n = "";
        for(BasicNode bn : nodes){
            n += bn.toString() + ", ";
        }
        String e = "";
        for(BasicEdge be : edges){
            e += be.toString();
        }
        return String.format("nodes: %s \n edges: %s", n, e);
    }
}
