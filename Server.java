import java.net.*;
import java.util.concurrent.Semaphore;
import java.io.*;


public class Server {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[0]);
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
            serverSocket.setReuseAddress(true);

            //cache service
            Cache cache = new Cache(10);
            //file service
            FileSystem fileSys = new FileSystem();

            

            //Semaphores permit only one use at a time
            Semaphore fileSysSem = new Semaphore(1);
            Semaphore cacheSem = new Semaphore(1);

            //Keep track of clients to distinguish between them
            int clients = 0;
            System.out.println("Server Start");

            while(true){
                //accept requests 
                Socket clientSocket = serverSocket.accept();
                clients++;
                System.out.println("New client: N" + clients);

                ClientHandler thread = new ClientHandler(clientSocket, clients, cache, fileSys, cacheSem, fileSysSem);
                thread.start();

            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            e.printStackTrace();;
        }
        finally{
            if(serverSocket != null){
                try{serverSocket.close();}
                catch(IOException e ){e.printStackTrace();}
            }
        }
    }
}

class ClientHandler extends Thread{
    Socket socket;
    int clientNum;

    static Cache cache;
    static FileSystem fileSystem;

    static Semaphore cacheSem;
    static Semaphore fileSystemSem;

    DataOutputStream outStream = null;
    DataInputStream inStream = null;

    ClientHandler(Socket socket, int num, Cache cch, FileSystem fileSys, Semaphore cahceSem, Semaphore fileSysSem){
        this.socket = socket;
        this.clientNum = num;
        cache = cch;
        fileSystem = fileSys;
        cacheSem = cahceSem;
        fileSystemSem = fileSysSem;
    }

    public void run(){

        try{
            outStream = new DataOutputStream(socket.getOutputStream());
            //inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            inStream = new DataInputStream(socket.getInputStream());

            String clientInput = "";
            String response = "";

            //Input loop
            while(clientInput != "End"){
                clientInput = inStream.readUTF();
                System.out.println("Client message N" +clientNum+ ": " + clientInput);

                response = processInput(clientInput, inStream, clientNum); 
                outStream.writeUTF(response);
            }

        }
        catch(IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally{
            if(outStream != null){
                try {
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String processInput(String input, DataInputStream inStream, int clientNum) throws IOException, InterruptedException{
        String command;
        String arg1 = null;
        String arg2 = null;
        
        try{ 
            String[] ins = input.split(" ");
            command = ins[0];
            if(ins.length == 2){
                arg1 = ins[1];
            }
            else if(ins.length == 3){
                arg1 = ins[1];
                arg2 = ins[2];
            }
        }
        catch(Exception e) {command = input;}//if unable to split, input has no args


        if(arg1 == null && arg2 == null){
            //commands with no parameters
            switch (command) {
                case "getFiles":
                    return getFiles();
                default:
                    return "Error: invalid input";
            }
        }
        else if(arg2 == null){ 
            //commands with only one parameter
             switch (command) {
                case "save":
                    String data = inStream.readUTF();
                    return saveFile(arg1+".txt", data);
                case "update":
                    String replacement = inStream.readUTF();
                    return updateFile(arg1+".txt", replacement);

                case "remove":
                    return removeFile(arg1+".txt");

                case "read":
                    return readFile(arg1+".txt");

                case "getTotal":
                    return getTotals(arg1, clientNum);
            
                default:
                    return "Error: invalid input";
            }
        }
        else{
            //commands with 2 parameters
            switch (command) {
                case "get":
                    
                    return getCounts(arg1, arg2);
            
                default:
                    return "Error: invalid command";
            }
        }
            
    }

    private static String saveFile(String name, String content) throws InterruptedException, IOException{

        WordCounter counter = new WordCounter(content);
        boolean valid = counter.validate();

        if(valid){
            int characters = counter.characters;
            int words = counter.words;
            int lines = counter.lines;

            //Critical section
            fileSystemSem.acquire();
            boolean status = fileSystem.storeFile(name, content, characters, words, lines);
            fileSystemSem.release();
            //Critical section end
            
            if(status){
                
                ////Critical section
                //save counts to cache
                cacheSem.acquire();
                cache.add(characters, name+"-chars");
                cache.add(words, name+"-words");
                cache.add(lines, name+"-lines");

                //total counts are now outdated
                //remove total counts from cache if they exist
                cache.remove("total-chars");
                cache.remove("total-words");
                cache.remove("total-lines");

                cacheSem.release();
                //critical section end

                return "File has been saved";
            }
            return "File already exists";

        }

        else{
            return String.join("  ", counter.errors);
        }

    }

    private static String updateFile(String name, String content) throws IOException, InterruptedException{
        WordCounter counter = new WordCounter(content);
        boolean valid = counter.validate();
        if(valid){
            int characters = counter.characters;
            int words = counter.words;
            int lines = counter.lines;

            //Critical section
            fileSystemSem.acquire();
            boolean updated = fileSystem.updateFile(name, content, characters, words, lines);
            fileSystemSem.release();
            //Critical section end
            
            if(updated){
                ////Critical section
                cacheSem.acquire();
                //old counts for this file are now outdated
                //remove old counts if they exist
                cache.remove(name+"-chars");
                cache.remove(name+"-words");
                cache.remove(name+"-lines");

                //save new counts to cache
                cache.add(characters, name+"-chars");
                cache.add(words, name+"-words");
                cache.add(lines, name+"-lines");

                //total counts are now outdated
                //remove total counts from cache if they exist
                cache.remove("total-chars");
                cache.remove("total-words");
                cache.remove("total-lines");

                cacheSem.release();
                //critical section end

                return "File has been updated";
            }
            return "Error: file does not exist, cannot be updated";

        }

        else{
            return String.join("  ", counter.errors);
        }

    }

    private static String removeFile(String filename) throws InterruptedException, IOException{
        
        //Critical section
        fileSystemSem.acquire();
        boolean removed = fileSystem.removeFile(filename);
        fileSystemSem.release();
        //Critical section end

        if(removed){

                //total counts are now outdated
                //remove total counts from cache if they exist
                //Critical section
                cacheSem.acquire();
                cache.remove("total-chars");
                cache.remove("total-words");
                cache.remove("total-lines");
                cacheSem.release();
                //Critical section end

            return "File was removed";
        }
        return "Could not remove file; it may not exist";
    }

    private static String readFile(String filename) throws FileNotFoundException, InterruptedException{

        //Critical section
        fileSystemSem.acquire();
        String content= fileSystem.readFile(filename);
        fileSystemSem.release();
        //Critical section end

        if(content == null){
            return "File does not exist";
        }
        else{
            return content;
        }

    }
    
    private static String getCounts(String filename, String categories) throws IOException, InterruptedException{
        String counts = "";

        //check if the file exists
        fileSystemSem.acquire();
        boolean fileExists = fileSystem.contains(filename);
        fileSystemSem.release();

        if(!fileExists){
            return "Error: file does not exist";
        }


        for(int i = 0; i < categories.length(); i ++){
            if(categories.charAt(i) == 'w'){

                //Try to find in cache first
                cacheSem.acquire();
                Object value = cache.search(filename+"-words");
                cacheSem.release();

                if(value == null){ //does not exist in cache
                    //find in file system instead
                    fileSystemSem.acquire();
                    counts += "words: " + fileSystem.getWordCount(filename);
                    counts += " ";
                    fileSystemSem.release();
                }
                else{
                    counts += "Words: "+value;
                    counts += " ";
                }

            }
            else if(categories.charAt(i) == 'c'){
                //Try to find in cache first
                cacheSem.acquire();
                Object value = cache.search(filename+"-chars");
                cacheSem.release();

                if(value == null){ //does not exist in cache
                    //find in file system instead
                    fileSystemSem.acquire();
                    counts += "chars: " + fileSystem.getCharCount(filename);
                    counts += " ";
                    fileSystemSem.release();
                }
                else{
                    counts += "chars: "+value;
                    counts += " "; 
                }
            }
            else if(categories.charAt(i) == 'l'){
                //Try to find in cache first
                cacheSem.acquire();
                Object value = cache.search(filename+"-lines");
                cacheSem.release();

                if(value == null){ //does not exist in cache
                    //find in file system instead
                    fileSystemSem.acquire();
                    counts += "lines: " + fileSystem.getLineCount(filename);
                    counts += " ";
                    fileSystemSem.release();
                }
                else{
                    counts += "lines: "+value;
                    counts += " "; 
                }
            }
            else{
                return "Error: input invalid";
            }
        }

        return counts;
    }

    private static String getTotals(String categories, int cleintNum) throws FileNotFoundException, InterruptedException{

        String totals = "";

        for(int i = 0; i < categories.length(); i ++){
            if(categories.charAt(i) == 'w'){
                //check cache first
                System.out.println("N"+cleintNum+" waiting for cache");
                cacheSem.acquire();
                System.out.println("N"+cleintNum+" aquired cache");
                Object value = cache.search("total-words");
                cacheSem.release();
                System.out.println("N"+cleintNum+" released cache");

                if(value == null){//does not exist in cache
                    fileSystemSem.acquire();
                    int totalWords = fileSystem.getTotalWords();
                    totals += "total words: " + totalWords;
                    totals += " ";
                    fileSystemSem.release();

                    //add to cache
                    System.out.println("N"+cleintNum+" waiting for cache");
                    cacheSem.acquire();
                    System.out.println("N"+cleintNum+" aquired cache");
                    cache.add(totalWords, "total-words");
                    cacheSem.release();
                    System.out.println("N"+cleintNum+" released cache");

                }
                else{//found in cache
                    totals += "total words: " + value;
                    totals += " ";
                    
                }
            }
            else if(categories.charAt(i) == 'c'){
                //check cache first
                System.out.println("N"+cleintNum+" waiting for cache");
                cacheSem.acquire();
                System.out.println("N"+cleintNum+" aquired cache");
                Object value = cache.search("total-chars");
                cacheSem.release();
                System.out.println("N"+cleintNum+" released cache");

                if(value == null){//does not exist in cache
                    fileSystemSem.acquire();
                    int totalChars = fileSystem.getTotalChars();
                    totals += "total chars: " + totalChars;
                    totals += " ";
                    fileSystemSem.release();

                    //add to cache
                    System.out.println("N"+cleintNum+" waiting for cache");
                    cacheSem.acquire();
                    System.out.println("N"+cleintNum+" aquired cache");
                    cache.add(totalChars, "total-chars");
                    cacheSem.release();
                    System.out.println("N"+cleintNum+" released cache");

                }
                else{//found in cache
                    totals += "total chars: " + value;
                    totals += " ";
                    
                }
            }
            else if(categories.charAt(i) == 'l'){
                //check cache first
                System.out.println("N"+cleintNum+" waiting for cache");
                cacheSem.acquire();
                System.out.println("N"+cleintNum+" aqcuired cache");
                Object value = cache.search("total-lines");
                cacheSem.release();
                System.out.println("N"+cleintNum+" aqcuired cache");

                if(value == null){//does not exist in cache
                    fileSystemSem.acquire();
                    int totalLines = fileSystem.getTotalLines();
                    totals += "total lines: " + totalLines;
                    totals += " ";
                    fileSystemSem.release();

                    //add to cache
                    System.out.println("N"+cleintNum+" waiting for cache");
                    cacheSem.acquire();
                    cache.add(totalLines, "total-Lines");
                    cacheSem.release();

                }
                else{//found in cache
                    totals += "total lines: " + value;
                    totals += " ";
                    
                }
            }
            else{
                return "Error: input invalid";
            }
        }

        return totals;
    }
    
    private static String getFiles() throws InterruptedException{

        //Critical section
        fileSystemSem.acquire();
        String files = fileSystem.listFiles();
        fileSystemSem.release();
        //Critical section end

        if(files.length() > 0){
            return files;
        }
        return "No files yet";
    }


}
