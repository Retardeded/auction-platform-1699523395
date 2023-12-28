package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.AuctionRepository;

import java.math.BigDecimal;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    public boolean placeBid(Auction auction, AuctionUser bidder, BigDecimal bidAmount) {
        if (bidAmount.compareTo(auction.getHighestBid()) > 0) {
            auction.setHighestBid(bidAmount);
            auction.setHighestBidder(bidder);
            auctionRepository.save(auction);
            return true;
        }
        return false;
    }
}
