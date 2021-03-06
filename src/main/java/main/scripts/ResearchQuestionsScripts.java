package main.scripts;

import main.graph.*;
import main.util.NoMergeNodes;
import main.util.PathOfNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ResearchQuestionsScripts {


    public static List<Integer> getClassEditsPerCommit(EvolutionModel model){
        List<Integer> l = new ArrayList<>();
        Set<BasicNode<String>> Vnon = NoMergeNodes.getNonMergeNodes(model);
        for(BasicNode<String> versionNode : Vnon){
            int amountChanged = 0;
            for(BasicNode<String> snapshotNode: model.getSnapshotGraphOfVersion(versionNode).getNodes()){
                if(snapshotNode.getTransitionIn().size()>1){
                    throw new IllegalStateException(String.format("Vnon not working correctly for version: %s class: %s \n transisition edges: %s", versionNode.getObject(), snapshotNode.getObject(), snapshotNode.getTransitionIn()));
                }
                for(Tuple<BasicNode<String>, Integer> tuple : snapshotNode.getTransitionIn()){
                    if(tuple.getT2()>0){
                        amountChanged ++;
                    }
                }
            }

            l.add(amountChanged);
        }
        return l;
    }


    public static HashMap<String, List<Integer>> getAmountOfLOCPerCommitPerPerson(EvolutionModel model){
//        List<Integer> l = new ArrayList<>();
        Set<BasicNode<String>> Vnon = NoMergeNodes.getNonMergeNodes(model);
        HashMap<String, List<Integer>> map = new HashMap<>();
        for(BasicNode<String> versionNode : Vnon){
            String commiter = "";
            Set<BasicEdge<String>> incommingEdges = model.getEvolutionGraph().getIncommingEdgesOfNode(versionNode);
            if(incommingEdges.size()>1){
                throw new IllegalStateException("Vnon not working correctly");
            }
            if(incommingEdges.size()==1) {
                for (BasicEdge<String> edge : incommingEdges) {
                    commiter = ((ParameterizedEdge<String, String>) edge).getParameter();
                }

                int amountChanged = 0;
                for (BasicNode<String> snapshotNode : model.getSnapshotGraphOfVersion(versionNode).getNodes()) {
                    if (snapshotNode.getTransitionIn().size() > 1) {
                        throw new IllegalStateException("Vnon not working correctly");
                    }
                    for (Tuple<BasicNode<String>, Integer> tuple : snapshotNode.getTransitionIn()) {
                        amountChanged += tuple.getT2();
                    }
                }
                if (!map.containsKey(commiter)) {
                    map.put(commiter, new ArrayList<>());
                }
                map.get(commiter).add(amountChanged);
            }
        }
        return map;
    }

    public static List<Tuple<String, List<Double>>> getClassesScoreChange(EvolutionModel model, BasicNode<String> finalVersion){
        List<Tuple<String, List<Double>>> list = new ArrayList<>();
        for(BasicNode<String> snapshotNode : model.getSnapshotGraphOfVersion(finalVersion).getNodes()){
            Set<Tuple<BasicNode<String>, Integer>> path = PathOfNode.getPathOfNode(snapshotNode);
            int amountChanged = 0;
            for(Tuple<BasicNode<String>, Integer> tuple : path){
                if(tuple.getT2()>0){
                    amountChanged ++;
                }
            }
            double score;
            if(path.size()==0){
                score = 0;
            }else{
                score = 1.0 * amountChanged / path.size() * amountChanged;
            }
            ArrayList<Double> l = new ArrayList<>();
            l.add(score);
            l.add((double) path.size());
            l.add((double) amountChanged);
            list.add(new Tuple<>(snapshotNode.getObject(), l));
        }
        return list;
    }

}
