package pl.use.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserAndReadIsFalse(AuctionUser user);

    List<Notification> findByUserAndDeliveredIsFalse(AuctionUser user);

    long countByUserAndReadIsFalse(AuctionUser user);
}
