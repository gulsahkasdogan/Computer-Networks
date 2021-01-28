
// @ author Gulsah Kasdogan 
// Traverse a folder structure and download the required images
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InputStream;
import java.io.*;
import java.util.*;

public class TaxonomySearcher {
    /**
     * This is the main method
     * 
     * @param args used to take localhost and port
     * @return Nothing.
     * @exception IOException On input errors.
     * @see IOException
     */
    private static Socket clientSocket = null;
    private static InputStream inputStream = null;
    private static InputStreamReader inputStreamReader = null;
    private static BufferedReader fromServer = null;
    private static PrintWriter writer = null;
    private static Scanner in = null;
    private static DataOutputStream out = null;

    private static boolean userNameAuth = false;
    private static boolean passwordAuth = false;
    public static List<String> downloaded; // just to check if result is correct 

    public static void main(String[] args) throws UnknownHostException, IOException {
        TaxonomySearcher object = new TaxonomySearcher();

        downloaded = new ArrayList<String>();

        String host = args[0]; // = "localhost" or 127.0.0.1;
        int port = Integer.parseInt(args[1]);// = 60000;

        startConnection(host, port);

        // Get username
        System.out.print("Username Command:");
        String userName = in.nextLine();

        if (userName.contains("EXIT")) {
            boolean responseOK = sendExitCommand();
            if (responseOK)
                closeConnection();
        }
        else{
            userNameAuth = sendUserCommand(userName);
            if (userNameAuth) {
                // check password
                System.out.print("Password Command:");
                String password = in.nextLine();

                passwordAuth = sendPasswordCommand(password);
                if (passwordAuth) {
                    System.out.println("Authorized!");
                } else {
                    closeConnection();
                }
            } else {
                closeConnection();
            } 
        }
        

        if (userNameAuth && passwordAuth) {
            // 1. Get the list of files to be downloaded
            // 2. For each file, traverse the file structure and download the file
            String[] downloadFiles = getFiles("OBJ"); // name of files to be downloaded

            for (int i = 0; i < downloadFiles.length; i++) {
                // For each file run search function
                // findFile(downloadFiles[i]);
                String currFile = downloadFiles[i];
                String[] splitted = currFile.split("\\.");// splitted[0] = orange, splitted[1] = jpg
                String foldername = splitted[0]; // we will look for this folder
                findFile(foldername);    
            }

            //checkResult(downloadFiles);

            // close the program
            if (sendExitCommand())
                closeConnection();
        }
    }

    // check the output 
    public static void checkResult(String[] expected){
        // print the files expected to be downloaded 
        System.out.print("\nExpected: ");
        for(int i = 0; i<expected.length; i++){
            System.out.print(expected[i] + " ");
        }
        System.out.print("\nResult: ");
        // print downloaded list 
        for(int i = 0; i<downloaded.size(); i++){
            System.out.print(downloaded.get(i) + " ");
        }
        System.out.print("\n\n");
    }

    // if current directory contains only jpg py etc files it contains no folder ie
    // returns true
    // true: contains at least one folder
    // false: everything is a file, it contains no folder
    public static boolean containsFolder(String[] files) {
        boolean result = true;
        for (int i = 0; i < files.length; i++) {
            if (files[i].contains(".")) {
                // it is a file
                result = false;
            } else {
                // it is a folder, there is at least one folder
                return true;
            }
        }
        return result;
    }

    public static void findFile(String folderName) {
        // orange.jpg is in folder orange, find the folder with folderName and call GET
        // from the server
        boolean found = false;
        List<String> visitedFolders = new ArrayList<String>();
        
        while (found != true) {
            String[] cwdFiles = getFiles("NLST"); // files and folders under current working directory

            // check this level 
            for (int i = 0; i < cwdFiles.length; i++) {
                if (folderName.equals(cwdFiles[i])) {
                    // found the file
                    found = true;
                } 
            }

            if(found == false){
                if(containsFolder(cwdFiles)){
                    if(!allVisited(cwdFiles, visitedFolders)){
                        //there are folders to process
                        for (int i = 0; i < cwdFiles.length; i++) {
                            String curr = cwdFiles[i];
                            if(!curr.contains(".") && !visitedFolders.contains(curr)){
                                // it is a folder and it is not visited. Change dir 
                                visitedFolders.add(curr);
                                sendCWDRCommand(curr);
                                break;
                            }
                            //else it is a file, pass
                        }
                    }
                    else{
                        // all of them are visited, go up in the directory
                        sendCDUPCommand();
                    }
                }
                else{
                    //only files left, go up in directory.
                    sendCDUPCommand();
                }
            }
            else{
                // found: true
                //sendGetCommand(folderName);
                //System.out.println("\n\n FOUND " + folderName + "\n\n");
                sendCWDRCommand(folderName);
                downloaded.add(folderName + ".jpg");
                sendGetCommand(folderName + ".jpg");
                sendCDUPCommand();
            }
        }
    }

    // return true if all visited
    public static boolean allVisited(String[]children, List<String> visitedFolders){
        boolean result = true;
        for(int i = 0; i<children.length; i++){
            if(!children[i].contains(".")){
                // it is not a file, it is a folder
                if(!visitedFolders.contains(children[i])){
                    // not visited 
                    return false;
                }
            }
        }
        return result; 
    }

    /*
     * Get the list of files for following commands: OBJ: the list of files required
     * to be downloaded by the client. NLST: the list of all files and folders under
     * the current working directory of the server.
     */
    public static String[] getFiles(String command) {
        System.out.println("Sending: " + command);
        writer.println(command); // Obtains the list of files required to be downloaded by the client.
        String[] response = null;
        String[] listOfFiles = null;
        try {
            String responseFromServer = fromServer.readLine();
            System.out.println(responseFromServer);
            response = responseFromServer.split(" ");
            if (response[0].equals("OK")) {
                listOfFiles = Arrays.copyOfRange(response, 1, response.length);
            }
        } catch (IOException exc) {
            System.out.println("Exception:" + exc.getMessage());
        }
        return listOfFiles;
    }

    // Establish a connection between client and server
    public static void startConnection(String host, int port) throws UnknownHostException, IOException {
        // Create a socket
        clientSocket = new Socket(host, port);
        System.out.println("Client socket created " + host + " " + port);

        // Initialize objects
        inputStream = clientSocket.getInputStream(); // Returns an input stream for this socket.
        inputStreamReader = new InputStreamReader(inputStream);
        fromServer = new BufferedReader(inputStreamReader);
        writer = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new Scanner(System.in);
        out = new DataOutputStream(clientSocket.getOutputStream());

        System.out.println("Connection established.");
    }

    // Close the connection between client and server
    public static void closeConnection() throws UnknownHostException, IOException {
        try {
            inputStream.close();
            inputStreamReader.close();
            fromServer.close();
            writer.close();
            in.close();
            clientSocket.close();
        } catch (IOException exception) {
            System.out.println("Exception while exiting: " + exception.getMessage());
        }
        System.out.println("Connection is closed.");
        System.exit(0);
    }

    public static void sendGetCommand(String filename) {
        System.out.println("Sending: " + "GET " + filename);
        writer.println("GET " + filename);
        String binarySize = "";
        try {
            // String responseFromServer = fromServer.readLine();
            // System.out.println(responseFromServer);
            byte[] response = new byte[7]; // <ISND><--->
            byte[] dataSize = new byte[3];
            inputStream.read(response, 0, 7); // I S N D x x x 
            for(int i = 0; i<4; i++){
                //print response message 
                System.out.print(Character.toString((char)response[i]));
            }
            System.out.print("\n");
            for(int i = 4; i<response.length; i++){
                dataSize[i-4] = response[i];
                String bin = String.format("%8s",Integer.toBinaryString(dataSize[i-4] & 0xFF)).replace(' ', '0');;
                binarySize = binarySize + bin;
            }
            
            int size = Integer.parseInt(binarySize,2);
            //System.out.println("Size: " + size);
            
            // read the rest of the image 
            byte[] image = new byte[size]; // <ISND><--->
            int bytesRead = inputStream.read(image, 0, size); 
            //System.out.println("Number of bytes read: " + bytesRead);

            while(bytesRead < size){
                // means we are not done
                bytesRead += inputStream.read(image, 0, size-bytesRead); 
                //System.out.println("Number of bytes read: " + bytesRead);
            }

        } catch (IOException exc) {
            System.out.println("Exception:" + exc.getMessage());
        }
    }

    public static boolean sendCWDRCommand(String child) {
        System.out.println("Sending: " + "CWDR " + child);
        writer.println("CWDR " + child);
        return evaluateResponse();
    }

    public static boolean sendCDUPCommand() {
        System.out.println("Sending: " + "CDUP");
        writer.println("CDUP");
        return evaluateResponse();
    }

    // Send command: USER <username>
    public static boolean sendUserCommand(String username) {
        System.out.println("Sending: " + "USER " + username);
        writer.println("USER " + username);
        return evaluateResponse();
    }

    // Send command: PASS <password>
    public static boolean sendPasswordCommand(String pass) {
        System.out.println("Sending: " + "PASS " + pass);
        writer.println("PASS " + pass);
        return evaluateResponse();
    }

    // Send command: EXIT
    public static boolean sendExitCommand() {
        System.out.println("Sending: " + "EXIT");
        writer.println("EXIT");
        return evaluateResponse();
    }

    // Evaluate and display the response of the server. Returns true if "OK", false
    // if "INVALID"
    public static boolean evaluateResponse() {
        String[] status = { "OK", "INVALID" };
        try {
            String responseFromServer = fromServer.readLine();
            System.out.println(responseFromServer);
            if (responseFromServer.contains(status[0]) && !responseFromServer.contains(status[1]))
                return true;
        } catch (IOException exc) {
            System.out.println("Exception:" + exc.getMessage());
        }
        return false;
    }
}