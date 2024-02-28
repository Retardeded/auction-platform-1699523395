package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import pl.use.auction.dto.NotificationDTO;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Notification;
import pl.use.auction.repository.NotificationRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class NotificationController {

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(Authentication authentication) {
        AuctionUser user = userService.findByUsernameOrThrow(authentication.getName());
        List<Notification> notifications = notificationRepository.findByUserAndReadIsFalse(user);

        List<NotificationDTO> notificationDTOs = notifications.stream()
                .map(notification -> new NotificationDTO(
                        notification.getId(),
                        notification.getDescription(),
                        notification.getActionUrl(),
                        notification.getActionText()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(notificationDTOs);
    }

    @PostMapping("/notifications/mark-read/{notificationId}")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long notificationId, Authentication authentication) {
        AuctionUser user = userService.findByUsernameOrThrow(authentication.getName());
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUser().equals(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You cannot mark this notification as read.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);

        return ResponseEntity.ok().body("Notification marked as read");
    }
}
