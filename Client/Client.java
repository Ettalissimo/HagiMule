package Client;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import Annuaire.Annuaire;

public class Client {
    private String id;
    private List<String> fichiersPossedes;

    private Daemon daemon;
    private Downloader downloader;
    private String film;

    public Client(String id, int portDaemon) {
        this.id = id +":"+ portDaemon;
        this.fichiersPossedes = new ArrayList<>();
        this.daemon = new Daemon(portDaemon);
        this.downloader = new Downloader();
    }

    public Client(String id, int portDaemon, String film) {
        this.id = id +":"+ portDaemon;
        this.fichiersPossedes = new ArrayList<>();
        this.daemon = new Daemon(portDaemon);
        this.downloader = new Downloader();
        this.film = film;
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

    public void telechargerFichier(String fichier, Annuaire annuaire) {
        try {
            List<String> sources = annuaire.rechercherSources(fichier);
            System.out.println("les sources disponible : "+ sources);
            if (sources.isEmpty()) {
                System.out.println("Aucune source disponible pour le fichier : " + fichier);
                return;
            }

            // on peut implemanter un code qui optimise ce nombre de fragments 
            // proposition simple : nbr fragment == nbr sources disponibles 
            int totalFragments = 2;
            downloader.demarrerTelechargement(fichier, sources, totalFragments);
        } catch (RemoteException e) {
            System.err.println("Erreur lors de la recherche ou du téléchargement : " + e.getMessage());
        }
    }


    
    public void ajouterFichier(String fichier) {
        fichiersPossedes.add(fichier);
    }

    // Main
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage : java client.Client <clientId> <daemonPort>");
            return;
        }

        String clientId = args[0];
        int daemonPort = Integer.parseInt(args[1]);
        String choix = args[2];
        


        try {
            Annuaire annuaire = (Annuaire) Naming.lookup("//localhost:9008/Annuaire");

            if (choix.equals("0")) {
                Client client = new Client(clientId, daemonPort);
                //client.ajouterFichier("file1.txt");
                //client.ajouterFichier("file2.txt");
                client.ajouterFichier("film.bits");

                client.mettreAJourFichiers(annuaire);
                new Thread(client::lancerDaemon).start();
                
            } else {
                String film = args[3];
                Client client = new Client(clientId, daemonPort, film);
                //client.ajouterFichier("file3.txt");
                //client.ajouterFichier("file4.txt");
                //client.mettreAJourFichiers(annuaire);

                client.telechargerFichier(film, annuaire);
            }
            
            

        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du client : " + e.getMessage());
        }
    }

    // Tester

    /*
     * cd src
       javac annuaire/*.java client/*.java

       rm Annuaire/*.class Client/*.class

       java Annuaire.AnnuaireImpl

       java Client.Client oussama 8080 0      (Daemon)
       java Client.Client hlima 8081 0      (Daemon)

       java Client.Client abdellatif 8082 1 film.bits  (Downloader)

     * 
     */
}
