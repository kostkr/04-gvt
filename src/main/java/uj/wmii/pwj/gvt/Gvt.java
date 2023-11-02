package uj.wmii.pwj.gvt;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class Gvt {

    private final ExitHandler exitHandler;

    private final String separator = System.getProperty("file.separator");

    public Gvt(ExitHandler exitHandler) {
        this.exitHandler = exitHandler;
    }

    public static void main(String... args) {
        Gvt gvt = new Gvt(new ExitHandler());
        gvt.mainInternal(args);
    }

    void mainInternal(String... args) {
        if(args.length == 0)
            exitHandler.exit(1, "Please specify command.");
        else if (args[0].equals("init"))
            init();
        else if (!Files.isDirectory(Path.of(".gvt")))
            exitHandler.exit(-2, "Current directory is not initialized. Please use init command to initialize.");
        else if (args[0].equals("add"))
            add(args);
        else if (args[0].equals("detach"))
            detach(args);
        else if (args[0].equals("checkout"))
            checkout(args);
        else if (args[0].equals("commit"))
            commit(args);
        else if (args[0].equals("history"))
            history(args);
        else if (args[0].equals("version"))
            version(args);
        else
            exitHandler.exit(1, "Unknown command.");
    }

    private void init(){
        if(Files.isDirectory(Path.of(".gvt"))) {
            exitHandler.exit(10, "Current directory is already initialized.");
        }else try {
            new File(".gvt" + separator + "0").mkdirs();
            new File(".gvt" + separator + "0" + separator + "message.txt").createNewFile();

            setMessage(new File(".gvt" + separator + "0" + separator + "message.txt"), "GVT initialized.");
            exitHandler.exit(0,"Current directory initialized successfully.");
        }catch (Exception e){
            exitHandler.exit(-3, "Underlying system problem. See ERR for details.");
        }
    }

    private void add(String... args){
        if(args.length < 2){
            exitHandler.exit(20, "Please specify file to add.");
        }else if(!new File(args[1]).exists()){
            exitHandler.exit(21, "File not found. File: " + args[1]);
        }else try{
            int lastVersion = getLastVersion();
            if(new File(".gvt" + separator + lastVersion + separator + args[1]).exists()){
                exitHandler.exit(0, "File already added. File: " + args[1]);
            }else {
                new File(".gvt" + separator + (lastVersion+1)).mkdir();
                copyDir(new File(".gvt" + separator + lastVersion), new File(".gvt" + separator + (lastVersion+1)));
                copyFile(new File(args[1]), new File(".gvt" + separator + (lastVersion + 1) + separator + args[1]));

                String message = getOptionalParam("-m", args);
                if(message == null)
                    message = "File added successfully. File: " + args[1];
                setMessage(new File(".gvt" + separator + (lastVersion + 1) + separator + "message.txt"), message);

                exitHandler.exit(0,"File added successfully. File: " + args[1]);
            }
        }catch (Exception e){
            exitHandler.exit(22, "File cannot be added. See ERR for details. File: " + args[1]);
        }
    }

    private void detach(String... args) {
        if (args.length < 2){
            exitHandler.exit(30, "Please specify file to detach.");
        } else try{
            int lastVersion = getLastVersion();
            if (!new File(".gvt" + separator + lastVersion + separator + args[1]).exists())
                exitHandler.exit(0, "File is not added to gvt. File: " + args[1]);
            else{
                new File(".gvt" + separator + (lastVersion + 1)).mkdir();
                copyDir(new File(".gvt" + separator + lastVersion), new File(".gvt" + separator + (lastVersion + 1)));
                new File(".gvt" + separator + (lastVersion + 1) + separator + args[1]).delete();

                String comment = getOptionalParam("-m", args);
                if (comment == null)
                    comment = "File detached successfully. File: " + args[1];
                setMessage(new File(".gvt" + separator + (lastVersion + 1) + separator + "message.txt"), comment);
                exitHandler.exit(0, "File detached successfully. File: " + args[1]);
            }
        }catch (IOException e){
            exitHandler.exit(31, "File cannot be detached, see ERR for details. File: " + args[1]);
        }
    }

    private void checkout(String... args){
         if (!new File(".gvt" + separator + args[1]).exists()){
            exitHandler.exit(60, "Invalid version number: " + args[1]);
        } else try {
            copyDir(new File(".gvt" + separator + args[1]), new File("."));
            exitHandler.exit(0, "Checkout successful for version: " + args[1]);
        } catch (IOException e){
            exitHandler.exit(-3, "Underlying system problem. See ERR for details.");
        }
    }

    private void commit(String... args){
        if (args.length < 2) {
            exitHandler.exit(50, "Please specify file to commit.");
        } else if (!new File(args[1]).exists()) {
            exitHandler.exit(51, "File not found. File: " + args[1]);
        } else try{
            int lastVersion = getLastVersion();
            if(!new File(".gvt" + separator + lastVersion + separator + args[1]).exists()){
                exitHandler.exit(0, "File is not added to gvt. File: " + args[1]);
            }else {
                new File(".gvt" + separator + (lastVersion + 1)).mkdir();
                copyFile(new File(args[1]), new File(".gvt" + separator + (lastVersion + 1) + separator + args[1]));

                String comment = getOptionalParam("-m", args);
                if (comment == null)
                    comment = "File committed successfully. File: " + args[1];
                setMessage(new File(".gvt" + separator + (lastVersion + 1) + separator + "message.txt"), comment);
                exitHandler.exit(0, "File committed successfully. File: " + args[1]);
            }
        } catch (IOException e) {
            exitHandler.exit(52, "File cannot be committed, see ERR for details. File: " + args[1]);
        }
    }

    private void history(String... args){
        int lastVersion = getLastVersion();
        int last = 0;
        try{
            last = Integer.parseInt(getOptionalParam("-last", args));
            --last;
        }
        catch(Exception e){
            last = lastVersion;
        }

        try {
            StringBuilder message = new StringBuilder();

            for(int version = lastVersion; version >= 0 && last >= 0; --version, --last){
                Scanner sc = new Scanner(new File(".gvt" + separator + version + separator + "message.txt"));
                message.append(version + ": " + sc.nextLine() + "\n");
                sc.close();
            }

            exitHandler.exit(0, message.toString());
        } catch (IOException e) {
            exitHandler.exit(-3, "Underlying system problem. See ERR for details.");
        }
    }

    private void version(String... args){
        int version = 0;
        if(args.length < 2) version = getLastVersion();
        else try {
            version = Integer.parseInt(args[1]);
        }catch (NumberFormatException e) {}

        try {
            if (!new File(".gvt" + separator + version).exists())
                exitHandler.exit(60, "Invalid version number: " + version);
            else
                exitHandler.exit(0, "Version: " + version + "\n" +
                        Files.readString(Path.of(".gvt" + separator + version + separator + "message.txt")));
        } catch (IOException e){
            exitHandler.exit(60, "Invalid version number: " + version);
        }
    }

    private void setMessage(File file, String message){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath()))) {
            writer.write(message);
        }catch (IOException e){
            exitHandler.exit(-3, "Underlying system problem. See ERR for details.");
        }
    }

    private int getLastVersion(){
        int version = 0;
        while(new File(".gvt" + separator + version).exists()){++version;}
        return version - 1;
    }

    private void copyDir(File source, File destination) throws IOException {
        File[] listOfFiles = source.listFiles();
        if (listOfFiles != null)
            for (File file : listOfFiles) {
                Files.copy(file.toPath(), destination.toPath().resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
    }

    private void copyFile(File source, File destination) throws IOException{
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    String getOptionalParam(String param, String... args) {
        for (int i = 1; i < args.length; i++)
            if (args[i].equals(param))
                return args[i + 1];
        return null;
    }
}