package pl.use.auction.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
public class AuctionUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String role;
    @Email
    @Column(unique = true)
    private String email;
    @NotEmpty
    @Size(min = 4, max = 20)
    private String username;
    private String firstName;
    private String lastName;
    private String location;
    private String phoneNumber;
    @NotEmpty
    private String password;

    private boolean isVerified = false;
    private String verificationToken;

    private String resetToken;
    private LocalDateTime resetTokenExpiryTime;

    @OneToMany(mappedBy = "auctionCreator")
    private List<Auction> createdAuctions;

    @OneToMany(mappedBy = "highestBidder")
    private List<Auction> bidAuctions;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "observed_auctions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "auction_id")
    )
    private Set<Auction> observedAuctions = new HashSet<>();

    @Override
    public String toString() {
        return "AuctionUser{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                ", isVerified=" + isVerified +
                ", verificationToken='" + verificationToken + '\'' +
                ", resetToken='" + resetToken + '\'' +
                ", resetTokenExpiryTime=" + resetTokenExpiryTime +
                '}';
    }
}