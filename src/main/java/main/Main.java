package main;

import main.exceptions.NotAClassException;
import main.graph.BasicGraph;
import main.graph.BasicNode;
import main.graph.EvolutionModel;
import main.graph.Tuple;
import main.parser.Parser;
import main.parser.SnapshotGraphBuilder;
import main.scripts.ResearchQuestionsScripts;
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

//    public static final String URL = "https://github.com/boschma2702/research2";
    public static final String URL = "https://github.com/JvdK/F4U-Bank";
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

    private main.util.Timer timer;

    public static void main(String[] args) throws IOException, GitAPIException {
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
        timer = new main.util.Timer();
        git = Git.cloneRepository().setURI(URL).setDirectory(gitFolder).call();
        repository = git.getRepository();
        timer.time("Done cloning");
        model = new EvolutionModel();
        Tuple<RevCommit, RevCommit> result = buildVersionGraph();
        timer.time("Done building");

        List<Integer> classEditsPerCommit = ResearchQuestionsScripts.getClassEditsPerCommit(model);
        timer.time("Done getClassEdits");
        HashMap<String, List<Integer>> amountLOCPerCommitPerPerson = ResearchQuestionsScripts.getAmountOfLOCPerCommitPerPerson(model);
        timer.time("Done amountLoc");
        List<Tuple<String, Double>> getClassChangeScore = ResearchQuestionsScripts.getClassesScoreChange(model, model.evolutionLookup(result.getT2().getId().getName()));
        timer.time("Done classChangeScores");

        System.out.println(classEditsPerCommit);
        System.out.println(amountLOCPerCommitPerPerson);
        System.out.println(getClassChangeScore);




//        System.out.println(model.getEvolutionGraph());

//        Iterable<RevCommit> iterable = git.log().call();
//
//        for (RevCommit i : iterable) {
//            System.out.println(String.format(ANSI_PURPLE + "%s: %s" + ANSI_RESET, i.getFullMessage(), i.getId().getName()));
//            System.out.println(model.getSnapshotGraphs().get(model.evolutionLookup(i.getId().getName())));
////            System.out.println(model);
//            System.out.println(model.getInTransitionEdgesOfVersion().get(model.getEvolutionGraph().getNode(i.getId().getName())));
//            System.out.println("========================");
//        }

    }


    /**
     * Builds the version graphs and calls corresponding functions to build the snapshotgraphs and transitionedges
     * @return a tupel containing the first and the last revcommit of the repository.
     * @throws GitAPIException
     * @throws IOException
     */
    public Tuple<RevCommit, RevCommit> buildVersionGraph() throws GitAPIException, IOException {
        Iterable<RevCommit> iterable = git.log().call();
        RevCommit first = null;
        RevCommit last = null;
        for (RevCommit i : iterable) {
            if(first==null){
                first = i;
            }
            String commitHash = i.getId().getName();
            RevCommit[] parents = i.getParents();

            BasicNode<String> node = model.evolutionLookup(commitHash);
            if (node == null) {
                node = new BasicNode<>(commitHash);
                model.addEvolutionNode(node);
                generateSnapshotGraphOfCommit(i, node);
            }
            for (int count = 0; count < parents.length; count++) {
                BasicNode<String> parent = model.evolutionLookup(parents[count].getId().getName());
                if (parent == null) {
                    parent = new BasicNode<>(parents[count].getId().getName());
                    model.addEvolutionNode(parent);
                }
                model.addEvolutionEdge(parent, node, i.getCommitterIdent().getName());
            }
            addSnapshotGraph(i, parents);
            last = i;
        }
        // last is the first commit made, first the last commit made
        return new Tuple<>(last, first);
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
                treeWalk.enterSubtree();
            } else {
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                try {
                    Parser parser = new Parser(new String(loader.getBytes()));
                    builder.addParser(parser);
                } catch (NotAClassException e) {
                    System.err.println("Java file found withouth class declaration");
                }
            }
        }
        model.getSnapshotGraphs().put(node, builder.generateGraph());
        snapshotGraphBuilderMap.put(commit.getId().getName(), builder);
    }

    /**
     * Creates the snapshotsgraphs. It also calls the function that is responsible to generate the transitionedges
     * @param commit current commit working on, this snapshotgraph should already been build
     * @param parents parents of the current commit. Snapshotgraphs of these will be build.
     * @throws IOException
     * @throws GitAPIException
     */
    public void addSnapshotGraph(RevCommit commit, RevCommit[] parents) throws IOException, GitAPIException {
        BasicNode<String> evolutionNode = model.getEvolutionGraph().getNode(commit.getId().getName());
        if (!(parents.length == 0)) {
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
                List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);
                Set<String> toRemoveParsers = new HashSet<>();
                Map<String, Parser> toReplaceParcers = new HashMap<>();
                Map<String, Integer> linesChangedMap = new HashMap<>();

                for (DiffEntry entry : entries) {
                    int linesChanged = getLinesChanged(diffFormatter, entry);
                    switch (entry.getChangeType()) {
                        case ADD:
                            try {
                                String parser = Parser.getName(getContentsOfChangedFile(commit, entry.getNewPath()));
                                toRemoveParsers.add(parser);
                            } catch (NotAClassException e) {
                                System.err.println("Java file found withouth class declaration");
                            }
                            break;
                        case DELETE:
                            try {
                                Parser parser = new Parser(getContentsOfChangedFile(parentCommit, entry.getOldPath()));
                                parentBuilder.addParser(parser);
                            } catch (NotAClassException e) {
                                System.err.println("Java file found withouth class declaration");
                            }
                            break;
                        case MODIFY:
                            try {
                                Parser p = new Parser(getContentsOfChangedFile(parentCommit, entry.getOldPath()));
                                toReplaceParcers.put(p.getName(), p);
                                linesChangedMap.put(p.getName(), linesChanged);
                            } catch (NotAClassException e) {
                                System.err.println("Java file found withouth class declaration");
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("undefined changetype: " + entry.getChangeType());
                    }
                }

                for (Parser p : builder.getParsers()) {
                    if (toRemoveParsers.contains(p.getName())) {
                        // do nothing
                    }else if (toReplaceParcers.keySet().contains(p.getName())) {
                        parentBuilder.addParser(toReplaceParcers.get(p.getName()));
                    }else {
                        parentBuilder.addParser(p);
                    }
                }

                snapshotGraphBuilderMap.put(parentCommit.getId().getName(), parentBuilder);
                BasicGraph<String> snapshotGraph = parentBuilder.generateGraph();
                model.getSnapshotGraphs().put(parentNode, snapshotGraph);
                addTransitionEdges(parentNode, evolutionNode, snapshotGraph, currentSnapshotGraph, linesChangedMap);
            }
        }else{
            //add artificial node
            BasicNode<String> n = new BasicNode<>("p"+commit.getId().getName());
            model.addEvolutionNode(n);
            model.addEvolutionEdge(n, evolutionNode, commit.getCommitterIdent().getName());
            model.getSnapshotGraphs().put(n, new BasicGraph<>());
        }
    }

    /**
     * Adds the transition edges. This function relies on the fact that each node in the snapshotgraph has a unique identifier
     * @param evolutionNodeFrom node from the evolutionmodel from which the transition edges originate
     * @param evolutionNodeTo node from the evolutionmodel to which the transition edges go to
     * @param fromGraph the graph where the transitionedges will originate from
     * @param toGraph the graph to which the transistionedges will go
     * @param linesChangedMap a map containing the lines changed of each class
     */
    private void addTransitionEdges(BasicNode<String> evolutionNodeFrom, BasicNode<String> evolutionNodeTo, BasicGraph<String> fromGraph, BasicGraph<String> toGraph, Map<String, Integer> linesChangedMap) {
        for (BasicNode<String> from : fromGraph.getNodes()) {
            BasicNode<String> to = toGraph.getNode(from.getObject());
            if (to != null) {
                model.addTransitionEdge(evolutionNodeFrom, evolutionNodeTo, from, to, linesChangedMap.getOrDefault(from.getObject(), 0));
            }
        }
    }

    private int getLinesChanged(DiffFormatter diffFormatter, DiffEntry entry) throws IOException {
        int sum = 0;
        for (Edit e : diffFormatter.toFileHeader(entry).toEditList()) {
            sum += e.getLengthA() + e.getLengthB();
        }
        return sum;
    }

    /**
     * Retrieves the contents of the given file located at the given path at the given commit
     * @param commit
     * @param path
     * @return contents of the file
     */
    private String getContentsOfChangedFile(RevCommit commit, String path) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
