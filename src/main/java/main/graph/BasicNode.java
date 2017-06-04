package main.graph;

import java.util.HashSet;
import java.util.Set;

public class BasicNode<T> {

    private T object;
    private Set<BasicNode<T>> edgesIn;
    private Set<BasicNode<T>> edgesOut;


    public BasicNode(T object){
        this.object = object;
        edgesIn = new HashSet<>();
        edgesOut = new HashSet<>();
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


    public T getObject() {
        return object;
    }

    @Override
    public String toString() {
        return object.toString();
    }
}
