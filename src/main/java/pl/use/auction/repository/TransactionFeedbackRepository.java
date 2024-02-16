package pl.use.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.TransactionFeedback;

import java.util.Optional;

public interface TransactionFeedbackRepository extends JpaRepository<TransactionFeedback, Long> {
    Optional<TransactionFeedback> findByAuction(Auction auction);
    Optional<TransactionFeedback> findByAuctionAndBuyer(Auction auction, AuctionUser buyer);
}
