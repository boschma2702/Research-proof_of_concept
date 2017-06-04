package main.parser;

import main.graph.BasicGraph;
import main.graph.BasicNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SnapshotGraphBuilder {


    List<Parser> parsers = new ArrayList<>();
    BasicGraph<String> snapshotGraph = new BasicGraph<>();
    Set<String> classes = new HashSet<>();

    Set<Parser> parserDependenciesStillToResolve = new HashSet<>();

    public static void main(String[] args) throws IOException {
        File system1 = new File("C:\\Users\\reneb_000\\Documents\\GitLab\\Research-proof_of_concept\\src\\main\\java");
        List<Parser> parsers = new ArrayList<>();
        getParsersOfFile(system1, parsers);
        SnapshotGraphBuilder builder = new SnapshotGraphBuilder();
        builder.setParsers(parsers);
        System.out.println(builder.generateGraph());

//        new Parser(readFile("C:\\Users\\reneb_000\\Documents\\GitLab\\Research-proof_of_concept\\src\\main\\java\\main\\parser\\Parser.java", Charset.defaultCharset()));
    }

    public static void getParsersOfFile(File file, List<Parser> list) throws IOException {
        if(file.isDirectory()){
            for(File f:file.listFiles()){
                getParsersOfFile(f, list);
            }
        }else{
            String contence = readFile(file.getPath(), Charset.defaultCharset());
            list.add(new Parser(contence));
        }
    }

    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }


    public void addParser(Parser parser) {
        this.parsers.add(parser);
    }

    public void setParsers(List<Parser> parsers) {
        this.parsers = parsers;
    }

    public BasicGraph<String> generateGraph() {
        for (Parser p : parsers) {
            snapshotGraph.addNode(new BasicNode<>(p.getName()));
            classes.add(p.getClassName());
        }
        for (Parser p : parsers) {
            for (String dependency : p.getKnownDependencies()) {
                if(snapshotGraph.getNode(dependency)==null){
                    snapshotGraph.addNode(new BasicNode<>(dependency));
                }
                snapshotGraph.addEdge(p.getName(), dependency);
            }
            for (String dependency : p.getDependencies()) {
                if (classes.contains(dependency)) {
                    for (Parser parser : parsers) {
                        if (parser.getClassName().equals(dependency)) {
                            snapshotGraph.addEdge(p.getName(), parser.getName());
                            break;
                        }
                    }
                } else {
                    parserDependenciesStillToResolve.add(p);
                    p.addUnknownExternalDependencies(dependency);
                    //still not found, meaning external dependency comming from start import
//                    if(snapshotGraph.getNode(dependency)==null){
//                        snapshotGraph.addNode(new BasicNode<>(dependency));
//                    }
//                    snapshotGraph.addEdge(p.getName(), dependency);
                }
            }
        }

        for(Parser p : parserDependenciesStillToResolve){
            for(String dependency : p.getUnknownExternalDependencies()){
                boolean found = false;
                checkDependency:
                for(Parser p2 : parsers){
                    if(!(p2 == p)) {
                        for (String imp : p2.getKnownDependencies()) {
                            if (imp.endsWith(dependency)) {
                                for(String prefix : p.getStarImports()){
                                    if(imp.startsWith(prefix)){
                                        snapshotGraph.addEdge(p.getName(), imp);
                                        //break to next to resolve
                                        found = true;
                                        break checkDependency;
                                    }
                                }
                            }
                        }
                    }
                }
                if(!found){
                    if(snapshotGraph.getNode(dependency)==null){
                        snapshotGraph.addNode(new BasicNode<>(dependency));
                    }
                    snapshotGraph.addEdge(p.getName(), dependency);
                }
            }
        }
        return snapshotGraph;
    }


}
