import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {
    static DataOutputStream outStream;
    static boolean replaceCR = true;
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        try{
            Socket socket = new Socket(hostName, portNumber);
            //PrintWriter outStream = new PrintWriter(socket.getOutputStream(), true);
            outStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inStream = new DataInputStream(socket.getInputStream());

            Scanner scanner = new Scanner(System.in);
            String userInput = "";
            System.out.println(welcomeMessage());

            //Input loop
            while (!userInput.equals("exit")) {
                //read input
                userInput = scanner.nextLine();

                //send input
                if(userInput.startsWith("save") || userInput.startsWith("update")){
                    try{
                        //command and file name
                        String[] ins = userInput.split(" ");
                        userInput = ins[0] + " " + ins[1];
                        

                        //contents of the file
                        Path path = Paths.get(ins[2]);
                        String data = Files.readString(path);
                        data = data.replace("\r", "");

                        outStream.writeUTF(userInput);
                        outStream.writeUTF(data);

                        //print server response
                        System.out.println(inStream.readUTF());

                    }
                    catch(Exception e){
                        System.out.println("Error: invalid input");
                    }
                }
                else if(!userInput.equals("exit")){
                    
                    outStream.writeUTF(userInput);
                    System.out.println(inStream.readUTF());
                }        
            }

            scanner.close();
            socket.close();

        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }

    private static String welcomeMessage(){
        return """
            ####################################
            --------------OPTIONS--------------
            ####################################

            Syntax: command variable variable
            Menu legend: command <variable> [combination]

            combination refers to any combination of the characters inside the bracket
            c = characters, w = words, l = lines

            Example: get myFile cw | returns the character and word count for "myFile"
            
            save <filename> <filepath.txt> 
            update <filename> <filepath.txt> 
            remove <filename> 
            read <filename> 

            getFiles
            getTotal [cwl]

            get <filename> [cwl]

            exit

            """;           
    }

 
}