package main.graph;

public class ParameterizedEdge<T, T2> extends BasicEdge<T> {

    private T2 parameter;

    public ParameterizedEdge(BasicNode<T> from, BasicNode<T> to, T2 paramater) {
        super(from, to);
        this.parameter = paramater;
    }

    public T2 getParameter() {
        return parameter;
    }

//    @Override
//    public String toString() {
////        return "("+from.toString()+", "+to.toString()+")";
//
////        return String.format("\"%s\" -> \"%s\"\n", from.toString().substring(0,7), to.toString().substring(0,7));
//        return String.format("\"%s\" -> \"%s\": %s,\n", getFrom().toString(), getTo().toString(), parameter);
////        return from.toString().substring(0,5)+" -> "+to.toString().substring(0,5)+"\n";
//    }
}
