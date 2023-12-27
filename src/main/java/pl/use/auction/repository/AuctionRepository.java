package pl.use.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByUserAndEndTimeAfter(AuctionUser user, LocalDateTime now); // Ongoing auctions
    List<Auction> findByUserAndEndTimeBefore(AuctionUser user, LocalDateTime now); // Past auctions

    List<Auction> findByUser(AuctionUser user);
}