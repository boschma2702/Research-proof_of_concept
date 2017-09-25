package main.symposium;

import main.Main;
import main.exceptions.NotAClassException;
import main.graph.BasicGraph;
import main.graph.BasicNode;
import main.parser.Parser;
import main.parser.SnapshotGraphBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class GraphOfCommitGenerator {

    private static final boolean INSPECT = true;

    public static final String URL = Main.URL;
    public static final String PATH_MAIN_JAVA = Main.PATH_MAIN_JAVA;
    private static String CLASSNAME = "honours.ing.banq.account.BankAccountServiceImpl";

    public static void main(String[] args) throws GitAPIException, IOException {
        String[] hashes = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + File.separator + "hashes.txt")), StandardCharsets.UTF_8).split("\\s+");

        File gitFolder = new File(System.getProperty("user.dir") + File.pathSeparator + "research");
        if (gitFolder.exists() && gitFolder.isDirectory()) {
            FileUtils.deleteDirectory(gitFolder);
        }
        Git git = Git.cloneRepository().setURI(URL).setDirectory(gitFolder).call();
        Repository repository = git.getRepository();

        StringBuilder numberOfLines = new StringBuilder();
        StringBuilder numberOfSelfWrittenClasses = new StringBuilder();
        StringBuilder averageOutDependencies = new StringBuilder();
        StringBuilder externalUsedClasses = new StringBuilder();

        StringBuilder numberOfLinesOfClass = new StringBuilder();
        StringBuilder numberOfInDependecyOfClass = new StringBuilder();
        StringBuilder numberOfOutDependencyOfClass = new StringBuilder();

        for(String hash : hashes){
            RevCommit commit = getCommitOfId(repository, hash);
            SnapshotGraphBuilder builder = generateSnapshotGraphOfCommit(repository, commit, PATH_MAIN_JAVA);
            BasicGraph<String> snapshotGraph = builder.generateGraph();

            numberOfLines.append(getTotalLineCount(builder.getParsers())+"\t");
            numberOfSelfWrittenClasses.append(builder.getParsers().size()+"\t");
            averageOutDependencies.append(getAverageOutDependency(snapshotGraph, builder.getParsers().size())+"\t");
            externalUsedClasses.append(getNumberOfExternalUsedClasses(snapshotGraph, builder.getParsers().size())+"\t");

            BasicNode<String> classNode = getNodeWithName(snapshotGraph, CLASSNAME);
            if(classNode==null){
                numberOfLinesOfClass.append(-1+"\t");
                numberOfInDependecyOfClass.append(-1+"\t");
                numberOfOutDependencyOfClass.append(-1+"\t");
            }else{
                numberOfLinesOfClass.append(findParserWithName(builder, CLASSNAME).getNumberOflines()+"\t");
                numberOfInDependecyOfClass.append(classNode.getEdgesIn().size()+"\t");
                numberOfOutDependencyOfClass.append(classNode.getEdgesOut().size()+"\t");
            }

        }

        System.out.println("Number of lines:");
        System.out.println(numberOfLines);
        System.out.println("Number of self written classes:");
        System.out.println(numberOfSelfWrittenClasses);
        System.out.println("Average amount of out-dependencies of selfwritten classes:");
        System.out.println(averageOutDependencies);
        System.out.println("Number of external used classes");
        System.out.println(externalUsedClasses);
        System.out.println("LOC of Class");
        System.out.println(numberOfLinesOfClass);
        System.out.println("In dep of class");
        System.out.println(numberOfInDependecyOfClass);
        System.out.println("Out dep of class");
        System.out.println(numberOfOutDependencyOfClass);
    }

    public static int getTotalLineCount(List<Parser> parsers){
        int total = 0;
        for(Parser p : parsers){
            total += p.getNumberOflines();
        }
        return total;
    }

    private static Parser findParserWithName(SnapshotGraphBuilder builder, String name){
        for(Parser parser : builder.getParsers()){
            if(parser.getName().equals(CLASSNAME)){
                return parser;
            }
        }
        return null;
    }

    public static RevCommit getCommitOfId(Repository repository, String id) throws IOException {
        ObjectId commitId = ObjectId.fromString(id);
        RevWalk revWalk = new RevWalk(repository);
        RevCommit commit = revWalk.parseCommit(commitId);
        revWalk.close();
        return commit;
    }

    private static SnapshotGraphBuilder generateSnapshotGraphOfCommit(Repository repository, RevCommit commit, String pathFilter) throws IOException {
        int classesInCommit = 0;
        SnapshotGraphBuilder builder = new SnapshotGraphBuilder();
        RevTree tree = commit.getTree();

        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        try {
            if (usePathFilter(pathFilter)) {
                treeWalk.setFilter(PathFilter.create(pathFilter));
            }

            treeWalk.setFilter(PathSuffixFilter.create(".java"));
            while (treeWalk.next()) {
                if (treeWalk.isSubtree()) {
                    treeWalk.enterSubtree();
                } else {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    String contents = new String(loader.getBytes());
                    try {
                        Parser parser = new Parser(contents);
                        builder.addParser(parser);
                        classesInCommit++;
                    } catch (NotAClassException e) {
                        if(INSPECT) {
                            System.out.println("--------------------------------------------");
                            System.out.println(contents);
                            System.out.println("--------------------------------------------");
                            System.out.println("File with no class found, press any key to continue");
//                            Scanner scanner = new Scanner(System.in);
//                            scanner.next();
//                            scanner.close();
                        }
                    }
                }
            }
            return builder;
        } finally {
            treeWalk.close();
        }
    }

    private static BasicNode<String> getNodeWithName(BasicGraph<String> snapshotGraph, String name){
        for(BasicNode<String> classNode : snapshotGraph.getNodes()){
            if(classNode.getObject().equals(name)){
                return classNode;
            }
        }
        return null;
    }

    private static boolean usePathFilter(String path) {
        return !path.equals("");
    }


    private static double getAverageOutDependency(BasicGraph<String> graph, int amountSelfWrittenClasses){
        int totalOut = 0;
        for(BasicNode basicNode : graph.getNodes()){
            totalOut += basicNode.getEdgesOut().size();
        }
        BigDecimal decimal = new BigDecimal(1.0d * totalOut / amountSelfWrittenClasses);
        return decimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private static int getNumberOfExternalUsedClasses(BasicGraph graph, int amountSelfWrittenClasses){
        return graph.getNodes().size()-amountSelfWrittenClasses;
    }

}
