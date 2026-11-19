# User API

Owns user accounts, login and refresh tokens, administrator user management,
and the internal user lookup consumed by Hospital. This guide describes the
existing behavior; it does not introduce new permissions or response contracts.

## Code map

Paths below are relative to `src/main/java/com/hospitalmanagement/user/`.

| Package | Responsibility | Start reading here |
| --- | --- | --- |
| `controller` | Public HTTP endpoints | `AuthController`, `UserManagementController` |
| `controller/internal` | Service-to-service HTTP endpoints | `InternalUserController` |
| `service/impl` | Account operations and entity-to-DTO mapping | `AuthServiceImpl`, `UserManagementServiceImpl` |
| `repository`, `entity` | Database access and persisted models | `UserRepository`, `User` |
| `dto` | Request and response contracts | `UserDTO`, `response/UserResponse` |
| `config` | General configuration | `OpenApiConfig`, `PasswordConfig` |
| `security/config` | User and service security filter chains | `UserSecurityConfig`, `InternalApiSecurityConfig` |
| `security/jwt/user` | User JWT signing, verification, roles and properties | `JwtTokenService`, `JwtDecoderConfig` |
| `security/jwt/internal` | Hospital JWT verification and trust properties | `InternalJwtDecoderConfig` |
| `security/jwks` | Diagnostic metadata for JWKS requests | `CallerMetadataInterceptor` |
| `common/exception` | Application errors and HTTP error mapping | `ErrorCode`, `GlobalExceptionHandler` |

## Permissions: who can call what?

`InternalApiSecurityConfig` and `UserSecurityConfig` define the two stateless filter chains. Spring uses the first
matching chain, rather than running a request through both.

| Route | Authentication and authorization |
| --- | --- |
| `/internal/**` | Order 1: dedicated Hospital service-token decoder |
| `GET /internal/users/{id}` | Valid service token mapped to `SERVICE_HOSPITAL` |
| Other internal operations | Denied by the internal chain |
| `POST /auth/register`, `/auth/login`, `/auth/refresh` | No authentication required |
| Health, Swagger and configured `/.well-known/**` routes | No authentication required |
| `/admin/users/**` | User token with `ROLE_ADMIN` |
| Other routes, including `POST /auth/logout` | Authenticated user token |

The internal lookup authorizes a service identity, not an individual user's
ownership of the requested account. It accepts any existing user UUID once
Hospital is authenticated. Any end-user access policy must be enforced by the
calling feature; this endpoint does not compare an end-user ID with `{id}`.

`InternalJwtDecoderConfig` verifies RS256 against Hospital's public keys and
checks the configured issuer, subject, audience, token type, required claims,
timestamps and maximum lifetime. Only after validation does `InternalApiSecurityConfig`
assign `SERVICE_HOSPITAL`. A normal user access token does not grant internal access.

`CallerMetadataInterceptor` adds diagnostic headers to the external user-token
decoder's JWKS requests. `X-Caller-*` headers do not establish identity or grant
permissions. The internal decoder has its own HTTP configuration.

## Follow the internal lookup in code

Example request from Hospital:

```http
GET /internal/users/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <learning-service-token>
```

1. `InternalApiSecurityConfig.internalSecurityFilterChain()` matches `/internal/**`.
2. Spring Security invokes the decoder configured by
   `InternalJwtDecoderConfig.internalJwtDecoder()`. This happens before the controller.
3. The validated service identity receives `SERVICE_HOSPITAL`; the GET rule permits it.
4. `controller/internal/InternalUserController.getUser(id)` delegates to
   `UserManagementService.getInternalUser(id)`.
5. Spring's injected implementation, `UserManagementServiceImpl.getInternalUser(uuid)`,
   calls its private `findUserOrThrow(uuid)` helper.
6. `UserRepository.findById(uuid)` queries the database. An absent user raises
   `BusinessException(USER_NOT_FOUND)`.
7. The service maps the entity to `InternalUserResponse`. The response's
   numeric role comes from the current database record.

Successful internal responses use this shape, without an `APIResponse` envelope.

```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "example-doctor",
    "email": "doctor@example.invalid",
    "isActivated": true,
    "createdAt": "2026-11-01T10:00:00",
    "updatedAt": "2026-11-02T10:00:00",
    "role": 4
  }
}
```

Role codes are USER=1, PATIENT=2, STAFF=3, DOCTOR=4 and ADMIN=5. An inactive
account still returns its current role with `isActivated: false`. Public
administrator and authentication responses retain their existing fields.

For login, follow `AuthController.login()` -> `AuthServiceImpl.login()` ->
`retrieveActivatedAccount()` -> `JwtTokenService.generateAccessToken()` and
`RefreshTokenService.createAndSaveRefreshToken()`. User access tokens identify
an end user; Hospital service tokens are generated by Hospital, not this login endpoint.

## Existing error behavior

| Situation | HTTP status / handling |
| --- | --- |
| Missing or invalid internal service token | 401, handled by Spring Security |
| Authenticated service denied by internal route policy | 403, handled by Spring Security |
| Requested user absent | 404, `USER_NOT_FOUND` |
| Invalid login credentials | 401, `USER_INVALID_CREDENTIALS` |
| Inactive login account | 403, `USER_ACCOUNT_NOT_ACTIVATED` |
| Duplicate registration email or username | 409, corresponding business error |
| Invalid request body or supported validation failure | 400, corresponding request/validation error |
| Unmapped database integrity violation | 400, `INVALID_REQUEST` |
| Unexpected application exception | 500, `INTERNAL_SERVER_ERROR` |

`BusinessException` carries an `ErrorCode`.
`GlobalExceptionHandler.handleBusinessException()` uses that code's status and
returns an `APIResponse` error envelope, even for internal-controller errors.
Security-filter failures use separate handling and should not be assumed to
have the same JSON envelope. The calling service decides how to translate an
upstream error; this project does not control Hospital's error mapping.

## Run and debug locally

Use Java 17 or later compatible with the configured build, Maven Wrapper, a
reachable MySQL database, and the JWKS service. Supply configuration through the
environment or IDE run configuration; `.env` files are not automatically loaded
by Spring Boot. See `.env.example` for deployment examples and [JWT.md](JWT.md)
for signing-key setup. Use host paths for an IDE run and container paths for Docker.

Required application settings include:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_JWK_SET_URI
JWT_ISSUER
APP_SECURITY_SIGNING_KEYS_0_KID
APP_SECURITY_SIGNING_KEYS_0_PRIVATEKEYPATH
APP_SECURITY_SIGNING_KEYS_0_ACTIVE=true
```

Configure `INTERNAL_JWT_JWK_SET_URI` to the Hospital public-key endpoint reachable
from User. Its default uses the Docker hostname `hospital-jwks-api`. The issuer and subject
default to `learning-service`, audience to `user-service`, clock skew to `5s`, and
maximum lifetime to `300s`. These must agree with Hospital's token configuration.
`SERVER_PORT` defaults to `8080`; use distinct host ports when running both services locally.

From this directory in PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

For interactive debugging, start `UserApplication` in the IDE's Debug mode.
Set breakpoints on these executable statements, then call Hospital's lookup endpoint:

| File | Breakpoint statement | Inspect |
| --- | --- | --- |
| `security/jwt/internal/InternalJwtDecoderConfig.java` | `Object kid = jwt.getHeaders().get("kid");` inside the validator lambda | `jwt.getClaims()`, `jwt.getHeaders()` |
| `controller/internal/InternalUserController.java` | `return userManagementService.getUser(id);` | `id` |
| `service/impl/UserManagementServiceImpl.java` | `User user = findUserOrThrow(uuid);` | `uuid` |
| Same file, private helper | `if (optionalUser.isEmpty())` | Database result in `optionalUser` |
| Same file, `getUser` | `return new UserResponse(new UserDTO(` | Entity being mapped |

The decoder's bean-construction statements run at startup; its validator lambda
runs during authentication. Signature failures can occur before that lambda.
Resume Hospital to send its HTTP request, then switch to User's debugger; stepping
cannot cross JVMs. Increase Hospital's local read timeout while pausing User and
allow for service-token expiry while inspecting authentication.

Existing regression tests include `InternalJwtAuthenticationTest` for token and
security-chain isolation, `InternalUserControllerTest` for the internal endpoint,
and `JwtDecoderConfigTest` for external user-token verification.
