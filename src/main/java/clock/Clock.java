package clock;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import clock.Logger.Level;

/**
 * <p>This class is the main class for configuration, management of resources and starting a timer specified by console input.</p>
 * 
 * @author Thomas Meier
 *
 */
public class Clock {
	
	// default values
	private static final int DEFAULT_TIMER = 8;
	private static final int UPDATE_INTERVAL = 10000;
	private static final Path DIRECTORY_PATH = Path.of(System.getProperty("user.home") + "/Documents/Clock");
	private static final String FILE_NAME = "clock_" + LocalDate.now().getYear() +  ".log";
	private static final String STRING_LINE_SEPERATOR = System.lineSeparator();
	
	// configuration
	private boolean verbose = false;
	private boolean sound = false;
	private boolean help = false;
	private boolean end = false;
	private boolean log = true;
	private boolean clear = false;
	
	// input parameters
	private LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
	private LocalTime breaktime = LocalTime.of(0, 45);
	private LocalTime overtime = LocalTime.of(0, 0);
	private LocalTime freetime = LocalTime.of(0, 0);
	
	// utility values
	private boolean nextDay = false;
	private long currentOvertime = 0;
	
	//TODO sound
	//TODO end
	//TODO multithreading -> for I/O
	//TODO automatically calculate overtime at programm exit -> put into logs
	//TODO read accumulated overtime from logs and inform user on programm start -> ask if and how much of it should be applied to timer
	//TODO argument for log level
	//TODO warning for max log-size (with overwrite option? -> old entries overwritten?)
	//TODO logentries for timer reached milestones (every hour or so)
	//TODO smartflush -> dont flush on every writeEntry instead at certain intervals (like time or events)
	
	public static void main(String[] args) throws IOException {
		Clock clock = new Clock();
		String[] testArgs = {"adsdasdasdascxycxy"};
		args = testArgs;
		
		// read config into clock
		clock.parseArgs(args);
		
		
		// exit prematurely if any problems arise
		if(clock.applyConfiguration()) {
			return;
		}
		
		// shutdown hook for Logging message if process is killed
		clock.addShutdownHook();
		
		
		// start timer with given configuration
		clock.startTimer();
	}
	
	
	
	// Main functions
	
	/**
	 * Parses the given argument array into the clocks configuration and notifies user through {@code System.out} if necessary.
	 * 
	 * @param args - argument array to be parsed
	 */
	private void parseArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			try {
				switch(arg) {
					case "-h","--help": {
						help = true;
						printHelp();
						return;
					}
					case "-t","--time": {
						if(i+1 < args.length) {
							time = LocalTimeParser.fromString(args[++i]);
						} else {
							System.out.println("No time found after the argument!" + STRING_LINE_SEPERATOR + " Usage: -t|--time <time>");
						}
					} break;
					case "-b","--breaktime": {
						if(i+1 < args.length) {
							breaktime = LocalTimeParser.fromString(args[++i]);
						} else {
							System.out.println("No time found after the argument!" + STRING_LINE_SEPERATOR + " Usage: -b|--breaktime <time>");
						}
					} break;
					case "-o","--overtime": {
						if(i+1 < args.length) {
							overtime = LocalTimeParser.fromString(args[++i]);
						} else {
							System.out.println("No time found after the argument!" + STRING_LINE_SEPERATOR +" Usage: -o|--overtime <time>");
						}
					} break;
					case "-f","--freetime": {
						if(i+1 < args.length) {
							freetime = LocalTimeParser.fromString(args[++i]);
						} else {
							System.out.println("No time found after the argument!" + STRING_LINE_SEPERATOR + " Usage: -f|--freetime <time>");
						}
					} break;
					//TODO this here can be the point of change for configration of log level
					case "-l","--nolog": {
						log = false;
						Logger.setLevel(Level.NONE);
					} break;
					case "-v","--verbose": {
						verbose = true;
					} break;
					case "-s","--sound": {
						sound = true;
					}
					case "-c","--clearlog": {
						clear = true;
						return;
					} 
					default:
						System.out.println("Could not parse argument \"" + arg + "\". Stopping process." + STRING_LINE_SEPERATOR
								+ "Valid arguments can be referenced in the following help screen:" + STRING_LINE_SEPERATOR);
						printHelp(true);
						help = true;
						return;
				}
			} catch (DateTimeParseException | IllegalArgumentException e) {
				System.out.println("Invalid time parameter \"" + args[i] + "\" after argument \"" + arg + "\". Stopping process." + STRING_LINE_SEPERATOR
						+ "Valid parameters for the arguments are referenced in the following help screen:" + STRING_LINE_SEPERATOR);
				printHelp(true);
				help = true;
				return;
			}
		}
	}
	
	/**
	 * Applies the current configuration parameters to the clock (like creating a logger/logfile for {@code log=true}). 
	 * Returns a boolean to signal if the programm should quit prematurely.
	 * 
	 * @return {@code true} if the process should quit prematurely; {@code false} otherwise 
	 * @throws IOException if console-reading failed
	 */
	private boolean applyConfiguration() throws IOException {
		if(log) {
			try {
				if(createLogfileAndInitiateLogger()) {
					System.out.println("Created new logging file at " + DIRECTORY_PATH + STRING_LINE_SEPERATOR);
				}
			} catch(IOException e) {
				System.out.println("Got an exception while initiating logging: " + e.getMessage() + STRING_LINE_SEPERATOR +
						"Should the programm continue without logging? (y/n)");
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				if(!mapStringBoolean(reader.readLine())) {
					return true;
				} 
			}
		}
		
		if(help) {
			Logger.info("Printing help text and exit process.");
			return true;
		}
		
		if(clear) {
			try {
				clearlog();
			} catch(IOException e) {
				System.out.println("Got an exception while clearing log: " + e.getMessage());
			}	
			return true;
		}
		
		if(verbose) {
			Logger.info("Setting console output to verbose.");
			printConfiguration();
			System.out.println();
		}
		
		return false;
	}
	
	/**
	 * Starts the timer with console-output for the current clock-configuration.
	 */
	private void startTimer() {
		String emptyLine = "\r                                                                                                   \r";
		String currentLine = "";
		LocalTime ending = calculateTimer();

		if(ending.isBefore(time)) {
			Logger.info("Ending time is at the next day.");
			nextDay = true;
		}
		
		Logger.info("Started Timer for configuration " + toString() + ".");
		Logger.info("Timer runs out at " + ending + " in " + formatTimeDifference(LocalTime.now(), ending) + ".");
					
		//TODO there are better solutions for overwriting the old line
		while(!end) {
			LocalTime now = LocalTime.now();
			int compareValue = now.truncatedTo(ChronoUnit.MINUTES).compareTo(ending);
			System.out.print(emptyLine);
			if(compareValue < 0 || nextDay) {
				currentLine = "Timer runs out at " + ending + " in " + formatTimeDifference(now, ending) + ".";
			} else if(compareValue > 0) {
				currentLine = "Timer already ran out at " + ending + ", " + formatTimeDifference(now, ending) + " ago.";
				currentOvertime = ending.until(now, ChronoUnit.MINUTES);
			} else {
				currentLine = "Timer is over right now!";
				nextDay = false;
			}
			System.out.print(currentLine);
			try {
				if(verbose) {
					for (int i = 0; i < UPDATE_INTERVAL / 1000; i++) {
						System.out.print(emptyLine + currentLine + " Sleeping for " + (UPDATE_INTERVAL / 1000 - i) + " seconds.");
						Thread.sleep(1000);
					}
				} else {
					Thread.sleep(UPDATE_INTERVAL);
				}
			} catch (InterruptedException e) {
				Logger.error(e);
				if(verbose) {
					System.out.println("Thread was interrupted while sleeping!");
				}
			}
		}
		
		//TODO add currentOvertime to sumOvertime
		Logger.info("Exited Timer.");
	}
	
	
	
	// Utility functions
	
	/**
	 * Deletes old logfile and creates an empty new one.
	 * 
	 * @return true if process finished succesfully; false if any problems arise
	 * @throws IOException if any IOExceptions occur with the file
	 */
	private boolean clearlog() throws IOException {
		File logfile = new File(DIRECTORY_PATH.toString() + "/" + FILE_NAME);
		Logger.close();
		System.out.print("Deleting logfile at \"" + logfile.getAbsolutePath() + "\"... ");

		if(!logfile.exists()) {
			System.out.println("No logfile named \"" + logfile.getName() + "\" available to be deleted.");
		}
		
		if(!logfile.delete()) {
			Files.delete(Paths.get(logfile.getAbsolutePath()));
			System.out.println("Failed!");
			return false;
		} else {
			System.out.println("Successfull!");
		}
		
		System.out.print("Creating new empty logfile at \"" + logfile.getAbsolutePath() + "\"... ");
		if(!logfile.createNewFile()) {
			System.out.print("File already exists. This should not be possible!");
		} else {
			System.out.print("Succesfull!");
		}
		
		return true;
	}
	
	private String formatTimeDifference(LocalTime before, LocalTime reference) {
		int mvb = before.getHour() * 60 + before.getMinute(); // minute value for before
		int mvr = reference.getHour() * 60 + reference.getMinute(); // minute value for reference
		
		
		if(mvb > mvr) {
			mvr += 1440; // add a full day's worth of minutes
		}
		
		int difference = mvr - mvb;
		int hours = difference / 60;
		int minutes = difference % 60;
		
		
		return  (hours > 0 ? hours > 1 ? hours + " hours" : hours + " hour" : " ") + (hours > 0 && minutes > 0 ? " " : "") +
				(minutes > 0 ? minutes > 1 ? minutes + " minutes" : minutes + " minute" : "");
	}

	private LocalTime calculateTimer() {
		return time.plusHours(DEFAULT_TIMER + breaktime.getHour() + overtime.getHour() - freetime.getHour()).plusMinutes(breaktime.getMinute() + overtime.getMinute() - freetime.getMinute());
	}
	
	/**
	 * Adds a hook that informs the user that the process was killed unexpectedly.
	 */
	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
	        public void run() {
	        	Logger.warning("Process was killed irregularly!");
	            System.out.println("Process was killed irregularly!");
	        }
	    }, "Shutdown-Thread"));
	}
	
	/**
	 * Creates a new logfile at the given FILE_PATH constant and initiates the logger to log into that file with log-level INFO.
	 * 
	 * @return {@code true} if the file does not exist and was successfully created; {@code false} if the file already exists
	 * @throws IOException if an I/O-Exception occurs while creating
	 */
	private boolean createLogfileAndInitiateLogger() throws IOException {
		File logfile = new File(DIRECTORY_PATH.toString() + "/" + FILE_NAME);
		Files.createDirectories(DIRECTORY_PATH);
		
		boolean isNew = logfile.createNewFile();
		
		Logger.init(logfile, Level.INFO);
		
		return isNew;	
	}
	
	private boolean mapStringBoolean(String string) {
		if(string == null) {
			return false;
		}
		
		switch(string.toLowerCase()){
			case "y","yes","yep","yeah","yea": return true;
			default: return false;
		}
	}
	
	//TODO update this to current arguments
	private void printConfiguration() {
		System.out.println("Starting timer with configuration:" + STRING_LINE_SEPERATOR
				+ "----------------------------------" + STRING_LINE_SEPERATOR
				+ "sound = " + sound + STRING_LINE_SEPERATOR
				+ "time = " + time + STRING_LINE_SEPERATOR
				+ "breaktime = " + breaktime + STRING_LINE_SEPERATOR
				+ "freetime = " + freetime + STRING_LINE_SEPERATOR
				+ "file = " + log + STRING_LINE_SEPERATOR);
		System.out.println("Programm defaults are set to:" + STRING_LINE_SEPERATOR
				+ "------------------------------" + STRING_LINE_SEPERATOR
				+ "update interval = " + UPDATE_INTERVAL + "ms" + STRING_LINE_SEPERATOR
				+ "default timer length = " + DEFAULT_TIMER + STRING_LINE_SEPERATOR
				+ "file path = " + DIRECTORY_PATH);
	}
	
	private void printHelp() {
		printHelp(false);
	}
	
	private void printHelp(boolean skipIntro) {
		if(!skipIntro) {
			System.out.println("This java file is used for creating remindal timers with 8-hour intervals. It can create" + STRING_LINE_SEPERATOR
				+ "a console message additional to a sound if needed." + STRING_LINE_SEPERATOR);
		}
		System.out.println( "Usage: java -jar .\\Clock.jar [options]" + STRING_LINE_SEPERATOR + STRING_LINE_SEPERATOR
				+ "Options:" + STRING_LINE_SEPERATOR
				+ "  -t --time <time>      | Defines a time from which the timer will count. The format" + STRING_LINE_SEPERATOR
				+ "                        | for the supplied <time> parameter should be" + STRING_LINE_SEPERATOR
				+ "                        | \"<hours>:<minutes>\" where 24 >= \"<hours>\" >= 0 and" + STRING_LINE_SEPERATOR
				+ "                        | 60 >= \"<minutes>\" 0." + STRING_LINE_SEPERATOR
				+ "                        | If not defined the current system time will be used." + STRING_LINE_SEPERATOR
				+ "                        |" + STRING_LINE_SEPERATOR
				+ "  -b --breaktime <time> | Defines the length of all breaks which the timer will add" + STRING_LINE_SEPERATOR
				+ "                        | to the total timer. The format for the <time> parameter" + STRING_LINE_SEPERATOR
				+ "                        | should be \"<hours>:<minutes>\" or \"<minutes>\" where" + STRING_LINE_SEPERATOR
				+ "                        | 24 >= \"<hours>\" >= 0 and 60 >= \"<minutes>\" >= 0." + STRING_LINE_SEPERATOR
				+ "                        | If not defined a default of 45 minutes will be used." + STRING_LINE_SEPERATOR
				+ "                        |" + STRING_LINE_SEPERATOR
				+ "  -o --overtime <time>  | Defines the length of overtime to be added to the" + STRING_LINE_SEPERATOR
				+ "                        | total timer. The format for the <time> parameter should" + STRING_LINE_SEPERATOR
				+ "                        | be \"<hours>:<minutes>\" or \"<minutes>\" where" + STRING_LINE_SEPERATOR
				+ "                        | 24 >= \"<hours>\" >= 0 and 60 >= \"<minutes>\" >= 0." + STRING_LINE_SEPERATOR
				+ "                        | If not defined a default of 0 minutes will be used." + STRING_LINE_SEPERATOR
				+ "                        |" + STRING_LINE_SEPERATOR
				+ "  -f --freetime <time>  | Defines the length of freetime to be substracted from" + STRING_LINE_SEPERATOR
				+ "                        | the total timer. The format for the <time> parameter" + STRING_LINE_SEPERATOR
				+ "                        | should be \"<hours>:<minutes>\" or \"<minutes>\" where" + STRING_LINE_SEPERATOR
				+ "                        | 24 >= \"<hours>\" >= 0 and 60 >= \"<minutes>\" >= 0." + STRING_LINE_SEPERATOR
				+ "                        | If not defined a default of 0 minutes will be used." + STRING_LINE_SEPERATOR
				+ "                        |" + STRING_LINE_SEPERATOR
				+ "  -l --nolog            | Prevents the logging to a log-file at the path" + STRING_LINE_SEPERATOR
				+ "                        | \"" + DIRECTORY_PATH + "\"." + STRING_LINE_SEPERATOR
				+ "                        | Only affects the current instance of execution." + STRING_LINE_SEPERATOR
				+ "                        |" + STRING_LINE_SEPERATOR
				+ "  -c --clearlog         | Clears the logfile currently located at " + STRING_LINE_SEPERATOR
				+ "                        | \"" + DIRECTORY_PATH + "\"." + STRING_LINE_SEPERATOR
				+ "                        | Then immediatly exits the programm." + STRING_LINE_SEPERATOR
				+ "                        |" + STRING_LINE_SEPERATOR
				+ "  -h --help             | Prints this help text and exits the programm." + STRING_LINE_SEPERATOR
				+ "                        |" + STRING_LINE_SEPERATOR
				+ "  -v --verbose          | Prints additional info on execution." + STRING_LINE_SEPERATOR
				+ "                        |" + STRING_LINE_SEPERATOR
				+ "  -s --sound            | Creates a sound in addition to the default console" + STRING_LINE_SEPERATOR
				+ "                        | message when the timer runs out.");
	}



	@Override
	public String toString() {
		return "Clock [verbose=" + verbose + ", sound=" + sound + ", help=" + help + ", end=" + end + ", time=" + time 
				+ ", breaktime=" + breaktime + ", overtime=" + overtime + ", freetime=" + freetime + ", nextDay=" + nextDay + "]";
	}
}
