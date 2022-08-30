package clock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class Logger {
	private static Level level;
	private static File logfile;
	private static boolean smartflush = false; //TODO: this should serve an actual purpose
	
	private static final String whitespaces = "                             ";
	private static final Level maxStringLength = Level.WARNING;	
	private static FileWriter fileWriter;
	private static BufferedWriter writer;
	
	public static enum Level{
		DEBUG,
		INFO,
		WARNING,
		ERROR,
		NONE
	}
	
	static void debug(String message) {
		writeNonErrorEntry(Level.DEBUG, message);
	}
	
	static void info(String message) {
		writeNonErrorEntry(Level.INFO, message);
	}
	
	static void warning(String message) {
		writeNonErrorEntry(Level.WARNING, message);	
	}
	
	static void error(Exception e) {
		if(logfile == null || level == null) {
			System.out.println("Warning! Logging attempt while Logger is not initialized!");
			return;
		}
		if(level.ordinal() > Level.ERROR.ordinal()) {
			return;
		}
		
		if(smartflush) {
			writeEntry(Level.ERROR, e.getMessage() + Arrays.toString(e.getStackTrace()));
		} else {
			writeEntryFlush(Level.ERROR, e.getMessage() + Arrays.toString(e.getStackTrace()));
		}
	}
	
	static void init(File logfile, Level level) throws IOException {
		Logger.logfile = logfile;
		Logger.level = level;
		Logger.writer = openLoggingStream();
	}
	
	static void close() throws IOException {
		writer.close();
	}

	private static void writeNonErrorEntry(Level levelToWrite, String message) {
		if(logfile == null || level == null) {
			System.out.println("Warning! Logging attempt while Logger is not initialized!");
			return;
		}
		if(level.ordinal() > levelToWrite.ordinal()) {
			return;
		}
		
		if(smartflush) {
			writeEntry(levelToWrite, message);
		} else {
			writeEntryFlush(levelToWrite, message);
		}
	}
	
	private static void writeEntryFlush(Level level, String message) {
		try {
			writer.write(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " | " + level.name() + whitespaces.substring(0, maxStringLength.name().length() - level.name().length()) + " | " + message);
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			System.out.println("Could not write to logfile! Disabling further logging for this instance.");
			Logger.level = Level.NONE;
		}
	}
	
	private static void writeEntry(Level level, String message) {
		try {
			writer.write(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " | " + level.name() + whitespaces.substring(0, maxStringLength.name().length() - level.name().length()) + " | " + message);
			writer.newLine();
		} catch (IOException e) {
			System.out.println("Could not write to logfile! Disabling further logging for this instance.");
			Logger.level = Level.NONE;
		}
	}
	
	private static BufferedWriter openLoggingStream() throws IOException {		
		fileWriter = new FileWriter(logfile, true);
		
		return new BufferedWriter(fileWriter);
	}
	
	static void enableSmartflush() {
		smartflush = true;
	}
	
	static void disableSmartflush() {
		smartflush = false;
	}

	static Level getLevel() {
		return level;
	}

	static void setLevel(Level level) {
		Logger.level = level;
	}
}
