package main.util;

import main.graph.BasicNode;
import main.graph.EvolutionModel;

import java.util.HashSet;
import java.util.Set;

public class NoMergeNodes {

    public static Set<BasicNode<String>> getNonMergeNodes(EvolutionModel model){
        Set<BasicNode<String>> set = new HashSet<>();
        for(BasicNode<String> node :model.getEvolutionGraph().getNodes()){
            if(node.getEdgesIn().size()<2){
                set.add(node);
            }
        }
        return set;
    }

}
