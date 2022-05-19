package clock;

@Deprecated
public class TimeBuilder {
	Time time;
	
	public TimeBuilder(){
		this.time = new Time();
	}
	
	/**
	 * Parses a (partial) time from a string. Takes input of the form
	 * <p>7:20 <br>
	 * 21</p>
	 * 
	 * @param timeString - either of the form hh:mm or mm
	 * @return
	 */
	public TimeBuilder fromString(String timeString) {
		if(timeString.contains(":")) {
			
			String[] split = timeString.split(":");
			
			if(split.length > 2) {
				throw new IllegalArgumentException("There are too many colons in the string \"" + timeString + "\"");
			}
			
			this.time.setHours(Short.parseShort(split[0]));
			timeString = split[1];
		}
		
		this.time.setMinutes(Short.parseShort(timeString));
		
		return this;
	}
	
	/**
	 * Clears the current time in the builder.
	 * 
	 * @return instance of the builder
	 */
	public TimeBuilder clear() {
		this.time = new Time();
		return this;
	}
	
	/**
	 * Sets the hours for the time currently in the builder.
	 * 
	 * @param hours
	 * @return instance of the builder
	 */
	public TimeBuilder hours(int hours) {
		this.time.setHours(hours);
		return this;
	}
	
	/**
	 * Sets the minutes for the time currently in the builder.
	 * 
	 * @param minutes
	 * @return instance of the builder
	 */
	public TimeBuilder minutes(int minutes) {
		this.time.setMinutes(minutes);
		return this;
	}
	
	/**
	 * Fully builds the time in this builder. If any fields were left blanc they are set to 0.
	 * 
	 * @return time built
	 */
	public Time build() {
		return this.time;
	}
}
