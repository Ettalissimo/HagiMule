package Client;

import java.io.File;
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
    private String[] mesFilms; 

    public Client(String id, int portDaemon) {
        this.id = id +":"+ portDaemon;
        this.fichiersPossedes = new ArrayList<>();
        this.daemon = new Daemon(portDaemon);
        this.downloader = new Downloader();
        // Automatiser les noms des films  
        this.mesFilms = new String[100];
        File filmDir = new File("./MesFilms");
        File[] files = filmDir.listFiles();
        int i = 0;
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    System.out.println(file.getName());
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
            int totalFragments = sources.size();
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
        String choix = args[2]; // deamon == 0 & downloader == 1
        


        try {
            Annuaire annuaire = (Annuaire) Naming.lookup("//localhost:9008/Annuaire");
            Client client = new Client(clientId, daemonPort);

            if (choix.equals("0")) {
                for (String film : client.mesFilms){
                    client.ajouterFichier(film);
                }
                client.mettreAJourFichiers(annuaire);
                
                new Thread(client::lancerDaemon).start();
                
            } else {
                String film = args[3];
                client.telechargerFichier(film, annuaire);
            }
            
            

        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du client : " + e.getMessage());
        }
    }

    // Tester

    /*
     * cd src
       javac Annuaire/*.java Client/*.java

       rm Annuaire/*.class Client/*.class

       java Annuaire.AnnuaireImpl

       java Client.Client oussama 8080 0      (Daemon)
       java Client.Client hlima 8081 0      (Daemon)

       java Client.Client abdellatif 8082 1 film.bits  (Downloader)

     * 
     */
}
