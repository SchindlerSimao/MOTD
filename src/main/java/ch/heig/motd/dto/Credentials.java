package ch.heig.motd.dto;

/**
 * Credentials data transfer object.
 * @param username username
 * @param password password
 */
public record Credentials(String username, String password) { }
