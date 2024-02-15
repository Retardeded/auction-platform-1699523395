package pl.use.auction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.use.auction.dto.NotificationDTO;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Notification;
import pl.use.auction.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WebSocketUserService webSocketUserService;

    public void createAndSendNotification(Auction auction) throws JsonProcessingException {
        Notification notification = new Notification();
        notification.setDescription("Congratulations! You are the highest bidder for the auction: " + auction.getTitle());
        notification.setActionUrl("/profile/my-bids-and-watches");
        notification.setActionText("Check your bids");
        AuctionUser highestBidder = auction.getHighestBidder();
        storeNotificationForUser(highestBidder, notification);
        NotificationDTO notificationDTO = new NotificationDTO(
                notification.getId(),
                notification.getDescription(),
                notification.getActionUrl(),
                notification.getActionText()
        );

        String notificationJson = new ObjectMapper().writeValueAsString(notificationDTO);

        if (isUserOnline(highestBidder)) {
            messagingTemplate.convertAndSendToUser(
                    highestBidder.getEmail(),
                    "/queue/notifications",
                    notificationJson
            );
        }
    }

    public void createAndSendEndingSoonNotification(Auction auction, AuctionUser observer) throws JsonProcessingException {
        Notification notification = new Notification();
        notification.setDescription("The auction '" + auction.getTitle() + getTimeLeftMessage(auction));
        notification.setActionUrl("/auction/" + auction.getSlug());
        notification.setActionText("View Auction");

        storeNotificationForUser(observer, notification);

        NotificationDTO notificationDTO = new NotificationDTO(
                notification.getId(),
                notification.getDescription(),
                notification.getActionUrl(),
                notification.getActionText()
        );

        String notificationJson = new ObjectMapper().writeValueAsString(notificationDTO);

        if (isUserOnline(observer)) {
            System.out.println("SEND");
            messagingTemplate.convertAndSendToUser(
                    observer.getEmail(),
                    "/queue/notifications",
                    notificationJson
            );
        }
    }

    public String getTimeLeftMessage(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        long minutesBetween = java.time.Duration.between(now, auction.getEndTime()).toMinutes();

        // Calculate hours between and round up
        long hoursBetween = (minutesBetween + 59) / 60; // Add 59 minutes before dividing to ensure rounding up

        if (hoursBetween <= 0) {
            return " is ending in less than an hour!";
        } else if (hoursBetween == 1) {
            return " is ending in approximately 1 hour!";
        } else {
            return " is ending in approximately " + hoursBetween + " hours!";
        }
    }

    private boolean isUserOnline(AuctionUser user) {
        return webSocketUserService.isUserOnline(user.getEmail());
    }

    private void storeNotificationForUser(AuctionUser user, Notification notification) {
        notification.setUser(user);
        notificationRepository.save(notification);
    }

    public void deliverStoredNotifications(AuctionUser user) {
        List<Notification> storedNotifications = notificationRepository.findByUserAndDeliveredIsFalse(user);

        for (Notification notification : storedNotifications) {
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications",
                    notification.getDescription()
            );
            notification.setDelivered(true);
            notificationRepository.save(notification);
        }
    }
}
