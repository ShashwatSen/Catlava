package com.catlava.plugin.utils;

import com.catlava.plugin.config.PluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Custom logging utilities for CatLava plugin.
 * Provides file-based logging and custom log formatting.
 */
public class LoggerUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();
    
    private static PluginConfig config;

    public static void initialize(PluginConfig pluginConfig) {
        config = pluginConfig;
    }

    /**
     * Get or create a logger with custom formatting
     */
    public static Logger getLogger(Class<?> clazz) {
        return loggers.computeIfAbsent(clazz.getName(), 
            name -> LoggerFactory.getLogger(clazz));
    }

    /**
     * Log with custom formatting
     */
    public static void log(Logger logger, String level, String message) {
        if (!config.getLogging().isEnabled()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String formattedMessage = String.format("[%s] [%s] [%s] %s", 
            timestamp, Thread.currentThread().getName(), level, message);

        // Log to console
        switch (level) {
            case "ERROR" -> logger.error(formattedMessage);
            case "WARN" -> logger.warn(formattedMessage);
            case "DEBUG" -> logger.debug(formattedMessage);
            default -> logger.info(formattedMessage);
        }

        // Log to file if configured
        if (config.getLogging().getLogFile() != null) {
            writeToFile(config.getLogging().getLogFile(), formattedMessage);
        }
    }

    /**
     * Log info message
     */
    public static void info(Logger logger, String message) {
        log(logger, "INFO", message);
    }

    /**
     * Log error message
     */
    public static void error(Logger logger, String message) {
        log(logger, "ERROR", message);
    }

    /**
     * Log error with exception
     */
    public static void error(Logger logger, String message, Throwable throwable) {
        error(logger, message);
        if (throwable != null) {
            log(logger, "ERROR", "Exception: " + throwable.getMessage());
            for (StackTraceElement element : throwable.getStackTrace()) {
                log(logger, "ERROR", "  at " + element.toString());
            }
        }
    }

    /**
     * Log warning message
     */
    public static void warn(Logger logger, String message) {
        log(logger, "WARN", message);
    }

    /**
     * Log debug message
     */
    public static void debug(Logger logger, String message) {
        if (config.getLogging().getLogLevel().equals("DEBUG")) {
            log(logger, "DEBUG", message);
        }
    }

    /**
     * Write message to log file
     */
    private static void writeToFile(String filePath, String message) {
        try {
            Path path = Path.of(filePath);
            
            // Create parent directories if they don't exist
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            
            // Create file if it doesn't exist
            File file = path.toFile();
            if (!file.exists()) {
                file.createNewFile();
            }

            // Append to file
            Files.writeString(path, message + System.lineSeparator(), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
                
        } catch (IOException e) {
            // Silently fail if we can't write to file
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    /**
     * Create a formatted log entry for events
     */
    public static String formatEvent(String eventType, String guildId, String... details) {
        StringBuilder sb = new StringBuilder();
        sb.append("Event[").append(eventType).append("]");
        sb.append(" Guild[").append(guildId).append("]");
        
        for (int i = 0; i < details.length; i += 2) {
            if (i + 1 < details.length) {
                sb.append(" ").append(details[i]).append("[").append(details[i + 1]).append("]");
            }
        }
        
        return sb.toString();
    }

    /**
     * Create a formatted log entry for requests
     */
    public static String formatRequest(String method, String path, String clientIp) {
        return String.format("Request method[%s] path[%s] from[%s]", method, path, clientIp);
    }

    /**
     * Create a formatted log entry for responses
     */
    public static String formatResponse(String method, String path, int statusCode, long durationMs) {
        return String.format("Response method[%s] path[%s] status[%d] duration[%dms]", 
            method, path, statusCode, durationMs);
    }

    /**
     * Clean up old log files
     */
    public static void cleanupOldLogs(String logDirectory, int daysToKeep) {
        try {
            Path dir = Path.of(logDirectory);
            if (!Files.exists(dir)) return;

            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
            
            Files.list(dir)
                .filter(path -> path.toString().endsWith(".log"))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("Deleted old log file: " + path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete log file: " + path);
                    }
                });
                
        } catch (IOException e) {
            System.err.println("Failed to cleanup old logs: " + e.getMessage());
        }
    }
}

