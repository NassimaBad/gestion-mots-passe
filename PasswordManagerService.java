package com.passwordmanager.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PasswordManagerService extends Remote {
    boolean login(String username, String password) throws RemoteException;
    boolean register(String username, String password) throws RemoteException;
    void addPassword(String username, PasswordEntry entry) throws RemoteException;
    List<PasswordEntry> listPasswords(String username) throws RemoteException;
    void updatePassword(String username, PasswordEntry entry) throws RemoteException;
    void deletePassword(String username, String website) throws RemoteException;
} 