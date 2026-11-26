import java.io.*;
import java.net.*;
import java.util.ArrayList;

// Main server class
public class GameServer {
    public static void main(String args[]) {
        try {
            // Create a server socket on port 3000
            ServerSocket server = new ServerSocket(3000);
            System.out.println("Server Started");

            // Infinite loop to accept clients
            for (;;) {
                Socket nextClient = server.accept();
                ClientHandler c = new ClientHandler(nextClient);
                c.start(); // Each client runs on a separate thread
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

// Challenge class holds all challenge-related data
class Challenge {
    String nickNameChallenger;
    Socket challenger;
   // Socket player1;
   // Socket player2;
    boolean TurnsSwitched = false;
    boolean joinedPlayer1;
    boolean joinedPlayer2;
    String Category;
    String secretName;
    int maxAttempts;
    String question;
    String answer;
    String guess;
    char choice;
    boolean playing = false;
    boolean readCFinished = false;
    boolean readQFinished = false;
    boolean readAFinished = false;
    boolean readGFinished = false;
    Boolean playersTurn = false;
    boolean playingStatusChanged = false;

    // Shared list of available challenges
    static ArrayList<Challenge> AvailableChallenges = new ArrayList<>();

    // Constructor
    Challenge(String name, Socket ch, String cat, String secret, int attempts) {
        nickNameChallenger = name;
        maxAttempts = attempts;
        Category = cat;
        secretName = secret;
        challenger = ch;
    }
}

// Handles each connected client
class ClientHandler extends Thread {
    Socket nextClient;
    String nickname;
    BufferedReader in;
    DataInputStream din;
    DataOutputStream dout;
    PrintWriter pout;
    Challenge ch;
    int remainingAttempts;

    // Constructor initializes I/O streams
    ClientHandler(Socket next) {
        try {
            nextClient = next;
            din = new DataInputStream(nextClient.getInputStream());
            dout = new DataOutputStream(nextClient.getOutputStream());
            in = new BufferedReader(new InputStreamReader(nextClient.getInputStream()));
            pout = new PrintWriter(new OutputStreamWriter(nextClient.getOutputStream()), true); // Auto-flush

            nickname = in.readLine();
            System.out.println(nickname + " connected to the server");

        } catch (IOException io) {
            System.out.println(io);
        }
    }
    
    @Override
    public void run() {
        try {
            String choice = in.readLine(); // Create or Join

            if (choice.equals("Create")) {
                runAsChallenger(); // Challenger mode
            } else {
                // Send list of available challenges to the player
                String challenge = "";
                if(Challenge.AvailableChallenges.size()==0){    //Check if there is availabel challenges
                    pout.println("Sorry, no Available Challenges");
                }else{
                for (int i = 0; i < Challenge.AvailableChallenges.size(); i++) {
                    challenge = i + "- " + Challenge.AvailableChallenges.get(i).nickNameChallenger + "- attempst: " + Challenge.AvailableChallenges.get(i).maxAttempts;
                    pout.println(challenge);
                }
                pout.println("END");

                int chNum = din.readInt(); // Player selects challenge number
                ch = Challenge.AvailableChallenges.get(chNum);
                
                // Assign player to available slot
                if (ch.joinedPlayer1 == false) {
                    //ch.player1 = nextClient;
                    ch.joinedPlayer1 = true;
                    this.remainingAttempts = ch.maxAttempts;
                } else {
                    //ch.player2 = nextClient;
                    ch.joinedPlayer2 = true;
                    this.remainingAttempts = ch.maxAttempts;
                }

                pout.println("Joined!");

                // Wait until challenger starts the game
                while (!(ch.playing)) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }

                pout.println("Challenge Started");

                int turns = 0;

                synchronized (ch.playersTurn) {
                    if (!ch.playersTurn) {
                        turns++;
                        ch.playersTurn = true;
                    }
                }

                // Player turn loop
                while (ch.playing) {
                    if (turns == 1) {
                        turns = runAsPlayer1();
                    } else {
                        turns = runAsPlayer2();
                    }
                }
                
                }
            }

        } catch (IOException io) {
            System.out.println(io);
        }
    }

    // Challenger flow
    public void runAsChallenger() {
        try {
            ch = new Challenge(nickname, nextClient, in.readLine(), in.readLine(), din.readInt());
            System.out.println(nickname + " Created a challenge");
            Challenge.AvailableChallenges.add(ch);

            // Wait for both players to join
            while (!(ch.joinedPlayer1 && ch.joinedPlayer2)) {
                Thread.yield();
            }

            Challenge.AvailableChallenges.remove(ch);
            ch.playing = true;

            pout.println("Challenge Started");
            System.out.println(ch.nickNameChallenger + "'s challenge started");

            // Game loop
            while (ch.playing) {
                ch.readQFinished = false;

                while (!ch.readQFinished) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }

                pout.println(ch.question); // Send question to challenger
                ch.answer = in.readLine(); // Get answer
                ch.readAFinished = true;

                System.out.println("the answer is :" + ch.answer);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                
                while(ch.TurnsSwitched == false){
                    try{Thread.sleep(100);}
                    catch(InterruptedException o){}
                }
                    
                if(ch.playing == false) break;
            }
            System.out.println("Challenger code ended");
            pout.println("End");

        } catch (IOException io) {
            System.out.println(io);
        }
    }

    // Player 1's turn
    public int runAsPlayer1() {
        try{
            Thread.sleep(100);
        }catch(InterruptedException o ){}
        ch.TurnsSwitched = false;
        pout.println("Your Turn");

        try {
            ch.question = in.readLine(); // Player asks question
            ch.readQFinished = true;

            while (!ch.readAFinished) {
                Thread.sleep(100);
            }

            pout.println(ch.answer); // Show answer

            ch.choice = in.readLine().charAt(0); // Ask to guess
            ch.readCFinished = true;

            
            if (ch.choice == 'Y' || ch.choice == 'y') {
                if(this.remainingAttempts == 0)
                this.remainingAttempts--;
                ch.guess = in.readLine(); // Get guess
                ch.readGFinished = true;
                
                            try{
                Thread.sleep(100);
            }
            catch(InterruptedException e){
            }
                
                
                if (ch.guess.equals(ch.secretName)) {
                    pout.println("true");
                    ch.playing = false;
                                try {
                    Thread.sleep(100);
                } catch (InterruptedException i) {
                }
                } else {
                    pout.println("false");
                    dout.writeInt(this.remainingAttempts);
                    
                    if (this.remainingAttempts == 0)
                        ch.playing = false;
                    try {
                    Thread.sleep(100);
                    } catch (InterruptedException i) {}
                    ch.playingStatusChanged = true;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException i) {}
             }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        // Reset flags for next round
        ch.readCFinished = false;
        ch.readGFinished = false;
        ch.readQFinished = false;
        ch.readAFinished = false;
        ch.TurnsSwitched = true;
        return 0;
    }

    // Player 2's turn (reads data only)
    public int runAsPlayer2() {
        try{
            Thread.sleep(100);
        }catch(InterruptedException o ){}
        ch.TurnsSwitched = false;
        pout.println("Opponent's Turn");

        // Wait for question
        while (!ch.readQFinished) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException i) {
            }
        }

        pout.println(ch.question); // Show question

        // Wait for answer
        while (!ch.readAFinished) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException i) {
            }
        }

        pout.println(ch.answer); // Show answer

        // Wait for Player 1's guess decision
        while (!ch.readCFinished) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException i) {
            }
        }

        pout.println(ch.choice); // Show Y/N choice

        if (ch.choice == 'Y' || ch.choice == 'y') {
            while (!ch.readGFinished) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException i) {
                }
                
            }
             
            pout.println(ch.guess); // Show guess

            // Check result
            if (ch.guess.equals(ch.secretName)) {
                pout.println("true");
                ch.playing = false;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException i) {
                }
            } else {
                pout.println("false");

                while (!ch.playingStatusChanged) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }

                try {
                    dout.writeBoolean(ch.playing);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }

        // Reset flags
        ch.readCFinished = false;
        ch.readGFinished = false;
        ch.readQFinished = false;
        ch.readAFinished = false;
        ch.TurnsSwitched = true;
        return 1;
    }
}
