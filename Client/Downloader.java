package Client;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class Downloader {

    private static final String DOWNLOAD_DIR = "downloads/";
    private static final String TmpFiles_DIR = "TmpFiles/";

    public Downloader() {
        new File(DOWNLOAD_DIR).mkdirs(); 
        new File(TmpFiles_DIR).mkdirs();
    }

    public void demarrerTelechargement(String fichier, List<String> sources , int totalFragments) {
        System.out.println("Telechargement du fichier : " + fichier);
        byte[][] fragments = new byte[totalFragments][]; // stockage dial les fragments recus
        Thread[] threadsFragments = new Thread[totalFragments];

        // Telecharger chaque fragment depuis les sources
        for (int i = 0; i < totalFragments; i++) {
            int fragmentId = i; 
            String source = sources.get(i % sources.size()); // Choisir une source pour ce fragment
            threadsFragments[i] = new Thread(() -> telechargerFragment(source, fichier, fragmentId, fragments, totalFragments));
            threadsFragments[i].start();
        }

        for (int i =0; i<totalFragments;i++){
            try {
                threadsFragments[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("wach kat assembler");
        assemblerFichier(fichier, totalFragments);

    }

    private void telechargerFragment(String source, String filmName, int fragmentId, byte[][] fragments, int totalFragments) {
        try {

            String[] parts = source.split(":");  // oussama:port
            // String host = parts[0];
            String host = "localhost"; 
            int port = Integer.parseInt(parts[1]);

            try (Socket socket = new Socket(host, port);
                 DataInputStream input = new DataInputStream(socket.getInputStream());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 FileOutputStream fos = new FileOutputStream(TmpFiles_DIR +"Tmp_" + fragmentId + "_" + filmName)) {
                
                // Envoyer l'identifiant du fragment au Daemon

                

                socket.setSoTimeout(0); // Désactiver le timeout
                out.println(filmName +":"+ fragmentId+ ":"+ totalFragments);
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                //int bytesRead = input.read(buffer);
                
                long fragmentSize = input.readLong(); // Read file size
                System.out.println("fragment size :" + fragmentSize);
                int bytesRead; 
                long totalBytesRead = 0;
                while (totalBytesRead < fragmentSize    && (bytesRead = input.read(buffer)) != -1 ) {
                    fos.write(buffer, 0, (fragmentSize - totalBytesRead>bufferSize)?bytesRead:(int)(fragmentSize-totalBytesRead));
                    totalBytesRead += bytesRead;
                    System.out.println("total bytesread: "+ totalBytesRead);
                }
                System.out.println("wach katkhrj mnha ");

                //fragments[fragmentId] = new byte[bufferSize];
                //System.arraycopy(buffer, 0, fragments[fragmentId], 0, bufferSize);


                //while ((bytesRead = input.read(buffer)) !=  )

                /*if (bytesRead > 0) {
                    fragments[fragmentId] = new byte[bytesRead];
                    System.arraycopy(buffer, 0, fragments[fragmentId], 0, bytesRead);
                    System.out.println("Fragment " + fragmentId + " reçu de " + source);
                }*/
                // System.out.println("Fragment recu de " + source + " : " + new String(buffer, 0, bytesRead));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du telechargement du fragment " + fragmentId +" depuis " + source + " : " + e.getMessage());
        }
    }

    private void assemblerFichier(String fichier, int totalFragments) {
        FileInputStream fis = null ;
        try (FileOutputStream fos = new FileOutputStream(DOWNLOAD_DIR + fichier)) {
            File file = new File("downloads/" + fichier);
            int bytesRead;
            int bufferMax = 4096; 
            byte[] buffer = new byte[bufferMax];

            for (int fragmentId = 0; fragmentId < totalFragments; fragmentId++){
                fis = new FileInputStream(TmpFiles_DIR +"Tmp_" + fragmentId + "_" + fichier);
                System.out.println("test test ");
                while (((bytesRead = fis.read(buffer)) != -1 ) ) {
                    fos.write(buffer, 0, bytesRead);
                }
                System.out.println("after after ");
            }



            /*for (byte[] fragment : fragments) {
                if (fragment != null) {
                    fos.write(fragment); // Écrire chaque fragment dans le fichier final
                }
            }*/

            long fileSize = file.length();
            System.out.println("Total file size: "+ fileSize);
            System.out.println("Fichier " + fichier + " assemblé avec succès !");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'assemblage du fichier : " + e.getMessage());
        } finally{
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
