package Client;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import java.util.concurrent.ConcurrentHashMap;

public class Downloader {
    private static final String DOWNLOAD_DIR = "downloads/";
    private static final String TmpFiles_DIR = "TmpFiles/";

    private ConcurrentHashMap<Integer, String> fragmentStatus; // État des fragments

    public Downloader() {
        new File(DOWNLOAD_DIR).mkdirs();
        new File(TmpFiles_DIR).mkdirs();
        fragmentStatus = new ConcurrentHashMap<>();
    }

    public void demarrerTelechargement(String fichier, List<String> sources, int totalFragments) {
        System.out.println("Telechargement du fichier : " + fichier);
        byte[][] fragments = new byte[totalFragments][];
        Thread[] threadsFragments = new Thread[totalFragments];

        for (int i = 0; i < totalFragments; i++) {
            fragmentStatus.put(i, "en cours");
            int fragmentId = i;
            String source = sources.get(i % sources.size());

            threadsFragments[i] = new Thread(() -> {
                boolean success = telechargerFragment(source, fichier, fragmentId, fragments);
                if (!success) {
                    fragmentStatus.put(fragmentId, "non téléchargé");
                } else {
                    fragmentStatus.put(fragmentId, "téléchargé");
                }
            });

            threadsFragments[i].start();
        }

        for (Thread thread : threadsFragments) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Relancer les fragments non téléchargés
        relancerFragmentsNonTelecharges(fichier, sources, fragments, totalFragments);

        assemblerFichier(fichier, totalFragments);
    }

    private boolean telechargerFragment(String source, String fichier, int fragmentId, byte[][] fragments) {
        try {
            String[] parts = source.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            try (Socket socket = new Socket(host, port);
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    FileOutputStream fos = new FileOutputStream(TmpFiles_DIR + "Tmp_" + fragmentId + "_" + fichier)) {

                socket.setSoTimeout(0);
                out.println(fichier + ":" + fragmentId + ":" + fragments.length);
                long fragmentSize = input.readLong();
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fragmentSize && (bytesRead = input.read(buffer)) != -1) {
                    fos.write(buffer, 0, (int) Math.min(fragmentSize - totalBytesRead, bytesRead));
                    totalBytesRead += bytesRead;
                }

                return totalBytesRead == fragmentSize;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du telechargement du fragment " + fragmentId + " depuis " + source + " : "
                    + e.getMessage());
            return false;
        }
    }

    private void relancerFragmentsNonTelecharges(String fichier, List<String> sources, byte[][] fragments,
            int totalFragments) {
        for (int fragmentId : fragmentStatus.keySet()) {
            if ("non téléchargé".equals(fragmentStatus.get(fragmentId))) {
                for (String source : sources) {
                    System.out.println(
                            "Relance du téléchargement du fragment " + fragmentId + " depuis la source " + source);
                    boolean success = telechargerFragment(source, fichier, fragmentId, fragments);
                    if (success) {
                        fragmentStatus.put(fragmentId, "téléchargé");
                        break;
                    }
                }
            }
        }
    }

    private void assemblerFichier(String fichier, int totalFragments) {
        try (FileOutputStream fos = new FileOutputStream(DOWNLOAD_DIR + fichier)) {
            for (int fragmentId = 0; fragmentId < totalFragments; fragmentId++) {
                try (FileInputStream fis = new FileInputStream(TmpFiles_DIR + "Tmp_" + fragmentId + "_" + fichier)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
            }
            System.out.println("Fichier " + fichier + " assemblé avec succès !");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'assemblage du fichier : " + e.getMessage());
        }
    }
}
