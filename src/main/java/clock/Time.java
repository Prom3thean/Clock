package clock;

import java.time.LocalTime;

@Deprecated
public class Time {
	private int hours;
	
	private int minutes;

	public Time() {
		this.hours = 0;
		this.minutes = 0;
	}
	
	public Time(int hours, int minutes) {
		this.hours = hours;
		this.minutes = minutes;
	}
	
	public Time(LocalTime time) {
		this.hours = time.getHour();
		this.minutes = time.getMinute();
	}
	
	public int getHours() {
		return hours;
	}

	public void setHours(int hours) {
		this.hours = hours;
	}

	public int getMinutes() {
		return minutes;
	}

	public void setMinutes(int minutes) {
		if(minutes > 59) {
			this.hours += minutes / 60;
			minutes %= 60;
		}
		this.minutes = minutes;
	}

	@Override
	public String toString() {
		return "Time [hours=" + hours + ", minutes=" + minutes + "]";
	}
}
