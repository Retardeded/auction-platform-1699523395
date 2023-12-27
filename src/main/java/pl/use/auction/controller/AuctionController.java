package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class AuctionController {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/auctions/create")
    public String createAuctionForm(Model model) {
        model.addAttribute("auction", new Auction());
        return "auctions/create-auction";
    }

    @PostMapping("/auctions/create")
    public String createAuction(@ModelAttribute Auction auction, BindingResult bindingResult, Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return "auctions/create-auction";
        }

        AuctionUser user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        auction.setUser(user);
        auction.setStartTime(LocalDateTime.now());
        auctionRepository.save(auction);

        return "redirect:/profile/auctions";
    }

    @GetMapping("/auctions/all")
    public String viewAllOngoingAuctions(Model model) {
        List<Auction> ongoingAuctions = auctionRepository.findByEndTimeAfter(LocalDateTime.now());
        model.addAttribute("ongoingAuctions", ongoingAuctions);
        return "auctions/all-auctions";
    }
}
