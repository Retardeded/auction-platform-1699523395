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
        notification.setAction("Check your bids");
        AuctionUser highestBidder = auction.getHighestBidder();
        storeNotificationForUser(highestBidder, notification);
        NotificationDTO notificationDTO = new NotificationDTO(
                notification.getId(),
                notification.getDescription(),
                notification.getAction()
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
