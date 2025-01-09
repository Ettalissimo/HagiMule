package Client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Daemon {
    private int port;
    private boolean actif;


    public Daemon(int port) {
        this.port = port;
        this.actif = false;
    }


    public void ouvrirConnexion() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            actif = true;
            System.out.println("Daemon en écoute sur le port : " + port);

            while (actif) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(0);
                new Thread(() -> gererRequete(clientSocket)).start();
            }
        } catch (Exception e) {
            System.err.println("Erreur dans le Daemon : " + e.getMessage());
        }
    }


    private void gererRequete(Socket clientSocket) {
        FileInputStream fis = null ;
        try {
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String[] sentData = in.readLine().split(":");
                String filmName = sentData[0];

                int fragmentId = Integer.parseInt(sentData[1]);
                File file = new File("MesFilms/" + filmName);
                long fileSize = file.length();
                System.out.println(fileSize);
                fis = new FileInputStream(file);

                int totalFragments = Integer.parseInt(sentData[2]);
                int fragmentSize = (int) (fileSize / totalFragments);
                long offset = fragmentId * (long) fragmentSize;

                if (fragmentId == -1) {
                    System.err.println("Aucun fragmentId reçu.");
                    return;
                } else if (fragmentId == totalFragments - 1){
                    int chyata = (int)fileSize - fragmentSize * totalFragments;
                    fragmentSize = fragmentSize + chyata ;
                }
                


                // Calculer offset dans le fichier et envoyer le fragment
                int bufferMax = 4096; 
                byte[] buffer = new byte[bufferMax];
                fis.skip(offset);
                int bytesRead;

                dos.writeLong(fragmentSize);
                System.out.println("fILE SIZE "+fragmentSize);
                int bytesSent = 0;

                while ( (bytesSent < fragmentSize) && ((bytesRead = fis.read(buffer)) != -1 )  ) {
                    dos.write(buffer, 0, (fragmentSize - bytesSent > bufferMax)?bytesRead:(fragmentSize - bytesSent));
                    bytesSent += bytesRead;
                }

                 
                System.out.println("Fragment " + fragmentId + " envoyé.");
            

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du fragment : " + e.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Handle any exceptions that occur during closing
                }
            }
        }
    }
}
