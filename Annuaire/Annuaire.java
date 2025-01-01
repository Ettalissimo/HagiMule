package Annuaire;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Annuaire extends Remote {

    void enregistrerFichier(String clientId, String fichier) throws RemoteException;

    void retirerClient(String clientId) throws RemoteException;
    
    List<String> rechercherSources(String fichier) throws RemoteException;
}