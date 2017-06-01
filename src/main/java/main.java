import graph.BasicNode;
import graph.EvolutionModel;
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

public class main {

    public static final String URL = "https://github.com/JvdK/F4U-Bank";
    public static final String NAME = "research";

    public static void main(String[] args) throws IOException, GitAPIException {
        EvolutionModel model = new EvolutionModel();

        File gitFolder = new File(System.getProperty("user.dir")+"\\"+NAME);
        if(gitFolder.exists() && gitFolder.isDirectory()){
            FileUtils.deleteDirectory(gitFolder);
        }

        Git git = Git.cloneRepository().setURI(URL).setDirectory(gitFolder).call();

        System.out.println("done cloning");

        buildVersionGraph(git, model);
        System.out.println(model.getEvolutionGraph().toString());

    }


    public static void buildVersionGraph(Git git, EvolutionModel model) throws GitAPIException {
        Iterable<RevCommit> iterable = git.log().call();

        for (RevCommit i : iterable){
            String commitHash = i.getId().getName();
            RevCommit[] parents = i.getParents();

            BasicNode<String> node = model.evolutionLookup(commitHash);
            if(node==null){
                node = new BasicNode<>(commitHash);
                model.addEvolutionNode(node);
                System.out.println("miss");
            }

            for(int count=0; count<parents.length; count++){
                BasicNode<String> parent = model.evolutionLookup(parents[count].getId().getName());
                if(parent==null){
                    parent = new BasicNode<>(parents[count].getId().getName());
                    model.addEvolutionNode(parent);
                }
                model.addEvolutionEdge(parent, node, i.getCommitterIdent().getName());
            }
        }
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
