package main.symposium;

import main.exceptions.NotAClassException;
import main.parser.Parser;
import main.parser.SnapshotGraphBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import java.io.IOException;
import java.util.Scanner;

public class GraphOfCommitGenerator {

    private static final boolean INSPECT = true;

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
                            Scanner scanner = new Scanner(System.in);
                            scanner.next();
                            scanner.close();
                        }
                    }
                }
            }
            return builder;
        } finally {
            treeWalk.close();
        }
    }

    private static boolean usePathFilter(String path) {
        return !path.equals("");
    }

}
