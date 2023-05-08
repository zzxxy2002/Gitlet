package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {

    private String name;

    private String content;

    private String blobID;

    public Blob(String name) {
        File filePath = new File(name);
        this.name = filePath.getName();
        this.content = Utils.readContentsAsString(filePath);
        this.blobID = Utils.sha1(this.content);
    }
    
    public String getName() {
        return this.name;
    }

    public String getContent() {
        return this.content;
    }
}
