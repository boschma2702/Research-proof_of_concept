package main.parser;

import main.exceptions.NotAClassException;
import main.graph.BasicGraph;
import main.graph.BasicNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SnapshotGraphBuilder {


    private List<Parser> parsers = new ArrayList<>();
    private BasicGraph<String> snapshotGraph = new BasicGraph<>();
    private Set<String> classes = new HashSet<>();

    private Set<Parser> parserDependenciesStillToResolve = new HashSet<>();

    public static void main(String[] args) throws IOException {
//        File system1 = new File("C:\\Users\\reneb_000\\Documents\\GitLab\\Research-proof_of_concept\\src\\main\\java");
        File system1 = new File("C:\\Users\\reneb_000\\Documents\\GitLab\\F4U-Bank\\src\\main\\java");
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
            try {
                Parser parser = new Parser(contence);
                list.add(parser);
            }catch (NotAClassException e){
                System.err.println("Java file detected withouth a class declaration");
            }


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
            Iterator<String> iterator = p.getUnknownExternalDependencies().iterator();
//            for(String dependency : p.getUnknownExternalDependencies()){
            while(iterator.hasNext()){
                String dependency = iterator.next();
                boolean found = false;
                checkDependency:
                for(Parser p2 : parsers){
                    if(!(p2 == p)) {
                        for (String imp : p2.getKnownDependencies()) {
                            if (imp.endsWith(dependency)) {
                                for(String prefix : p.getStarImports()){
                                    if(imp.startsWith(prefix)){
                                        snapshotGraph.addEdge(p.getName(), imp);
                                        p.getKnownDependencies().add(imp);
                                        iterator.remove();
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


    public List<Parser> getParsers() {
        return parsers;
    }
}
