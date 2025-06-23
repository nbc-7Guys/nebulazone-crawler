package nbc.chillguys.nzcrawler.product.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatalogExtractUtil {

	private static final Pattern SOCKET_PATTERN = Pattern.compile("\\(([^)]+)\\)");

	private CatalogExtractUtil() {}

	public static String extractFirstWord(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}

		return text.split(" ")[0].trim();
	}

	public static String extractFirstDescriptionPart(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}

		return description.split("/")[0].trim();
	}

	public static String extractSocket(String description) {
		String firstPart = extractFirstDescriptionPart(description);
		if (firstPart == null) {
			return null;
		}

		Matcher matcher = SOCKET_PATTERN.matcher(firstPart);
		return matcher.find() ? matcher.group(1) : null;
	}
}