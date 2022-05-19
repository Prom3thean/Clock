package clock;

import java.time.LocalTime;

public class LocalTimeParser {
	public static LocalTime fromString(String toParse) {
		if(toParse.length() > 5) {
			throw new IllegalArgumentException("String to parse cannot be longer than 5 characters but is " + toParse.length() + " characters long.");
		}
					
		if(toParse.contains(":")) {
			if(toParse.length() < 5) {
				toParse = "0" + toParse;
			}
		} else {
			if(toParse.length() < 2) {
				toParse = "0" + toParse;
			}
			toParse = "00:" + toParse;
		}
		
		return LocalTime.parse(toParse);
	}
}
