package pl.use.auction.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

public class StringUtils {

    public static String slugToCategoryName(String slug) {
        String[] words = slug.replace("-", " ").split("\\s+");

        return Arrays.stream(words)
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static String createSlugFromTitle(String title) {
        return Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^\\w\\s]", "")
                .replaceAll("\\s+", "-")
                .toLowerCase();
    }
}
