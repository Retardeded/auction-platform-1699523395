package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketEventListener {

    private final WebSocketUserService webSocketUserService;

    @Autowired
    public WebSocketEventListener(WebSocketUserService webSocketUserService) {
        this.webSocketUserService = webSocketUserService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        String username = extractUsername(event);
        webSocketUserService.registerConnectedUser(username);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String username = extractUsername(event);
        webSocketUserService.deregisterDisconnectedUser(username);
    }

    private String extractUsername(AbstractSubProtocolEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();
        return userPrincipal != null ? userPrincipal.getName() : null;
    }
}
