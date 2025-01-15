package Client;

import java.io.File;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import Annuaire.Annuaire;

public class Client {
    private String id;
    private List<String> fichiersPossedes;
    private Daemon daemon;
    private Downloader downloader;
    private String[] mesFilms;
    private Annuaire annuaire;

    private boolean actif;

    public Client(String id, int portDaemon) {
        this.id = id + ":" + portDaemon;
        this.fichiersPossedes = new ArrayList<>();
        this.daemon = new Daemon(portDaemon);
        this.downloader = new Downloader();

        // heartbeat mechanisme
        this.actif = true;

        // Automatiser les noms des films
        this.mesFilms = new String[100];
        File filmDir = new File("./MesFilms");
        File[] files = filmDir.listFiles();
        int i = 0;
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    // System.out.println(file.getName());
                    this.mesFilms[i] = file.getName();
                    i++;
                }

            }
        }
    }

    public void lancerDaemon() {
        daemon.ouvrirConnexion();
    }

    public void mettreAJourFichiers(Annuaire annuaire) {
        for (String fichier : fichiersPossedes) {
            try {
                annuaire.enregistrerFichier(id, fichier);
            } catch (RemoteException e) {
                System.err.println("Erreur lors de l'enregistrement du fichier : " + fichier);
            }
        }
    }

    // Amelioration: Prendre en compte l'arrivée de nouveaux clients
    public void reconsulterAnnuairePeriodiquement() {
        new Thread(() -> {
            while (actif) {
                try {
                    Map<String, List<String>> fichiersEtSources = annuaire.obtenirFichiersEtSources();
                    // System.out.println("Fichiers disponibles dans l'annuaire : " +
                    // fichiersEtSources);
                    Thread.sleep(15000); // Reconsulter toutes les 15 secondes
                } catch (Exception e) {
                    System.err.println("Erreur lors de la consultation de l'annuaire : " + e.getMessage());
                }
            }
        }).start();
    }

    public void telechargerFichier(String fichier, Annuaire annuaire) {
        try {
            List<String> sources = annuaire.rechercherSources(fichier);
            System.out.println("les sources disponible : " + sources);
            if (sources.isEmpty()) {
                System.out.println("Aucune source disponible pour le fichier : " + fichier);
                return;
            }

            // on peut implemanter un code qui optimise ce nombre de fragments
            // proposition simple : nbr fragment == nbr sources disponibles
            int totalFragments = sources.size();
            downloader.demarrerTelechargement(fichier, sources, totalFragments);
        } catch (RemoteException e) {
            System.err.println("Erreur lors de la recherche ou du téléchargement : " + e.getMessage());
        }
    }

    public void ajouterFichier(String fichier) {
        if (fichier != null)
            fichiersPossedes.add(fichier);
    }

    // Envoi des heartbeats pour indiquer l'activité
    private void envoyerHeartbeats() {
        while (actif) {
            try {
                annuaire.envoyerHeartbeat(id);
                Thread.sleep(10000); // Envoi toutes les 10 secondes
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi du heartbeat : " + e.getMessage());
            }
        }
    }

    public static boolean isValidCommand(String command, String[] validCommands) {
        for (String validCommand : validCommands) {
            if (validCommand.equals(command)) {
                return true;
            }
        }
        return false;
    }

    // Main
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage : java client.Client <diaryName> <daemonPort>");
            return;
        }

        try {
            String clientId = InetAddress.getLocalHost().getHostName();
            String diaryName = args[0];
            int daemonPort = Integer.parseInt(args[1]);
            Annuaire annuaire = (Annuaire) Naming.lookup("//" + diaryName + ":9008/Annuaire");
            // Annuaire annuaire = (Annuaire) Naming.lookup("//localhost:9008/Annuaire");
            Client client = new Client(clientId, daemonPort);
            client.annuaire = annuaire; // Initialisation de l'annuaire

            for (String film : client.mesFilms) {
                client.ajouterFichier(film);
            }
            client.mettreAJourFichiers(annuaire);
            new Thread(client::lancerDaemon).start();
            new Thread(client::envoyerHeartbeats).start();
            client.reconsulterAnnuairePeriodiquement();

            // Affichage Interface Ligne de Commande
            String[] validCommands = { "afficherfilms", "telecharger" };
            String input;
            Scanner scanner = new Scanner(System.in);
            System.out.println("Entrer une commande parmi : afficherfilms et telecharger <nomfilm>");
            while (true) {
                System.out.print("\nEnter a command: ");
                input = scanner.nextLine().trim();

                String[] parts = input.split(" ", 2);
                String command = parts[0].toLowerCase();
                String argument = (parts.length > 1) ? parts[1].trim() : null;

                if (isValidCommand(command, validCommands)) {
                    // Handle valid commands
                    switch (command) {
                        case "afficherfilms":
                            List<String> films = annuaire.getFichiers();
                            System.out.println("La liste des films disponibles :");
                            for (String film : films) {
                                System.out.println("- " + film);
                            }
                            break;
                        case "telecharger":
                            if (argument != null) {
                                String film = argument;
                                client.telechargerFichier(film, annuaire);
                            } else {
                                System.out.println("Commande incomplète :\nUsage : telecharger <nomfilm>");
                            }
                            break;
                    }
                } else {
                    // Invalid command
                    System.out.println(
                            "Comande non valide. Veuillez entrer l'une des commandes suivantes: \n - afficherfilms\n - telecharger <nomfilm> ");
                }
            }

            // Mode téléchargement : Télécharger un fichier spécifique

            // will be uncommented when we test from different terminals
            /*
             * for (String fichier : client.mesFilms){
             * client.ajouterFichier(fichier);
             * }
             * client.mettreAJourFichiers(annuaire);
             */

        } catch (

        Exception e) {
            System.err.println("Erreur lors du démarrage du client : " + e.getMessage());
        }

    }

    // Tester

    /*
     * cd src
     * javac Annuaire/*.java Client/*.java
     * 
     * rm Annuaire/*.class Client/*.class
     * 
     * java Annuaire.AnnuaireImpl
     * 
     * java Client.Client oussama 8080 0 (Daemon)
     * java Client.Client hlima 8081 0 (Daemon)
     * 
     * java Client.Client abdellatif 8082 1 film.bits (Downloader)
     * 
     * 
     */
}
