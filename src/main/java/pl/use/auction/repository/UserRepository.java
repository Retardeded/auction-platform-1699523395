package pl.use.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.use.auction.model.AuctionUser;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AuctionUser, Long> {
    Optional<AuctionUser> findByEmail(String email);
}