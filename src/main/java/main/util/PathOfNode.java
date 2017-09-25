package main.util;

import main.graph.BasicNode;
import main.graph.EvolutionModel;
import main.graph.ParameterizedEdge;
import main.graph.Tuple;

import java.util.*;

public class PathOfNode {


    public static List<Tuple<BasicNode<String>, Integer>> getPathOfNode(BasicNode<String> node, Set<BasicNode<String>> snapShotsDone){
        List<Tuple<BasicNode<String>, Integer>> result = new LinkedList<>();
        result.addAll(node.getTransitionIn());
        snapShotsDone.add(node);
        for(Tuple<BasicNode<String>, Integer> tuple: node.getTransitionIn()){
            if(!snapShotsDone.contains(tuple.getT1())){
                result.addAll(getPathOfNode(tuple.getT1(), snapShotsDone));
            }
        }
//
//        Set<Tuple<BasicNode<String>, Integer>> set = new HashSet<>();
//
//        set.addAll(node.getTransitionIn());
//
//        for(Tuple<BasicNode<String>, Integer> tuple: node.getTransitionIn()){
//            set.addAll(getPathOfNode(tuple.getT1()));
//        }

        return result;
    }
}
