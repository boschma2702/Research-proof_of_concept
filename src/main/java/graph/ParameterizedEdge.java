package graph;

public class ParameterizedEdge<T, T2> extends BasicEdge<T> {

    private T2 parameter;

    public ParameterizedEdge(BasicNode<T> from, BasicNode<T> to, T2 paramater) {
        super(from, to);
        this.parameter = paramater;
    }

    public T2 getParameter() {
        return parameter;
    }
}
