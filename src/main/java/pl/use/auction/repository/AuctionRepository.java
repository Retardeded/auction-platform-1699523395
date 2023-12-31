package pl.use.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByAuctionCreatorAndEndTimeAfter(AuctionUser user, LocalDateTime now);
    List<Auction> findByAuctionCreatorAndEndTimeBefore(AuctionUser user, LocalDateTime now);

    List<Auction> findByAuctionCreator(AuctionUser user);

    List<Auction> findByEndTimeAfter(LocalDateTime time);

    List<Auction> findByHighestBidder(AuctionUser user);
}