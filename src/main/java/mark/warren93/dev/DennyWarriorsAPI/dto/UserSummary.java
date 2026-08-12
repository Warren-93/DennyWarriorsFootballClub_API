package mark.warren93.dev.DennyWarriorsAPI.dto;

import mark.warren93.dev.DennyWarriorsAPI.model.Role;
import mark.warren93.dev.DennyWarriorsAPI.model.User;

public record UserSummary(String id, String username, Role role) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getUserName(), user.getRole());
    }
}
