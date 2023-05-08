package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;



/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Sam Xu
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */


    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** gitlet *//** commit */
    private static final File COMMIT = new File(GITLET_DIR, "commit");
    /** branch */
    private static final File BRANCH = new File(GITLET_DIR, "branch");
    /** stageAdd */
    private static final File STAGE = new File(GITLET_DIR, "stageAdd");
    /** stageRemove */
    private static final File PAST_STAGE = new File(GITLET_DIR, "stageRm");
    private static ArrayList<String> allbranches;
    private static String branchNow;
    private static Commit currentCommit;

    /** Init */
    public static void init() {
        if (GITLET_DIR.exists()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        } else {
            COMMIT.mkdir();
            GITLET_DIR.mkdir();
            BRANCH.mkdir();
            PAST_STAGE.mkdir();
            STAGE.mkdir();
            currentCommit = new Commit();
            branchNow = "master";
            allbranches = new ArrayList<>();
            allbranches.add(branchNow);
            writeContents(new File(GITLET_DIR, "HEAD"), serialize(currentCommit));
            writeContents(new File(BRANCH, branchNow), serialize(currentCommit));
        }
    }

    public static void add(String file) {
        File f = new File(file);
        if (!f.exists()) {
            exit("File does not exist.");
        }
        currentCommit = getHead();
        File checkRemove = new File(PAST_STAGE, file);
        if (checkRemove.exists()) {
            checkRemove.delete();
            return;
        }
        HashMap<String, Blob> mapP2B = currentCommit.getMap();
        for (String name : mapP2B.keySet()) {
            if (name.equals(file)) {
                if (readContentsAsString(f).equals(mapP2B.get(file).getContent())) {
                    File checkAdd = new File(STAGE, file);
                    if (checkAdd.exists()) {
                        checkAdd.delete();
                    }
                    return;
                }
            }
        }
        Blob added = new Blob(file);
        writeContents(new File(STAGE, added.getName()), serialize(added));
        HashMap<String, String> remote = new HashMap<>();
        //writeRemote(remote);//
    }

    /** Commit */
    public static void commit(String message) {
        if (Objects.requireNonNull(STAGE.list()).length == 0
                && Objects.requireNonNull(PAST_STAGE.list()).length == 0) {
            exit("No changes added to the commit.");
        }
        if (message.length() == 0) {
            exit("Please enter a commit message.");
        }
        currentCommit = getHead();
        branchNow = currentCommit.getBranch();
        HashMap<String, Blob> bMap = currentCommit.getMap();
        ArrayList<Blob> blobs = new ArrayList<>();
        for (String name : Objects.requireNonNull(plainFilenamesIn(PAST_STAGE))) {
            bMap.remove(name);
        }
        blobs.addAll(bMap.values());
        for (File file : STAGE.listFiles()) {
            blobs.add(readObject(file, Blob.class));
        }
        String[] checkMerge = message.split("\\s+");
        if (checkMerge[0].equals("Merged")) {
            currentCommit = new Commit(message, currentCommit.getID(),
                    readObject(new File(BRANCH, checkMerge[1]), Commit.class).getID(),
                    currentCommit.getBranch(), blobs);
        } else {
            currentCommit = new Commit(message, currentCommit.getID(),
                    null, currentCommit.getBranch(), blobs);
        }
        currentCommit.saveCommit();
        writeContents(new File(GITLET_DIR, "HEAD"), serialize(currentCommit));
        writeContents(new File(BRANCH, branchNow), serialize(currentCommit));
        clear(STAGE);
        clear(PAST_STAGE);
    }

    public static void globalLog() {
        for (File file : COMMIT.listFiles()) {
            if (!file.isDirectory()) {
                logOne(readObject(file, Commit.class).getID());
            }
        }
    }

    public static void remove(String filename) {
        currentCommit = getHead();
        File file = new File(filename);
        if (!file.exists() && !currentCommit.getMap().keySet().contains(filename)) {
            exit("File does not exist.");
        }
        List<String> list = plainFilenamesIn(STAGE);
        if (!list.contains(filename) && !currentCommit.getMap().keySet().contains(filename)) {
            exit("No reason to remove the file.");
        }
        if (list.contains(filename)) {
            File checkAdd = new File(STAGE, filename);
            checkAdd.delete();
            return;
        }
        if (currentCommit.getMap() != null) {
            Blob blobs = currentCommit.getMap().get(filename);
            writeContents(new File(PAST_STAGE, blobs.getName()), serialize(blobs));
        }

        restrictedDelete(new File(CWD, filename));
    }
    private static void clear(File path) {
        for (File file : path.listFiles()) {
            file.delete();
        }
    }
    private static Commit getHead() {
        return readObject(new File(".gitlet/HEAD"), Commit.class);
    }

    private static void logOne(String commitID) {
        Commit commit = getCommit(commitID);
        System.out.println("===" + "\n" + "commit "
                + commit.getID() + "\n" + "Date: " + commit.getTime() + "\n"
                + commit.getMsg() + "\n");
    }

    public static void logAll() {
        currentCommit = getHead();
        String commitID = currentCommit.getID();
        while (commitID != null) {
            logOne(commitID);
            commitID = Objects.requireNonNull(getCommit(commitID)).getParent();
        }
    }

    private static Commit getCommit(String filename) {
        File file = new File(COMMIT, filename);
        if (!file.exists()) {
            exit("No commit with that id exists.");
            return null;
        } else {
            return readObject(file, Commit.class);
        }
    }

    public static void checkID(String id, String fileName) {
        File file = new File(COMMIT, id);
        if (!file.exists()) {
            exit("No commit with that id exists.");
        } else {
            Commit commit = readObject(file, Commit.class);
            HashMap<String, Blob> givenMap = commit.getMap();
            if (!givenMap.containsKey(fileName)) {
                exit("File does not exist in that commit.");
            }
            writeContents(new File(fileName), givenMap.get(fileName).getContent());
        }
    }

    public static void checkAbr(String id, String filename) {
        List<String> allCommits = plainFilenamesIn(COMMIT);
        for (String i : allCommits) {
            if (i.contains(id)) {
                checkID(i, filename);
                return;
            }
        }
        exit("No commit with that id exists.");
    }

    public static void find(String message) {
        int times = 0;
        for (File file : COMMIT.listFiles()) {
            Commit commit = readObject(file, Commit.class);
            if (commit.getMsg().equals(message)) {
                System.out.println(commit.getID());
                times++;
            }
        }
        if (times == 0) {
            exit("Found no commit with that message.");
        }
    }

    public static void status() {
        List<String> list = plainFilenamesIn(COMMIT);
        if (list.size() == 0) {
            exit("Not in an initialized Gitlet directory.");
        }
        currentCommit = getHead();
        branchNow = currentCommit.getBranch();
        List<String> local = plainFilenamesIn(CWD);
        List<String> rm = plainFilenamesIn(PAST_STAGE);
        HashMap<String, Blob> map = currentCommit.getMap();
        System.out.println("=== Branches ===");
        for (String filename : plainFilenamesIn(BRANCH)) {
            if (filename.equals(branchNow)) {
                System.out.print("*");
            }
            System.out.println(filename);
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String name : plainFilenamesIn(STAGE)) {
            System.out.println(name);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String name : plainFilenamesIn(PAST_STAGE)) {
            System.out.println(name);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String name : ckModify(currentCommit)) {
            System.out.println(name);
        }
        String result = "";
        for (String keys : map.keySet()) {
            if (!(local.contains(keys) || rm.contains(keys))) {
                result += keys + " (deleted)";
            }
        }
        System.out.println(result);
        System.out.println("=== Untracked Files ===");
        for (String name : untracked(currentCommit)) {
            System.out.println(name);
        }
        System.out.println();
    }

    private static ArrayList<String> ckModify(Commit curCommit) {
        ArrayList<String> modifiedList = new ArrayList<>();
        HashMap<String, Blob> map = curCommit.getMap();
        for (File files : CWD.listFiles()) {
            if (!files.isDirectory() && map.containsKey(files.getName())
                    && !map.get(files.getName()).getContent().equals(readContentsAsString(files))) {
                modifiedList.add(files.getName() + " (modified)");
            }
        }
        return modifiedList;
    }

    public static ArrayList<String> untracked(Commit commit) {
        ArrayList<String> u = new ArrayList<>();
        for (File file : CWD.listFiles()) {
            if (!file.isDirectory()) {
                String name = file.getName();
                File t = new File(STAGE, name);
                if (!(t.exists() || commit.getMap().keySet().contains(name))) {
                    u.add(name);
                }
            }
        }
        return u;
    }

    public static void checkName(String fileName) {
        currentCommit = getHead();
        if (!currentCommit.getMap().containsKey(fileName)) {
            exit("File does not exist in that commit.");
        }
        File f = new File(fileName);
        String contents = currentCommit.getMap().get(fileName).getContent();
        writeContents(f, contents);
    }

    public static void checkBranch(String branch) {
        File f = new File(BRANCH, branch);
        if (!f.exists()) {
            exit("No such branch exists.");
        }
        currentCommit = getHead();
        if (currentCommit.getBranch().equals(branch)) {
            exit("No need to checkout the current branch.");
        }
        ArrayList<String> untrack = untracked(currentCommit);
        if (untrack.size() != 0) {
            exit("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        Commit branchHead = readObject(f, Commit.class);
        for (String filename : currentCommit.getMap().keySet()) {
            if (Objects.requireNonNull(plainFilenamesIn(CWD)).contains(filename)) {
                restrictedDelete(filename);
            }
        }
        HashMap<String, Blob> branchMap = branchHead.getMap();
        for (String filename : branchMap.keySet()) {
            String contents = branchMap.get(filename).getContent();
            writeContents(new File(filename), contents);
        }
        writeContents(new File(".gitlet/HEAD"), Utils.serialize(branchHead));
        clear(STAGE);
        clear(PAST_STAGE);
    }

    public static void branch(String branchName) {
        File newBranch = new File(BRANCH, branchName);
        if (newBranch.exists()) {
            exit("A branch with that name already exists.");
        }
        currentCommit = getHead();
        currentCommit.setBranch(branchName);
        writeContents(newBranch, serialize(currentCommit));
    }

    public static void rmBranch(String branchName) {
        File branch = new File(BRANCH, branchName);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
        }
        currentCommit = getHead();
        if (currentCommit.getBranch().equals(branchName)) {
            exit("Cannot remove the current branch.");
        }
        branch.delete();
    }

    public static void reset(String id) {
        File file = new File(COMMIT, id);
        if (!file.exists()) {
            exit("No commit with that id exists.");
        }
        ArrayList<String> untrack = untracked(getHead());
        if (untrack.size() != 0) {
            exit("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        Commit givenCommit = getCommit(id);

        currentCommit = getHead();
        HashMap<String, Blob> map = currentCommit.getMap();
        List<String> local = plainFilenamesIn(CWD);
        for (String filename : map.keySet()) {
            if (local.contains(filename)) {
                restrictedDelete(filename);
            }
        }

        List<String> addNames = plainFilenamesIn(STAGE);
        for (String filename : addNames) {
            if (local.contains(filename)) {
                restrictedDelete(filename);
            }
        }
        HashMap<String, Blob> givenMap = givenCommit.getMap();
        for (String filename : givenMap.keySet()) {
            File f = new File(CWD, filename);
            writeContents(f, givenMap.get(filename).getContent());
        }
        writeContents(new File(".gitlet/HEAD"), serialize(givenCommit));
        String givenBranch = givenCommit.getBranch();
        writeContents(new File(BRANCH, givenBranch), serialize(givenCommit));
        clear(STAGE);
        clear(PAST_STAGE);
    }

    public static void resetShort(String commitID) {
        List<String> allCommits = plainFilenamesIn(COMMIT);
        for (String id : allCommits) {
            if (id.contains(commitID)) {
                reset(id);
                return;
            }
        }
        exit("No commit with that id exists.");
    }

    public static void merge(String branchName) {
        mergeConditions(branchName);
        Commit givenBranch = readObject(new File(BRANCH, branchName), Commit.class);
        Commit splitPoint = lowestCommonAncestor(givenBranch, currentCommit);
        if (splitPoint.getID().equals(givenBranch.getID())) {
            exit("Given branch is an ancestor of the current branch.");
        }
        if (splitPoint.getID().equals(currentCommit.getID())) {
            checkBranch(givenBranch.getBranch());
            exit("Current branch fast-forwarded.");
        }
        HashSet<String> names = new HashSet<>();
        HashMap<String, Blob> split = splitPoint.getMap();
        names.addAll(split.keySet());
        HashMap<String, Blob> branchMap = givenBranch.getMap();
        names.addAll(branchMap.keySet());
        HashMap<String, Blob> map = currentCommit.getMap();
        names.addAll(map.keySet());
        for (String name : names) {
            String print = "<<<<<<< HEAD";
            print += "\n";
            if ((split.containsKey(name)
                    && map.containsKey(name)
                    && !branchMap.containsKey(name))
                    && split.get(name).getContent().equals(map.get(name).getContent())) {
                remove(name);
            } else if ((!split.containsKey(name)
                    && !map.containsKey(name)
                    && branchMap.containsKey(name))
                    || (split.containsKey(name)
                    && map.containsKey(name)
                    && branchMap.containsKey(name)
                    && split.get(name).getContent().equals(map.get(name).getContent())
                    && !split.get(name).getContent().equals(branchMap.get(name).getContent()))) {
                writeContents(new File(name), branchMap.get(name).getContent());
                add(name);
            } else if (split.containsKey(name)
                    && !map.containsKey(name)
                    && branchMap.containsKey(name)
                    && !split.get(name).getContent().equals(branchMap.get(name).getContent())) {
                print += "=======\n";
                print += branchMap.get(name).getContent();
                print += ">>>>>>>\n";
                doConflict(name, print);
            } else if (split.containsKey(name)
                    && map.containsKey(name)
                    && !branchMap.containsKey(name)
                    && !split.get(name).getContent().equals(map.get(name).getContent())) {
                print += map.get(name).getContent();
                print += "=======\n";
                print += ">>>>>>>\n";
                doConflict(name, print);
            } else if ((split.containsKey(name)
                && map.containsKey(name) && branchMap.containsKey(name)
                && !map.get(name).getContent().equals(split.get(name).getContent())
                && !map.get(name).getContent().equals(branchMap.get(name).getContent())
                && !branchMap.get(name).getContent().equals(split.get(name).getContent()))
                || (!split.containsKey(name)
                && map.containsKey(name)
                && branchMap.containsKey(name)
                && !map.get(name).getContent().equals(branchMap.get(name).getContent()))) {
                print += map.get(name).getContent();
                print += "=======\n";
                print += branchMap.get(name).getContent();
                print += ">>>>>>>\n";
                doConflict(name, print);
            }
        }
        commit("Merged " + branchName + " into " + currentCommit.getBranch() + ".");
    }

    private static void mergeConditions(String branchName) {
        if (PAST_STAGE.list().length != 0 || STAGE.list().length != 0) {
            exit("You have uncommitted changes.");
        }
        List<String> nameList = plainFilenamesIn(BRANCH);
        if (!nameList.contains(branchName)) {
            exit("A branch with that name does not exist.");
        }
        currentCommit = getHead();
        branchNow = currentCommit.getBranch();
        if (branchName.equals(branchNow)) {
            exit("Cannot merge a branch with itself.");
        }
        ArrayList<String> untracked = untracked(currentCommit);
        if (untracked.size() != 0) {
            exit("There is an untracked file in the way; delete it, or add and commit it first.");
        }
    }

    private static void doConflict(String name, String content) {
        writeContents(new File(name), content);
        add(name);
        System.out.println("Encountered a merge conflict.");
    }

    public static Commit lowestCommonAncestor(Commit branchA, Commit branchT) {
        HashSet<String> branchAComSet = new HashSet<>();
        String cmtIDA = branchA.getID();
        while (cmtIDA != null) {
            branchAComSet.add(cmtIDA);
            Commit curCMT = getCommit(cmtIDA);
            if (curCMT.getMergeParent() != null) {
                branchAComSet.add(curCMT.getMergeParent());
            }
            cmtIDA = curCMT.getParent();
        }
        String cmtIDT = branchT.getID();
        while (cmtIDT != null) {
            Commit curCMT = getCommit(cmtIDT);
            if (branchAComSet.contains(cmtIDT)) {
                return curCMT;
            }
            if (curCMT.getMergeParent() != null
                    && branchAComSet.contains(curCMT.getMergeParent())) {
                return getCommit(curCMT.getMergeParent());
            }
            cmtIDT = curCMT.getParent();
        }
        return null;
    }

    static void exit(String m) {
        if (m != null && !m.equals("")) {
            System.out.println(m);
        }
        System.exit(0);
    }

}
