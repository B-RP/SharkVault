import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class FileSystem {

    String rootName = "Files";
    String dataFile = "Files/Data/ServerData.txt";
    String sep = "/";

    public boolean storeFile(String fileName, String data, int chars, int words, int lines) throws IOException{

        //Initialize file object
        File newFile = new File(rootName+sep+fileName);

        //Check if it already exists 
        if (newFile.createNewFile()) {
            //Read the input line by line and add to the new file in server.
            Scanner reader = new Scanner(data);
            FileWriter writer = new FileWriter(newFile);

            writer.write(data);

            reader.close();
            writer.close();

            FileWriter dataWriter = new FileWriter(dataFile, true);
            dataWriter.write(fileName + " " + chars + " " + words + " " + lines + '\n');
            dataWriter.close();

            return true;
            
        } else {
            //file already exists 
            return false;
        }
    }

    public boolean updateFile(String fileName, String data, int chars, int words, int lines) throws IOException{

        File storedFile = new File(rootName+sep+fileName);
        //check if file exists
        if(storedFile.exists()){
            FileWriter writer = new FileWriter(storedFile);
            //rewrite the file with new data
            writer.write(data);
            writer.close();

            //update server data file
            File file = new File(dataFile);
            String newData = "";

            Scanner reader = new Scanner(file);
            while(reader.hasNextLine()){
                String line = reader.nextLine();
                //copy everything except the line with the updated file
                if(!line.startsWith(fileName)){
                    newData += line + '\n';
                }
                else{
                    //replace old info with new info
                    newData += fileName + " " + chars + " " + words + " " + lines + '\n';
                }
            }
            reader.close();
            //rewrite the file with the new data
            FileWriter dataWriter = new FileWriter(file);
            dataWriter.write(newData);
            dataWriter.close();

            return true;
        
        }
        else{
            //File does not exist, cannot be updated
            return false;
        }
    }

    public boolean removeFile(String fileName) throws IOException{
        File storedFile = new File(rootName+sep+fileName);

        if(storedFile.exists()){
            boolean deleted = storedFile.delete();

            if(deleted){
                //update server data file
                File file = new File(dataFile);
                String newData = "";

                Scanner reader = new Scanner(file);
                while(reader.hasNextLine()){
                    String line = reader.nextLine();
                    //copy everything except the line with the deleted file
                    if(!line.startsWith(fileName)){
                        newData += line + '\n';
                    }
                }
                reader.close();
                //rewrite the file with the new data
                FileWriter dataWriter = new FileWriter(file);
                dataWriter.write(newData);
                dataWriter.close();
                return true;
            }
        }
        return false;
    }

    public String listFiles(){
        File directory = new File(rootName);
        File[] files = directory.listFiles();

        String fileNames = "";

        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            if(!name.equals("Data")){
                fileNames += name;
                fileNames += " ";
            }
        }
        return fileNames;
    }

    public String readFile(String fileName) throws FileNotFoundException{
        File storedFile = new File(rootName+sep+fileName);

        if(storedFile.exists()){
            String data = "";

            Scanner reader = new Scanner(storedFile);
            while(reader.hasNextLine()){
                data += reader.nextLine();
            }
            reader.close();
            return data;
        }
        else{
            return null;
        }

    }

    public int getCharCount(String filename) throws FileNotFoundException{
        File file = new File(dataFile);

        Scanner reader = new Scanner(file);
        while(reader.hasNextLine()){
            String line = reader.nextLine();

            if(line.startsWith(filename)){
                reader.close();
                String[] info = line.split(" ");
                return Integer.parseInt(info[1]);
            }
        }
        reader.close();

        return -1;

    }
    
    public int getWordCount(String filename) throws IOException{
        File file = new File(dataFile);

        Scanner reader = new Scanner(file);
        while(reader.hasNextLine()){
            String line = reader.nextLine();
            System.out.println(line);
            System.out.println(filename);

            if(line.startsWith(filename)){
                reader.close();
                String[] info = line.split(" ");
                return Integer.parseInt(info[2]);
            }
        }
        reader.close();

        return -1;

    }
    
    public int getLineCount(String filename) throws IOException{
        File file = new File(dataFile);

        Scanner reader = new Scanner(file);
        while(reader.hasNextLine()){
            String line = reader.nextLine();
            System.out.println(line);
            System.out.println(filename);

            if(line.startsWith(filename)){
                reader.close();
                String[] info = line.split(" ");
                return Integer.parseInt(info[3]);
            }
        }
        reader.close();

        return -1;

    }

    public int getTotalChars() throws FileNotFoundException{
        int words = 0;

        File file = new File(dataFile);
        Scanner reader = new Scanner(file);

        while(reader.hasNextLine()){
            String line = reader.nextLine();
            String[] data = line.split(" ");
            words += Integer.parseInt(data[1]);
        }

        reader.close();

        return words;
    }

    public int getTotalWords() throws FileNotFoundException{
        int words = 0;

        File file = new File(dataFile);
        Scanner reader = new Scanner(file);

        while(reader.hasNextLine()){
            String line = reader.nextLine();
            String[] data = line.split(" ");
            words += Integer.parseInt(data[2]);
        }

        reader.close();
        
        return words;
    }

    public int getTotalLines() throws FileNotFoundException{
        int words = 0;

        File file = new File(dataFile);
        Scanner reader = new Scanner(file);

        while(reader.hasNextLine()){
            String line = reader.nextLine();
            String[] data = line.split(" ");
            words += Integer.parseInt(data[3]);
        }

        reader.close();
        
        return words;
    }

    public boolean contains(String filename){

        File file = new File(rootName+sep+filename);
        if(file.exists()){
            return true;
        }
        else{
            return false;

        }

    }
}
