package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Sammy
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            Repository.exit("Must have at least one argument");
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                String name = args[1];
                Repository.add(name);
                break;
            case "commit":
                if (args.length == 1){
                    Repository.exit("Please enter a commit message.");
                }
                String message = args[1];
                Repository.commit(message);
                break;
            case "log":
                Repository.logAll();
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "checkout":
                if (args.length == 4 && args[2].equals("--")) {
                    if (args[1].length() >= 35) {
                        Repository.checkID(args[1], args[3]);
                    } else {
                        Repository.checkAbr(args[1], args[3]);
                    }
                    break;
                }
                if (args.length == 3 && args[1].equals("--")) {
                    Repository.checkName(args[2]);
                    break;
                }
                if (args.length == 2) {
                    Repository.checkBranch(args[1]);
                    break;
                }
                break;
            case "find":
                Repository.find(args[1]);
                break;
            case "status":
                Repository.status();
                break;
            case "rm":
                Repository.remove(args[1]);
                break;
            case "branch":
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                if (args[1].length() < 35) {
                    Repository.resetShort(args[1]);
                } else {
                    Repository.reset(args[1]);
                }
                break;
            case "merge":
                Repository.merge(args[1]);
                break;
            default:
                Repository.exit("No command with that name exists");
        }
    }
}
