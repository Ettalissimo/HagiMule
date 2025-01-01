package Client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

public class Downloader {

    private static final String DOWNLOAD_DIR = "downloads/";

    public Downloader() {
        new File(DOWNLOAD_DIR).mkdirs(); 
    }

    public void demarrerTelechargement(String fichier, List<String> sources , int totalFragments) {
        System.out.println("Telechargement du fichier : " + fichier);
        byte[][] fragments = new byte[totalFragments][]; // stockage dial les fragments recus

        // Telecharger chaque fragment depuis les sources
        for (int i = 0; i < totalFragments; i++) {
            int fragmentId = i; 
            String source = sources.get(i % sources.size()); // Choisir une source pour ce fragment
            new Thread(() -> telechargerFragment(source, fragmentId, fragments)).start();
        }

        // Assembler le fichier film
        try {
            Thread.sleep(3000); // Simuler une attente
            assemblerFichier(fichier, fragments);
        } catch (InterruptedException e) {
            System.err.println("Erreur lors de l'attente pour l'assemblage : " + e.getMessage());
        }

    }

    private void telechargerFragment(String source, int fragmentId, byte[][] fragments) {
        try {

            String[] parts = source.split(":");  // he  re is the problem
            // String host = parts[0];
            String host = "localhost"; 
            int port = Integer.parseInt(parts[1]);

            try (Socket socket = new Socket(host, port);
                 InputStream input = socket.getInputStream()) {
                
                // Envoyer l'identifiant du fragment au Daemon
                socket.getOutputStream().write(fragmentId);

                byte[] buffer = new byte[1024];
                int bytesRead = input.read(buffer);
                if (bytesRead > 0) {
                    fragments[fragmentId] = new byte[bytesRead];
                    System.arraycopy(buffer, 0, fragments[fragmentId], 0, bytesRead);
                    System.out.println("Fragment " + fragmentId + " reçu de " + source);
                }
                // System.out.println("Fragment recu de " + source + " : " + new String(buffer, 0, bytesRead));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du telechargement du fragment " + fragmentId +" depuis " + source + " : " + e.getMessage());
        }
    }

    private void assemblerFichier(String fichier, byte[][] fragments) {
        try (FileOutputStream fos = new FileOutputStream(DOWNLOAD_DIR + fichier)) {
            for (byte[] fragment : fragments) {
                if (fragment != null) {
                    fos.write(fragment); // Écrire chaque fragment dans le fichier final
                }
            }
            System.out.println("Fichier " + fichier + " assemblé avec succès !");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'assemblage du fichier : " + e.getMessage());
        }
    }
}
