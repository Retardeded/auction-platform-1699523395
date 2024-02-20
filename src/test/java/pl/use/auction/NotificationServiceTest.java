package pl.use.auction;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Notification;
import pl.use.auction.repository.NotificationRepository;
import pl.use.auction.service.NotificationService;
import pl.use.auction.service.WebSocketUserService;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketUserService webSocketUserService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createAndSendNotificationForEndedAuction_Success() throws Exception {
        Auction auction = new Auction();
        auction.setTitle("Test Auction");

        AuctionUser highestBidder = new AuctionUser();
        highestBidder.setEmail("bidder@example.com");
        auction.setHighestBidder(highestBidder);

        AuctionUser auctionCreator = new AuctionUser();
        auctionCreator.setEmail("creator@example.com");
        auction.setAuctionCreator(auctionCreator);

        when(notificationService.isUserOnline(highestBidder)).thenReturn(true);
        when(notificationService.isUserOnline(auctionCreator)).thenReturn(true);

        notificationService.createAndSendNotificationForEndedAuction(auction);

        verify(messagingTemplate).convertAndSendToUser(eq("bidder@example.com"), eq("/queue/notifications"), anyString());
        verify(messagingTemplate).convertAndSendToUser(eq("creator@example.com"), eq("/queue/notifications"), anyString());
    }

    @Test
    void createAndSendEndingSoonNotification_Success() throws JsonProcessingException {
        Auction auction = new Auction();
        auction.setTitle("Exciting Auction");
        auction.setSlug("exciting-auction");
        LocalDateTime nowMinusShortMoment = LocalDateTime.now().minusHours(1).plusSeconds(30);
        auction.setEndTime(nowMinusShortMoment);
        AuctionUser observer = new AuctionUser();
        observer.setEmail("observer@example.com");

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setDescription("The auction 'Exciting Auction' is ending soon!");
        notification.setActionUrl("/auction/exciting-auction");
        notification.setActionText("View Auction");

        when(notificationService.isUserOnline(observer)).thenReturn(true);

        doNothing().when(messagingTemplate).convertAndSendToUser(
                eq(observer.getEmail()), eq("/queue/notifications"), anyString());

        notificationService.createAndSendEndingSoonNotification(auction, observer);

        verify(messagingTemplate).convertAndSendToUser(
                eq(observer.getEmail()), eq("/queue/notifications"), anyString());
    }
}
