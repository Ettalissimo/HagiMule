package Annuaire;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AnnuaireImpl extends UnicastRemoteObject implements Annuaire {

    private Map<String, List<String>> fichiers; // { nameSource:port , listes des films}
    private Map<String, Long> heartbeats; // { clientId, dernier timestamp }
    private Map<String, Integer> chargeClients = new ConcurrentHashMap<>();

    private static final long TIMEOUT = 30000; // 30 secondes

    public AnnuaireImpl() throws RemoteException {
        super();
        this.fichiers = new HashMap<>();
        this.heartbeats = new ConcurrentHashMap<>();

        // Lancer un thread pour surveiller les clients inactifs
        new Thread(this::verifierClientsInactifs).start();
    }

    // Mettre à jour la charge d'un client (+1 ou -1)
    public synchronized void majChargeClient(String clientId, int variation) throws RemoteException {
        chargeClients.merge(clientId, variation, Integer::sum);
    }

    @Override
    public synchronized void enregistrerFichier(String clientId, String fichier) throws RemoteException {
        fichiers.putIfAbsent(fichier, new ArrayList<>());
        if (!fichiers.get(fichier).contains(clientId)) {
            fichiers.get(fichier).add(clientId);
        }
    }

    @Override
    public synchronized void retirerClient(String clientId) throws RemoteException {
        fichiers.values().forEach(clients -> clients.remove(clientId));
        System.out.println("Client retire : " + clientId);
    }

    @Override
    public List<String> rechercherSources(String fichier) throws RemoteException {
        return fichiers.getOrDefault(fichier, Collections.emptyList());
    }

    // Methode pour recevoir un heartbeat
    public void envoyerHeartbeat(String clientId) throws RemoteException {
        heartbeats.put(clientId, System.currentTimeMillis());
    }

    // Vérifie les clients inactifs
    private void verifierClientsInactifs() {
        while (true) {
            try {
                Thread.sleep(10000); // Vérification toutes les 10 secondes
                long maintenant = System.currentTimeMillis();

                List<String> clientsInactifs = new ArrayList<>();
                synchronized (heartbeats) {
                    // Identifier les clients inactifs
                    heartbeats.forEach((clientId, lastHeartbeat) -> {
                        if (maintenant - lastHeartbeat > TIMEOUT) {
                            clientsInactifs.add(clientId);
                        }
                    });

                    // Retirer les clients inactifs et afficher un message unique
                    for (String clientId : clientsInactifs) {
                        try {
                            retirerClient(clientId); // Appelle la méthode pour retirer le client
                            heartbeats.remove(clientId); // Mise à jour du hashmap
                            System.out.println("Client retiré pour inactivité : " + clientId);
                        } catch (RemoteException e) {
                            System.err.println("Erreur lors du retrait du client inactif : " + clientId);
                        }
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("Erreur dans la vérification des clients inactifs : " + e.getMessage());
            }
        }
    }

    // Nouvelle méthode : retourne tous les fichiers et leurs sources
    @Override
    public synchronized Map<String, List<String>> obtenirFichiersEtSources() throws RemoteException {
        return new HashMap<>(fichiers);
    }

    public static void main(String[] args)
            throws RemoteException, MalformedURLException, java.rmi.AlreadyBoundException, UnknownHostException {
        LocateRegistry.createRegistry(9008);
        Naming.bind("//" + InetAddress.getLocalHost().getHostName() + ":9008/Annuaire", new AnnuaireImpl());
        System.out.println("RMI server is running");
    }

    @Override
    public List<String> getFichiers() throws RemoteException {
        Collection<String> films = fichiers.keySet();
        Set<String> uniqueStrings = new HashSet<>();
        for (String film : films) {
            uniqueStrings.add(film);
        }
        return new ArrayList<>(uniqueStrings);
    }

}
