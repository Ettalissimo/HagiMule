package Annuaire;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnuaireImpl extends UnicastRemoteObject implements Annuaire {

    private Map<String, List<String>> fichiers; // { nameSource:port , listes des filmes}

    public AnnuaireImpl() throws RemoteException {
        super();
        this.fichiers = new HashMap<>();
    }

    @Override
    public synchronized void enregistrerFichier(String clientId, String fichier) throws RemoteException {
        fichiers.putIfAbsent(fichier, new ArrayList<>());
        if (!fichiers.get(fichier).contains(clientId)) {
            fichiers.get(fichier).add(clientId);
            System.out.println("Fichier enregistre : " + fichier + " par le client : " + clientId);
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
    
    public static void main(String[] args) throws RemoteException, MalformedURLException, java.rmi.AlreadyBoundException {
        LocateRegistry.createRegistry(9008);
        Naming.bind("//localhost:9008/Annuaire", new AnnuaireImpl());
        System.out.println("RMI server is running");

    }
}
