# JWT signing and verification

For the package map, endpoint permissions, error behavior, and a step-by-step
internal lookup walkthrough, start with [README.md](README.md).

## Signing

`SigningKeyProperties` binds `app.security.signing.keys`. Docker Compose loads `.env.user-api` into the User container and maps root deployment settings explicitly. For example:

```dotenv
APP_SECURITY_SIGNING_KEYS_0_KID=user-rsa-v1
APP_SECURITY_SIGNING_KEYS_0_PRIVATEKEYPATH=/run/keys/jwt/userservice-private-v1.pem
APP_SECURITY_SIGNING_KEYS_0_ACTIVE=false
APP_SECURITY_SIGNING_KEYS_1_KID=user-rsa-v2
APP_SECURITY_SIGNING_KEYS_1_PRIVATEKEYPATH=/run/keys/jwt/userservice-private-v2.pem
APP_SECURITY_SIGNING_KEYS_1_ACTIVE=true
```

Configure only private keys owned by the user service. Keys must be PKCS#8 RSA
PEM files (`BEGIN PRIVATE KEY`). All configured files must exist; exactly one
key must be active, and key IDs must be unique. The provider loads them at
startup, so restart the application after changing keys or configuration.

`JwtTokenService` signs with the active key. `JWT_ISSUER` and
`JWT_ACCESS_TOKEN_TTL_SECONDS` supply the issuer and access-token lifetime through
`application.properties`. The token format and claims are unchanged.

The old `app.security.jwks.keys` configuration is no longer used by this service.
The old `JWT_KEY_ID`, `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`, and
`JWT_KEY_ACTIVE` placeholders have been removed. For an IDE run, supply the
signing variables explicitly and use host filesystem paths.

## Verification

`JwtDecoderConfig` uses `JWT_JWK_SET_URI` to fetch and cache public keys. The user
service selects a verification key using the token's `kid`, verifies RS256
locally, and validates the configured issuer and timestamps. Roles are then
handled by the existing security configuration. The token is not sent to JWKS.

JWKS requests use Spring Boot's managed HTTP client builder so configured tracing
continues to propagate. Each request sends `X-Caller-Service`, `X-Caller-Instance`,
`X-Caller-Version`, and the current MDC `X-Correlation-ID` (a new UUID when absent
or invalid). The correlation filter runs before security, making the request's
correlation ID available during JWT verification.

Override caller identity with `JWKS_CALLER_SERVICE`, `JWKS_CALLER_INSTANCE`, and
`JWKS_CALLER_VERSION`. Defaults are `spring.application.name` (`user-learning`),
`HOSTNAME` (or a generated instance UUID), and the Maven project version.
Values must be 1–64 ASCII letters/digits, dots, underscores or hyphens, starting
with a letter/digit; invalid identity values are sent as `unknown`.

The current decoder trusts the single issuer configured by `JWT_ISSUER`.
Publishing additional services' public keys does not grant trust to their
issuers. Multi-issuer support would need an explicit issuer-to-key-source trust
configuration; it is not enabled here.

The external JWKS must publish the public half of the active signing key with
the same `kid`. Publish a new public key before switching signing keys and keep
old public keys until their tokens expire (including allowed clock skew).

## Isolated internal authentication

`InternalApiSecurityConfig` orders the `/internal/**` chain first; `UserSecurityConfig` supplies the external chain second.
`InternalJwtDecoderConfig` trusts only the configured Learning JWKS URI, issuer and subject
`learning-service`, audience `user-service`, RS256 and `typ=internal+jwt`. It requires `kid`,
`jti`, `iat`, `exp`, validates optional `nbf`, rejects future issuance beyond clock skew and
limits token lifetime. Nimbus default type validation is explicitly disabled only on this
internal decoder and replaced by the exact type check. External JWT validation is unchanged.

An authenticated Learning identity receives only `SERVICE_LEARNING`; it never receives User
or Admin roles. Only GET `/internal/users/*` is authorized. Missing/invalid tokens return 401,
valid identities denied by route policy return 403. Both chains are stateless.

Signing keys are mounted as individual private files into User only. JWKS receives only the
public PEMs. Canonical indexed environment names use `PRIVATEKEYPATH`. The first-key legacy
placeholder remains accepted as a migration fallback, but additional keys must use canonical names.

See root `JWT-AUTHENTICATION.md` for variables, Docker paths and consumer migration.
