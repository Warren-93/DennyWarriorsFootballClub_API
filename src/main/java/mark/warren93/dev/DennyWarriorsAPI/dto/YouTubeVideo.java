package mark.warren93.dev.DennyWarriorsAPI.dto;

public record YouTubeVideo(
        String videoId,
        String title,
        String thumbnailUrl,
        String watchUrl,
        String publishedAt) {
}
