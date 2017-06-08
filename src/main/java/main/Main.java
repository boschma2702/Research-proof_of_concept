package main;

import main.exceptions.NotAClassException;
import main.graph.BasicGraph;
import main.graph.BasicNode;
import main.graph.EvolutionModel;
import main.parser.Parser;
import main.parser.SnapshotGraphBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;


import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.eclipse.jgit.lib.ObjectChecker.tree;

public class Main {

        public static final String URL = "https://github.com/meteoorkip/GraphterEffects";
//    public static final String URL = "https://github.com/boschma2702/research2";
    public static final String NAME = "research";

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static void main(String[] args) throws IOException, GitAPIException {
//        EvolutionModel model = new EvolutionModel();
//
//        File gitFolder = new File(System.getProperty("user.dir") + "\\" + NAME);
//        if (gitFolder.exists() && gitFolder.isDirectory()) {
//            FileUtils.deleteDirectory(gitFolder);
//        }
//
//        Git git = Git.cloneRepository().setURI(URL).setDirectory(gitFolder).call();
//
//        System.out.println("done cloning");
//
//        buildVersionGraph(git, model);
//        System.out.println(model.getEvolutionGraph().toString());
        new Main();
    }

    private Git git;
    private Repository repository;
    private Map<String, SnapshotGraphBuilder> snapshotGraphBuilderMap = new HashMap<>();
    private EvolutionModel model;

    public Main() throws IOException, GitAPIException {
        File gitFolder = new File(System.getProperty("user.dir") + "\\" + NAME);
        if (gitFolder.exists() && gitFolder.isDirectory()) {
            FileUtils.deleteDirectory(gitFolder);
        }
        git = Git.cloneRepository().setURI(URL).setDirectory(gitFolder).call();
        repository = git.getRepository();
        System.out.println("done cloning");
        model = new EvolutionModel();
        buildVersionGraph();
        Iterable<RevCommit> iterable = git.log().call();

        for (RevCommit i : iterable) {
            System.out.println(String.format(ANSI_PURPLE + "%s" + ANSI_RESET, i.getFullMessage()));
            System.out.println(model.getSnapshotGraphs().get(model.evolutionLookup(i.getId().getName())));
            System.out.println("========================");
        }
    }


    public void buildVersionGraph() throws GitAPIException, IOException {
        Iterable<RevCommit> iterable = git.log().call();

        for (RevCommit i : iterable) {

            String commitHash = i.getId().getName();
            RevCommit[] parents = i.getParents();

            BasicNode<String> node = model.evolutionLookup(commitHash);
            if (node == null) {
                node = new BasicNode<>(commitHash);
                model.addEvolutionNode(node);
                generateSnapshotGraphOfCommit(i, node);
//                System.out.println("miss");
            }
//            List<BasicNode<String>> parentNodes = new ArrayList<>();
            for (int count = 0; count < parents.length; count++) {
                BasicNode<String> parent = model.evolutionLookup(parents[count].getId().getName());
                if (parent == null) {
                    parent = new BasicNode<>(parents[count].getId().getName());
                    model.addEvolutionNode(parent);
                }
//                parentNodes.add(parent);
                model.addEvolutionEdge(parent, node, i.getCommitterIdent().getName());
            }
            addSnapshotGraph(i, parents);
        }

    }

    private void generateSnapshotGraphOfCommit(RevCommit commit, BasicNode<String> node) throws IOException {
        SnapshotGraphBuilder builder = new SnapshotGraphBuilder();
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(false);
        treeWalk.setFilter(PathFilter.create("src/main/java/"));
        treeWalk.setFilter(PathSuffixFilter.create(".java"));
        while (treeWalk.next()) {
            if (treeWalk.isSubtree()) {
//                    System.out.println("dir: " + treeWalk.getPathString());
                treeWalk.enterSubtree();
            } else {
//                    System.out.println("file: " + treeWalk.getPathString());
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                try{
                    Parser parser = new Parser(new String(loader.getBytes()));
                    builder.addParser(parser);
                }catch (NotAClassException e){
                    System.err.println("Java file found withouth class declaration");
                }

            }
        }
        model.getSnapshotGraphs().put(node, builder.generateGraph());
        snapshotGraphBuilderMap.put(commit.getId().getName(), builder);
//        System.out.println(model.getSnapshotGraphs().get(node));
    }

    public void addSnapshotGraph(RevCommit commit, RevCommit[] parents) throws IOException, GitAPIException {
        if (!(parents.length == 0)) {
            BasicNode<String> evolutionNode = model.getEvolutionGraph().getNode(commit.getId().getName());
            BasicGraph<String> currentSnapshotGraph = model.getSnapshotGraphOfVersion(evolutionNode);
            SnapshotGraphBuilder builder = snapshotGraphBuilderMap.get(commit.getId().getName());

            for (RevCommit parentCommit : parents) {
                BasicNode<String> parentNode = model.getEvolutionGraph().getNode(parentCommit.getId().getName());
                SnapshotGraphBuilder parentBuilder = new SnapshotGraphBuilder();

                ObjectReader reader = repository.newObjectReader();
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset(reader, parentCommit.getTree().getId());
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset(reader, commit.getTree().getId());

                DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
                diffFormatter.setRepository(repository);
                diffFormatter.setPathFilter(PathFilter.create("src/main/java/"));
                diffFormatter.setContext(0);
                // list containing all the changes between the commit and its parent
//                List<DiffEntry> entries = diffFormatter.scan(newTreeIter, oldTreeIter);
                List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);
                Set<String> toRemoveParsers = new HashSet<>();
                Map<String, Parser> toReplaceParcers = new HashMap<>();
                for(DiffEntry entry : entries) {
                    int linesChanged = getLinesChanged(diffFormatter, entry);
//                    System.out.println(entry);
//                    System.out.println(linesChanged);


                    switch (entry.getChangeType()) {
                        case ADD:
                            //TODO add transistion edges
                            try{
                                String parser = Parser.getName(getContentsOfChangedFile(commit, entry.getNewPath()));
                                toRemoveParsers.add(parser);
                            }catch (NotAClassException e){
                                System.err.println("Java file found withouth class declaration");
                            }
                            break;
                        case DELETE:
                            try{
                                Parser parser = new Parser(getContentsOfChangedFile(parentCommit, entry.getOldPath()));
                                parentBuilder.addParser(parser);
                            }catch (NotAClassException e){
                                System.err.println("Java file found withouth class declaration");
                            }
                            break;
                        case MODIFY:
                            try{
                                Parser p = new Parser(getContentsOfChangedFile(parentCommit, entry.getOldPath()));
                                toReplaceParcers.put(p.getName(), p);
                            }catch (NotAClassException e){
                                System.err.println("Java file found withouth class declaration");
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("undefined changetype: " + entry.getChangeType());
                    }
                }

                for(Parser p:builder.getParsers()){
                    if(toRemoveParsers.contains(p.getName())){
                       break;
                    }
                    if(toReplaceParcers.keySet().contains(p.getName())){
                        parentBuilder.addParser(toReplaceParcers.get(p.getName()));
                        break;
                    }
                    parentBuilder.addParser(p);
                }

                snapshotGraphBuilderMap.put(parentCommit.getId().getName(), parentBuilder);
                model.getSnapshotGraphs().put(parentNode, parentBuilder.generateGraph());

            }
//            System.out.println("=====================================");
        }

//        else{
//            TreeWalk treeWalk = new TreeWalk(repository);
//            treeWalk.addTree(tree);
//            treeWalk.setRecursive(false);
//            treeWalk.setFilter(PathFilter.create("src/main/java/"));
//            treeWalk.setFilter(PathSuffixFilter.create(".java"));
//            while (treeWalk.next()) {
//
//
//                if (treeWalk.isSubtree()) {
//                    System.out.println("dir: " + treeWalk.getPathString());
//                    treeWalk.enterSubtree();
//                } else {
//                    System.out.println("file: " + treeWalk.getPathString());
//                }
//            }
//            System.out.println("=========================================");
//        }
    }

    private int getLinesChanged(DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int sum = 0;
        for(Edit e: diffFormatter.toFileHeader(entry).toEditList()){
            sum += e.getLengthA() + e.getLengthB();
        }
        return sum;
    }

//    private String getContentsOfChangedFile (RevCommit parentCommit, DiffEntry entry){
//        return getContentsOfChangedFile(parentCommit, entry.getNewPath());
//    }

    private String getContentsOfChangedFile(RevCommit commit, String path){
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(path));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file: " + path);
            }
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            return new String(loader.getBytes());
        }catch (IncorrectObjectTypeException e) {
            e.printStackTrace();
        } catch (CorruptObjectException e) {
            e.printStackTrace();
        } catch (MissingObjectException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


//    public static void main(String[] args) throws GitAPIException, IOException {
//        Git git = Git.cloneRepository()
//                .setURI("https://github.com/boschma2702/research")
//                .call();
//
//        Iterable<RevCommit> iterable = git.log().call();
//
////        for (RevCommit i : iterable){
////            System.out.println(String.format("#%s parentcount: %d desc: %s", i.getId().getName(), i.getParentCount(), i.getFullMessage()));
////        }
//
//        Repository repository = git.getRepository();
//
//        try (RevWalk revWalk = new RevWalk(repository)) {
//            RevCommit commit = revWalk.parseCommit(ObjectId.fromString("0144505eba0c826e5dfe880e3a1ca392f9cb449d"));
//            // and using commit's tree find the path
//            RevTree tree = commit.getTree();
//            System.out.println("Having tree: " + tree);
//
//            // now try to find a specific file
//            try (TreeWalk treeWalk = new TreeWalk(repository)) {
//                treeWalk.addTree(tree);
//                treeWalk.setRecursive(true);
//                treeWalk.setFilter(PathSuffixFilter.create(".java"));
//
//                while(treeWalk.next()){
//                    ObjectId objectId = treeWalk.getObjectId(0);
//                    ObjectLoader loader = repository.open(objectId);
//                    System.out.println(treeWalk.getPathString());
//                    // and then one can the loader to read the file
//                    loader.copyTo(System.out);
//                }
//
////                if (!treeWalk.next()) {
////                    throw new IllegalStateException("Did not find expected file 'README.md'");
////                }
////
////                ObjectId objectId = treeWalk.getObjectId(0);
////                ObjectLoader loader = repository.open(objectId);
////
////                // and then one can the loader to read the file
////                loader.copyTo(System.out);
////                System.out.println(treeWalk.next());
//
//            }
//
//            revWalk.dispose();
//        }
//
//    }


}
