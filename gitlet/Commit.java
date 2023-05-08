package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.HashMap;


/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Sam Xu
 */
public class Commit implements Serializable {
    /**
     *
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    private String message;
    private String time;
    private String ID;
    private String branchName;
    private HashMap<String, Blob> blobs;
    private String parent;
    private String mergeParent;
    public String getMsg() {
        return message;
    }
    public String getTime() {
        return time;
    }
    public void setBranch(String branch) {
        branchName = branch;
    }
    public HashMap<String, Blob> getMap() {
        return blobs;
    }
    public String getBranch() {
        return branchName;
    }
    public String getID() {
        return ID;
    }
    public String getParent() {
        return parent;
    }
    public String getMergeParent() {
        return mergeParent;
    }

    private File CWD = new File(".");
    private File GITLET = new File(CWD, ".gitlet");
    private File commit = new File(GITLET, "commit");

    public Commit() {
        CWD.mkdir();
        GITLET.mkdir();
        commit.mkdir();
        message = "initial commit";
        time = "Thu Jan 1 00:00:00 1970 -0800";
        branchName = "master";
        blobs = new HashMap<>();
        parent = null;
        mergeParent = null;
        setCommitID();
        saveCommit();
    }
    public Commit(String message, String parent,
                  String mergeParent, String branchName,
                  ArrayList<Blob> blobs) {
        this.message = message;
        this.parent = parent;
        this.mergeParent = mergeParent;
        setTime();
        setMap(blobs);
        this.branchName = branchName;
        setCommitID();
        saveCommit();
    }
    public void setCommitID() {
        String id = "" + message + time;
        if (branchName != null) {
            id += branchName;
        }
        if (parent != null) {
            id += parent;
        }
        ID = Utils.sha1(id);
    }
    public void setTime() {
        DateTimeFormatter a = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss YYYY Z");
        time =  ZonedDateTime.now().format(a);
    }
    public void setMap(ArrayList<Blob> b) {
        this.blobs = new HashMap<>();
        if (b.size() != 0) {
            for (Blob blob : b) {
                this.blobs.put(blob.getName(), blob);
            }
        }
    }
    public void saveCommit() {
        Utils.writeContents(new File(commit, ID), (Object) Utils.serialize(this));
    }
}
