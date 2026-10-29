package mark.warren93.dev.DennyWarriorsAPI.dto;

/**
 * Where each Comet report link currently comes from, without the link itself
 * (it carries an API key). {@code filters} is the link's query string minus
 * the key, e.g. "CompetitionType=12&Status=ACTIVE", so an admin can check
 * which report filters a pasted link overrides.
 */
public record CometLinksResponse(LinkStatus fixtures, LinkStatus standings) {

    public record LinkStatus(String source, String keyHint, String filters) {
    }
}
