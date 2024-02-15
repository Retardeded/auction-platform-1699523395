package pl.use.auction.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketUserService {

    private final Set<String> connectedUseremails = ConcurrentHashMap.newKeySet();

    public Set<String> getConnectedUseremails() {
        return new HashSet<>(connectedUseremails);
    }

    public void registerConnectedUser(String email) {
        connectedUseremails.add(email);
    }

    public void deregisterDisconnectedUser(String email) {
        connectedUseremails.remove(email);
    }

    public boolean isUserOnline(String email) {
        return connectedUseremails.contains(email);
    }
}
