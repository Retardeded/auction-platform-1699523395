package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;

import java.math.BigDecimal;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    public boolean placeBid(Auction auction, AuctionUser bidder, BigDecimal bidAmount) {
        boolean bidPlaced = false;
        if (bidAmount.compareTo(auction.getHighestBid()) > 0) {
            auction.setHighestBid(bidAmount);
            auction.setHighestBidder(bidder);
            addToWatchlist(bidder, auction);

            auctionRepository.save(auction);
            bidPlaced = true;
        }
        return bidPlaced;
    }

    public void addToWatchlist(AuctionUser user, Auction auction) {
        user.getObservedAuctions().add(auction);
        userRepository.save(user);
    }

    public void removeFromWatchlist(AuctionUser user, Auction auction) {
        user.getObservedAuctions().remove(auction);
        userRepository.save(user);
    }
}
