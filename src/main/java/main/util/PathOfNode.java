package main.util;

import main.graph.BasicNode;
import main.graph.EvolutionModel;
import main.graph.ParameterizedEdge;
import main.graph.Tuple;

import java.util.HashSet;
import java.util.Set;

public class PathOfNode {


    public static Set<Tuple<BasicNode<String>, Integer>> getPathOfNode(BasicNode<String> node){
        Set<Tuple<BasicNode<String>, Integer>> set = new HashSet<>();

        set.addAll(node.getTransitionIn());

        for(Tuple<BasicNode<String>, Integer> tuple: node.getTransitionIn()){
            set.addAll(getPathOfNode(tuple.getT1()));
        }

        return set;
    }
}
