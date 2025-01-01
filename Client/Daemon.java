package Client;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Daemon {
    private int port;
    private boolean actif;
    private String filePath;
    private static int fragmentSize = 8; 


    // just for testing
    public Daemon(int port) {
        this.port = port;
        this.actif = false;
        this.filePath = "MesFilms/film.bits"; 
    }

    public Daemon(int port,String filePath) {
        this.port = port;
        this.actif = false;
        this.filePath = filePath;
    }

    public void ouvrirConnexion() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            actif = true;
            System.out.println("Daemon en écoute sur le port : " + port);

            while (actif) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> gererRequete(clientSocket)).start();
            }
        } catch (Exception e) {
            System.err.println("Erreur dans le Daemon : " + e.getMessage());
        }
    }


    private void gererRequete(Socket clientSocket) {
        try (OutputStream output = clientSocket.getOutputStream();
             FileInputStream fis = new FileInputStream(new File(filePath))) {

                // Lire fragmentId depuis le Downloader
                int fragmentId = clientSocket.getInputStream().read();
                if (fragmentId == -1) {
                    System.err.println("Aucun fragmentId reçu.");
                    return;
                }

                // Calculer offset dans le fichier et envoyer le fragment
                long offset = fragmentId * (long) fragmentSize;
                byte[] buffer = new byte[fragmentSize];
                fis.skip(offset);
                int bytesRead = fis.read(buffer);
                if (bytesRead > 0) {
                    output.write(buffer, 0, bytesRead);
                    System.out.println("Fragment " + fragmentId + " envoyé.");
                } else {
                    System.out.println("Fragment " + fragmentId + " non disponible (hors limites).");
                }

                /* 
                // Test de connexion sockets
                String message = "Fragment envoye.\n";
                output.write(message.getBytes());
                System.out.println("Fragment envoye  : " + clientSocket.getInetAddress());
                */
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du fragment : " + e.getMessage());
        }
    }
}
