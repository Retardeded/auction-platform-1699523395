package pl.use.auction;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import pl.use.auction.controller.NotificationController;
import pl.use.auction.dto.NotificationDTO;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Notification;
import pl.use.auction.repository.NotificationRepository;
import pl.use.auction.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class NotificationControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void testGetNotifications() {
        String userEmail = "user@example.com";
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail(userEmail);

        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setUser(currentUser);
        notification1.setDescription("Notification 1");
        notification1.setActionText("Action 1");
        notification1.setRead(false);

        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setUser(currentUser);
        notification2.setDescription("Notification 2");
        notification2.setActionText("Action 2");
        notification2.setRead(false);

        List<Notification> notifications = List.of(notification1, notification2);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(currentUser));
        when(notificationRepository.findByUserAndReadIsFalse(currentUser)).thenReturn(notifications);

        ResponseEntity<?> response = notificationController.getNotifications(authentication);

        assertEquals(200, response.getStatusCodeValue());

        @SuppressWarnings("unchecked")
        List<NotificationDTO> responseBody = (List<NotificationDTO>) response.getBody();

        assertNotNull(responseBody);
        assertEquals(2, responseBody.size());
        assertTrue(responseBody.stream().anyMatch(dto -> dto.getDescription().equals("Notification 1")));
        assertTrue(responseBody.stream().anyMatch(dto -> dto.getDescription().equals("Notification 2")));
    }

    @Test
    void testMarkNotificationAsRead_Success() {
        String userEmail = "user@example.com";
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail(userEmail);

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUser(currentUser);
        notification.setRead(false);

        when(authentication.getName()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(currentUser));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        ResponseEntity<?> response = notificationController.markNotificationAsRead(1L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void testMarkNotificationAsRead_NotFound() {
        String userEmail = "user@example.com";
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail(userEmail);

        when(authentication.getName()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(currentUser));
        when(notificationRepository.findById(anyLong())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            notificationController.markNotificationAsRead(1L, authentication);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Notification not found", exception.getReason());
    }

    @Test
    void testMarkNotificationAsRead_Forbidden() {
        String userEmail = "user@example.com";
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail(userEmail);

        AuctionUser otherUser = new AuctionUser();
        otherUser.setEmail("other@example.com");

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUser(otherUser);
        notification.setRead(false);

        when(authentication.getName()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(currentUser));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        ResponseEntity<?> response = notificationController.markNotificationAsRead(1L, authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}