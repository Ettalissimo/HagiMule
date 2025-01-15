package Util;

import java.util.Scanner;

public class DownloadPerformanceTest {

    // Mesure le temps d'exécution d'une commande
    public static long measureDownloadTime(String command) {
        long startTime = System.currentTimeMillis();
        try {
            Process process = Runtime.getRuntime().exec(command);

            // Lire la sortie pour éviter les blocages
            process.waitFor(); // Attendre la fin de la commande
        } catch (Exception e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis() - startTime;
    }

    // Calcule la vitesse de téléchargement
    public static double calculateSpeed(long downloadTime, long fileSizeMB) {
        // Temps en secondes
        double timeInSeconds = downloadTime / 1000.0;
        // Vitesse en MB/s
        return fileSizeMB / timeInSeconds;
    }

    // Calcule le facteur d'accélération
    public static double calculateAcceleration(long singleTime, long parallelTime) {
        return (double) singleTime / parallelTime;
    }

    public static void main(String[] args) {
        // Configuration : taille du fichier (en MB)
        long fileSizeMB = (long) 62; // Taille du fichier à télécharger
        String command = "java Client.Client 172.22.232.81 8080 1 BigBuckBunny.mp4";

        // Étape 1 : Mesurer le temps pour un téléchargement simple
        System.out.println("Test téléchargement simple...");
        long singleDownloadTime = measureDownloadTime(command);
        double singleSpeed = calculateSpeed(singleDownloadTime, fileSizeMB);
        System.out.println("Temps téléchargement simple : " + singleDownloadTime + " ms");
        System.out.println("Vitesse simple : " + String.format("%.2f", singleSpeed) + " MB/s");

        // Attente de l'utilisateur avant de passer au téléchargement parallèle
        System.out.println("\nAppuyez sur Entrée pour continuer avec le test de téléchargement parallèle...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine(); // Attendre une entrée utilisateur

        // Étape 2 : Mesurer le temps pour un téléchargement parallèle
        System.out.println("\nTest téléchargement parallèle...");
        long parallelDownloadTime = measureDownloadTime(command); // Configurez manuellement le mode parallèle
        double parallelSpeed = calculateSpeed(parallelDownloadTime, fileSizeMB);
        System.out.println("Temps téléchargement parallèle : " + parallelDownloadTime + " ms");
        System.out.println("Vitesse parallèle : " + String.format("%.2f", parallelSpeed) + " MB/s");

        // Étape 3 : Calculer le facteur d'accélération
        double accelerationFactor = calculateAcceleration(singleDownloadTime, parallelDownloadTime);
        System.out.println("\nFacteur d'accélération : " + String.format("%.2f", accelerationFactor));
    }
}
