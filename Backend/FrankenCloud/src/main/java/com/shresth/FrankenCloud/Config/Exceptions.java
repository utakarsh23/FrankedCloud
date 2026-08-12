package com.shresth.FrankenCloud.Config;

public class Exceptions {

    public static class DriveNotFoundException extends RuntimeException {
        public DriveNotFoundException() {
            super("No active drive accounts found.");
        }

        public DriveNotFoundException(String message) {
            super(message);
        }
    }

    public static class FileNotFoundException extends RuntimeException {
        public FileNotFoundException() {
            super("File not found.");
        }

        public FileNotFoundException(String message) {
            super(message);
        }
    }

    public static class InsufficientStorageException extends RuntimeException {
        public InsufficientStorageException() {
            super("Insufficient storage available.");
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() { super("Invalid credentials."); }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException() { super("Invalid token."); }
    }

    public static class FileMetadataFailedException extends RuntimeException {
        public FileMetadataFailedException() { super("Failed to save file metadata."); }
    }

    public static class UnauthorizedAccessException extends RuntimeException {
        public UnauthorizedAccessException() { super("Unauthorized access."); }
    }
}