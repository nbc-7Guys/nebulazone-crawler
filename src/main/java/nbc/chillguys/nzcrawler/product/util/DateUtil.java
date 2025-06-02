package nbc.chillguys.nzcrawler.product.util;

public abstract class DateUtil {

	public static String getQuarter(int month) {
		if (month >= 1 && month <= 3) return "Q1";
		if (month >= 4 && month <= 6) return "Q2";
		if (month >= 7 && month <= 9) return "Q3";
		if (month >= 10 && month <= 12) return "Q4";
		return "Unknown";
	}
}
