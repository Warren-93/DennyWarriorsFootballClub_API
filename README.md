# DennyWarriorsFootballClub_API

Spring Boot API for the club website. Deployment steps are in
[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md).

## Lombok conventions

Lombok generates boilerplate at compile time. Project-wide settings live in
[`lombok.config`](lombok.config). Use the annotation set for the kind of class
you're writing:

| Kind of class | Annotations | Why |
| --- | --- | --- |
| Mongo `@Document` models | `@Getter @Setter @NoArgsConstructor @ToString` | Not `@Data`: its `equals`/`hashCode` cover every mutable field, including the id Mongo assigns on save. Mark secrets with `@ToString.Exclude`. |
| Nested or JSON value types (`MatchResult`, `Fixture.MatchEvent`, Comet response wrappers) | `@Data @NoArgsConstructor` | Plain data holders, where value equality is what you want. |
| `@ConfigurationProperties` classes | `@Getter @Setter` | Spring binds through setters. No `toString`, so config values (some hold API keys) can't end up in logs. |
| Services, controllers, filters, `@Configuration` | `@RequiredArgsConstructor` on `private final` dependencies | Constructor injection without hand-written constructors. Field order is the constructor's parameter order. |
| Logging | `@Slf4j` (or `@Slf4j(topic = "...")` for a named logger) | Replaces `LoggerFactory.getLogger(...)`. |
| Request/response DTOs | Java `record`s, no Lombok | Records are the language's built-in immutable data type and replace Lombok's `@Value`. |

Write the constructor by hand when it does more than assign fields, such as
binding `@Value` properties or validating input (`JwtService`,
`LoginRateLimiter`, `AdminUserSeeder`). Exceptions also keep their explicit
`super(message)` constructors.

Avoid `@AllArgsConstructor` on models: with 20 fields it's easy to pass two
values of the same type in the wrong order. `lombok.config` flags it with a
warning.
