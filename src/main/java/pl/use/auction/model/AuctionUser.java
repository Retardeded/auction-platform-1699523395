package pl.use.auction.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class AuctionUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @Column(unique = true)
    private String email;

    private String password;

    private boolean isVerified = false;
    private String verificationToken;

    private String resetToken;
    private LocalDateTime resetTokenExpiryTime;

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