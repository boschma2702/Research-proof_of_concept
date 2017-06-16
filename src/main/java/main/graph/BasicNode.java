package main.graph;

import java.util.HashSet;
import java.util.Set;

public class BasicNode<T> {

    private T object;
    private Set<BasicNode<T>> edgesIn;

    public Set<BasicNode<T>> getEdgesIn() {
        return edgesIn;
    }

    public Set<BasicNode<T>> getEdgesOut() {
        return edgesOut;
    }

    public Set<Tuple<BasicNode<String>, Integer>> getTransitionIn() {
        return transitionIn;
    }

    public Set<Tuple<BasicNode<String>, Integer>> getTransitionOut() {
        return transitionOut;
    }

    private Set<BasicNode<T>> edgesOut;

    private Set<Tuple<BasicNode<String>, Integer>> transitionIn;
    private Set<Tuple<BasicNode<String>, Integer>> transitionOut;

    public BasicNode(T object){
        this.object = object;
        edgesIn = new HashSet<>();
        edgesOut = new HashSet<>();
        transitionIn = new HashSet<>();
        transitionOut = new HashSet<>();
    }

    /**
     * Used to add a incomming edge. When adding a node x to node this: this<--x
     * @param node
     */
    public void addIn(BasicNode<T> node){
        edgesIn.add(node);
    }

    /**
     * Used to add a outgoing edge. When adding a node x to node this: this-->x
     * @param node
     */
    public void addOut(BasicNode<T> node){
        edgesOut.add(node);
    }


    public void addTransitionIn(Tuple<BasicNode<String>, Integer> node){
        transitionIn.add(node);
    }

    public void addTransitionOut(Tuple<BasicNode<String>, Integer> node){
        transitionOut.add(node);
    }

    public T getObject() {
        return object;
    }

    @Override
    public String toString() {
        return object.toString();
    }
}
