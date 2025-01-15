package Annuaire;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface Annuaire extends Remote {

    void enregistrerFichier(String clientId, String fichier) throws RemoteException;

    void retirerClient(String clientId) throws RemoteException;

    List<String> rechercherSources(String fichier) throws RemoteException;

    // Méthode pour signaler un "heartbeat"
    void envoyerHeartbeat(String clientId) throws RemoteException;

    Map<String, List<String>> obtenirFichiersEtSources() throws RemoteException;

    List<String> getFichiers() throws RemoteException;

}