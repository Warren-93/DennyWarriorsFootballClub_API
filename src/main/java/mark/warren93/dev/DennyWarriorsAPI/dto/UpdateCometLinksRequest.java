package mark.warren93.dev.DennyWarriorsAPI.dto;

/**
 * Per link: null leaves it unchanged, blank clears it (back to the
 * environment variable), anything else replaces it.
 */
public record UpdateCometLinksRequest(String fixturesUrl, String standingsUrl) {
}
