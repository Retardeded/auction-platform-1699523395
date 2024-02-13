package pl.use.auction.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private AuctionUser user;

    private String description;

    private String action;

    private boolean read = false;

    private boolean delivered = false;
}