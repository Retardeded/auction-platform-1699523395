package pl.use.auction.model;

public enum FeaturedType {
    CHEAP("cheap.png"),
    EXPENSIVE("expensive.png"),
    NONE("");

    private final String imagePath;

    FeaturedType(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImagePath() {
        return imagePath;
    }
}
