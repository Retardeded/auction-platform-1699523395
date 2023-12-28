package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.use.auction.service.AuctionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuctionController {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionService auctionService;

    @GetMapping("/auctions/bid/{id}")
    public String showPlaceBidPage(@PathVariable("id") Long auctionId, Model model) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction Id: " + auctionId));

        model.addAttribute("auctionId", auctionId);
        model.addAttribute("currentHighestBid", auction.getHighestBid());
        return "auctions/place-bid";
    }

    @PostMapping("/auctions/bid")
    public String placeBid(@RequestParam("auctionId") Long auctionId,
                           @RequestParam("bidAmount") BigDecimal bidAmount,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction Id:" + auctionId));
        AuctionUser bidder = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean bidPlaced = auctionService.placeBid(auction, bidder, bidAmount);
        if (bidPlaced) {
            redirectAttributes.addFlashAttribute("successMessage", "Bid placed successfully!");
            return "redirect:/auctions/all";
        } else {
            model.addAttribute("errorMessage", "Bid not high enough!");
            model.addAttribute("auctionId", auctionId); // Include this to render the form again
            return "auctions/place-bid";
        }
    }

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
        auction.setAuctionCreator(user);
        auction.setStartTime(LocalDateTime.now());
        auctionRepository.save(auction);

        return "redirect:/profile/auctions";
    }

    @GetMapping("/auctions/all")
    public String viewAllOngoingAuctions(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Auction> ongoingAuctions = auctionRepository.findByEndTimeAfter(LocalDateTime.now())
                .stream()
                .filter(auction -> !auction.getAuctionCreator().equals(currentUser))
                .collect(Collectors.toList());

        model.addAttribute("ongoingAuctions", ongoingAuctions);
        return "auctions/all-auctions";
    }
}
