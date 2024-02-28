package pl.use.auction.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketUserService {

    private final Set<String> connectedUsernames = ConcurrentHashMap.newKeySet();

    public Set<String> getConnectedUsernames() {
        return new HashSet<>(connectedUsernames);
    }

    public void registerConnectedUser(String username) {
        connectedUsernames.add(username);
    }

    public void deregisterDisconnectedUser(String username) {
        connectedUsernames.remove(username);
    }

    public boolean isUserOnline(String username) {
        return connectedUsernames.contains(username);
    }
}
