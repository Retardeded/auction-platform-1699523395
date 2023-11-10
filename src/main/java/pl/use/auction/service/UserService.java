package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.use.auction.dto.UserRegistrationDto;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public AuctionUser registerNewUser(UserRegistrationDto registrationDto) {
        AuctionUser auctionUser = new AuctionUser();
        auctionUser.setEmail(registrationDto.getEmail());
        auctionUser.setPassword(registrationDto.getPassword());
        return userRepository.save(auctionUser);
    }
}