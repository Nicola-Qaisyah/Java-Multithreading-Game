import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient {
    
    Socket socket;               // Socket to connect to server
    String nickname;             // Player's nickname
    DataInputStream din;         // To read primitive data types from server (e.g., int)
    DataOutputStream dout;       // To send primitive data types to server (e.g., int)
    BufferedReader in;           // To read text lines from server
    PrintWriter pout;            // To send text lines to server
    String secretName;           // The secret name for the challenge (set by challenger)
    String Category;             // Category of the challenge
    int clientType;              // 1 for challenger, 2 for guesser
    
    public GameClient(){
        try{
            // Connect to server on localhost (loopback) at port 3000
            socket = new Socket(InetAddress.getLoopbackAddress(),3000);
            System.out.println("Connected to the Server");

            // Initialize input/output streams for communication
            din = new DataInputStream(socket.getInputStream());
            dout = new DataOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pout = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); // auto flush enabled
            
            Scanner input = new Scanner(System.in);
            System.out.println("Please enter your nickname:");
            nickname = input.nextLine();         // Read user's nickname from console
            pout.println(nickname);               // Send nickname to server
        }
        catch(IOException i){
            System.out.println("Could not connect " + i);
            return;
        }
    }

    public static void main(String [] args){
       
        GameClient client = new GameClient();    // Create new client instance and connect to server
        Scanner input = new Scanner(System.in);
        
        System.out.println("1.Create Challenge\n2.Join Challenge");
        int choice = input.nextInt();
        input.nextLine(); // consume newline
        
        switch(choice){
            case 1:
                client.pout.println("Create");       // Inform server that this client will create a challenge
                client.clientType = 1;                // Mark client type as challenger
                
                // Ask user for challenge category
                System.out.println("Which category you want?\n1.Movies\n2.Actors Actresses\n3.Famous Athletes / Sports Figures\n4.Scientists / Inventors\n5.Historical Figures\n6.Famous Books / Novels\n7.TV Series / Shows\n8.Countries or Cities ");
                client.Category = input.nextLine();  // Read category choice
                
                System.out.println("What is the secret name?");
                client.secretName = input.nextLine(); // Read the secret name
                
                // Send category and secret name to server
                client.pout.println(client.Category);
                client.pout.println(client.secretName);
                
                System.out.println("what is the maximum number of attempts?(0 for unlimited numbers)");
                try{
                    client.dout.writeInt(input.nextInt());   // Send max attempts to server as int
                    input.nextLine();
                }catch(IOException io){
                    System.out.println(io);
                }
                break;
                
            case 2:
                client.pout.println("Join");         // Inform server that this client wants to join a challenge
                client.clientType = 2;                // Mark client type as guesser
                try{
                    System.out.println("Fetching open challenges...");
                    String challenge;
                    
                    // Read and display open challenges from server until "END" is received
                    while (!(challenge = client.in.readLine()).equals("END")) {
                        System.out.println(challenge);
                        if(challenge.equals("Sorry, no Available Challenges"))
                            return;   //terminate the program
                    }
                    System.out.print("Enter the challenge number to join: ");
                    client.dout.writeInt(input.nextInt());    // Send the selected challenge number
                    input.nextLine();
                    System.out.println(client.in.readLine()); // Server responds with confirmation e.g. "Joined"
                    break; 
                }
                catch(IOException i){
                    System.out.println(i);
                }
        }
        
        try{
            System.out.println("Waiting for challenge to start...");
            String serverMessage;
            
            // Wait for the server to send "Challenge Started" signal
            while (!(serverMessage = client.in.readLine()).equals("Challenge Started")) {
            }
            System.out.println("Challenge Started");
           
            if(client.clientType == 1){
                // Challenger's part
                System.out.println("Challenger");
                
                String question = client.in.readLine();  // Read questions asked by guessers
                
                // Loop until server sends "End"
                while(!question.equals("End")){
                    System.out.println("The Question is: " + question);
                    System.out.println("What is your answer?(Yes/No/I don't Know)");
                    
                    // Send answer back to server
                    client.pout.println(input.nextLine());
                    
                    // Read next question or "End"
                    question = client.in.readLine();
                }
            }
            else{
                // Guesser's part
                boolean playing = true;
                
                while (playing) {
                    serverMessage = client.in.readLine();
                    if (serverMessage == null) break;
                    
                    if (serverMessage.equals("Your Turn")) {
                        System.out.println(serverMessage);   // Print "Your Turn"
                        System.out.println("Ask a Yes/No question: ");
                        
                        String question = input.nextLine();
                        client.pout.println(question);        // Send question to server
                        
                        System.out.println("Waiting for response...");
                        String response = client.in.readLine();  // Get answer to the question
                        System.out.println("Response: " + response);
                        
                        System.out.println("Do you want to guess? (Y/N):");
                        char guessChoice = input.nextLine().charAt(0);  // Read guess choice from user
                        client.pout.println(guessChoice);               // Send guess choice
                        
                        if (guessChoice == 'Y' || guessChoice == 'y') {
                            System.out.print("Enter your guess: ");
                            String guess = input.nextLine();
                            client.pout.println(guess);                   // Send guess to server
                            
                            String result = client.in.readLine();          // Read if guess was correct ("true"/"false")
                            if (result.equals("true")) {
                                System.out.println("You Won!");
                                playing = false;                            // End game if won
                            } else {
                                System.out.println("Incorrect guess");
                                int remainingAttempts = client.din.readInt();  // Read remaining attempts
                                if(remainingAttempts == 0){
                                    System.out.println("YOU LOST your attempts is 0");
                                    playing = false;                        // End game if no attempts left
                                }
                            }
                        }
                    } 
                    else if (serverMessage.equals("Opponent's Turn")) {
                        System.out.println("Waiting for opponent's question...");
                        System.out.println("Question: " + client.in.readLine());
                        System.out.println("Answer: " + client.in.readLine());
                        
                        char guessChoice = client.in.readLine().charAt(0);
                        System.out.println("Guess Choice for opponents: " + guessChoice);
                        
                        if (guessChoice == 'Y' || guessChoice == 'y') {
                            System.out.println("Opponent's Guess: " + client.in.readLine());
                            
                            String result = client.in.readLine();
                            if (result.equals("true")) {
                                System.out.println("You Lost");
                                playing = false;                // Game ends if opponent guessed correctly
                            } else {
                                System.out.println("Incorrect guess");
                                playing = client.din.readBoolean(); // Read if still playing or attempts left
                                if(!playing){
                                    System.out.println("YOU WON the Opponent's attempts is 0");
                                }
                            }
                        }
                    } 
                }

                System.out.println("Thanks for playing!");
                client.socket.close();   // Close socket connection when done
            }
        } 
        catch(IOException io){
            System.out.println(io);
        }
    }
}
