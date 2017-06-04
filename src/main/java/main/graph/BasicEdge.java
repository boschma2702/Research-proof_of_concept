package main.graph;

public class BasicEdge<T> {

    private BasicNode<T> from;
    private BasicNode<T> to;

    public BasicEdge(BasicNode<T> from, BasicNode<T> to){
        this.from = from;
        this.to = to;
    }

    public BasicNode<T> getFrom() {
        return from;
    }

    public BasicNode<T> getTo() {
        return to;
    }

    @Override
    public String toString() {
//        return "("+from.toString()+", "+to.toString()+")";

//        return String.format("\"%s\" -> \"%s\"\n", from.toString().substring(0,7), to.toString().substring(0,7));
        return String.format("\"%s\" -> \"%s\"\n", from.toString(), to.toString());
//        return from.toString().substring(0,5)+" -> "+to.toString().substring(0,5)+"\n";
    }
}
