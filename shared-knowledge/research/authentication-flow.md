---
name: authentication-flow-research
description: Complete authentication flow analysis including JWT token lifecycle, refresh token rotation, and session management
metadata:
  type: research
---

# Authentication Flow Research

**Date Researched:** 2026-10-20  
**Research Scope:** Java backend (milky_way-java) authentication system  
**Status:** Facts and Assumptions documented; limited test execution due to Maven POM error

## Executive Summary

The Milky Way application implements a multi-layered authentication system combining JWT (JSON Web Tokens), refresh token rotation, session management, and OAuth2 social login. The system is built primarily on Spring Security with JwtTokenProvider managing token generation/validation, TokenBlacklistService for token invalidation, and SessionService for user session lifecycle.

**Key Findings:**
- Access tokens (3600s expiry) and refresh tokens (604800s expiry) are both JWT-based, distinguished by a `type` claim
- Refresh token rotation is enforced: each refresh invalidates the old refresh token and issues a new one
- Tokens are stored in Redis for quick lookup; refresh tokens are also stored in the database
- Session invalidation at login ensures only one active session per user
- Comprehensive test coverage exists for refresh token rotation with 10+ test cases

---

## Architecture Overview

### Multi-Backend Setup
The project has multiple backend implementations:
- **Java Backend** (milky_way-java): Main authentication service - **FOCUS OF THIS RESEARCH**
- **PHP Backend** (milky_way-php): Laravel-based, proxies authentication requests to Java API via AuthService (GuzzleHttp)
- **Python Backend** (milky_way-python): Venv-based, not analyzed in this research
- **React Frontend** (milky_way-react): Client-side consumer, not analyzed in this research

PHP proxies auth calls to Java's `/api/v1/auth` endpoints (configured at `JAVA_API_URL` environment variable).

---

## Authentication Flow - Request to Response

### 1. LOGIN FLOW

#### 1.1 Request Entry Point
**File:** `milky_way-java/src/main/java/milky_way/controller/AuthController.java:98-112`

```
POST /api/v1/auth/login
Content-Type: application/json
Body: {
  "email": "user@example.com",
  "password": "password123"
}
```

- **Class:** `AuthController`
- **Method:** `login(@RequestBody UserRequestLogin userRequest, HttpServletResponse response)`
- **Return Type:** `ResponseEntity<?>`

#### 1.2 Controller → Service Delegation
**File:** `AuthController.java:101`
```java
Optional<LoginResponse> authResponse = authService.loginUser(userRequest, response);
```

#### 1.3 Service Layer Processing
**File:** `milky_way-java/src/main/java/milky_way/service/auth/AuthService.java:172-200`
**Method:** `loginUser(UserRequestLogin userRequest, HttpServletResponse response): Optional<LoginResponse>`

**Flow Steps:**

| Step | Action | Detail |
|------|--------|--------|
| 1 | Find User | `userRepository.findByEmail(userRequest.getEmail())` - throws `EMAIL_NOT_EXIST` if not found |
| 2 | Validate Password | `passwordEncoder.matches(userRequest.getPassword(), user.get().getPassword())` - throws `PASSWORD_INCORRECT` if mismatch |
| 3 | Check Deletion | Verify `user.getDeleteDate() == null` - throws `USER_NOT_FOUND` if deleted |
| 4 | Fetch Permissions | Extract user roles and fetch permissions from `PermissionService.findPermissionsByRolesIn(roleIds)` |
| 5 | Generate Access Token | `jwtTokenProvider.generateToken(userId, email, roles, permissions)` - type: "access", expiry: 3600s |
| 6 | Generate Refresh Token | `jwtTokenProvider.generateRefreshToken(userId, email, roles, permissions)` - type: "refresh", expiry: 604800s (7 days) |
| 7 | Store in Redis | `redisTokenService.saveAccessToken(email, accessToken)` + `saveRefreshToken(email, refreshToken)` |
| 8 | Store in Database | Delete existing refresh tokens: `tokenService.deleteRefreshTokens(user)` then save new one: `tokenService.saveRefreshToken(user, refreshToken)` |
| 9 | Invalidate Old Sessions | `sessionService.invalidateAllSessions(user.getEmail())` |
| 10 | Create New Session | `sessionService.createSession(user)` - expires 1 hour from now |
| 11 | Set Cookie | `jwtAuthenticationUtils.addRefreshTokenCookie(refreshToken, response)` - HttpOnly, Secure, Path: /api/v1/auth/, MaxAge: 7 days |

**Return:** `LoginResponse` with:
- `accessToken`: JWT string
- `sessionResponse`: { id, createdDate, expiresDate }

#### 1.4 Response to Client
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "sessionResponse": {
    "id": "session-uuid",
    "createdDate": "2026-10-20T12:00:00",
    "expiresDate": "2026-10-20T13:00:00"
  }
}
```

Refresh token is set as HttpOnly cookie automatically by Spring.

---

### 2. REFRESH TOKEN ROTATION FLOW

#### 2.1 Request Entry Point
**File:** `AuthController.java:114-124`

```
POST /api/v1/auth/refresh
Cookie: refreshToken=<old-token>
Authorization: Bearer <access-token> (optional, but endpoint requires authentication via @PreAuthorize)
```

- **Endpoint:** `/api/v1/auth/refresh`
- **Security:** Requires authenticated user (`@PreAuthorize("isAuthenticated()")`)
- **Authentication Method:** JwtAuthenticationFilter validates the Authorization Bearer token or refreshToken cookie

#### 2.2 JWT Authentication Filter
**File:** `milky_way-java/src/main/java/milky_way/config/JwtAuthenticationFilter.java:42-59`

**Public Endpoints** (bypass filter):
- `/api/v1/auth/register`
- `/api/v1/auth/login`
- `/api/v1/auth/send-reset-password-email`
- `/api/v1/auth/verify-otp`
- `/api/v1/auth/reset-password`
- `/api/v1/auth/oauth2/**` (all OAuth2 callbacks)

**For protected endpoints like `/refresh`:**
1. Extract JWT from `Authorization: Bearer <token>` header
2. Fall back to `refreshToken` cookie if no Bearer token
3. Validate token with `jwtTokenProvider.validateToken(token)`:
   - Check if token is blacklisted
   - Validate signature with secret key
   - Check expiration
4. Extract email from JWT claims and load user details
5. Set `SecurityContextHolder.getContext().setAuthentication(...)` to mark request as authenticated

#### 2.3 Refresh Service - Token Rotation
**File:** `AuthService.java:202-240`
**Method:** `refreshToken(HttpServletRequest request, HttpServletResponse response): RefreshResponse`

**Validation & Extraction:**
```java
String refreshToken = jwtAuthenticationUtils.extractRefreshTokenFromCookie(request);
if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken) || 
    !jwtTokenProvider.isRefreshToken(refreshToken)) {
  throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
}
```

**Core Rotation Steps:**

| Step | Action | Code Reference |
|------|--------|-----------------|
| 1 | Extract refresh token from cookie | `jwtAuthenticationUtils.extractRefreshTokenFromCookie(request)` |
| 2 | Validate token (not expired, not blacklisted, is refresh type) | `jwtTokenProvider.validateToken()` + `isRefreshToken()` |
| 3 | Extract email & ID from token | `jwtTokenProvider.getEmailFromJWT()` + `getIdFromJWT()` |
| 4 | Fetch user by email & ID | `userRepository.findByEmailAndId(email, id)` |
| 5 | Validate user exists & not deleted | Check `user.isEmpty()` and `user.get().getDeleteDate() == null` |
| 6 | **BLACKLIST OLD TOKEN** | `tokenBlacklistService.blacklistToken(refreshToken, expiryDate)` - stored in TokenBlacklist table with expiry info |
| 7 | Generate NEW access token | `jwtTokenProvider.generateToken(id, email, roles, permissions)` |
| 8 | Generate NEW refresh token | `jwtTokenProvider.generateRefreshToken(id, email, roles, permissions)` |
| 9 | Fetch/create session | Get existing session or create new one with 1-hour expiry |
| 10 | Set new refresh token cookie | `jwtAuthenticationUtils.addRefreshTokenCookie(newRefreshToken, response)` |
| 11 | Return new tokens | `RefreshResponse { accessToken, sessionResponse }` |

**Return:** `RefreshResponse` with new access token and current session info

**Critical Detail:** Each use of refresh token invalidates it and creates a new one. Tokens in the blacklist cannot be used again.

---

### 3. PROTECTED ENDPOINT ACCESS FLOW (e.g., GET /api/v1/auth/profile)

#### 3.1 Request Entry
```
GET /api/v1/auth/profile
Authorization: Bearer <access-token>
```

#### 3.2 JwtAuthenticationFilter Processing
**File:** `JwtAuthenticationFilter.java:42-89`

1. Check if endpoint is public (no, `/profile` is not in the list)
2. Extract JWT from `Authorization` header: `request.getHeader("Authorization").substring(7)`
3. Validate token: `jwtTokenProvider.validateToken(token)`
   - Check blacklist
   - Check signature
   - Check expiration
4. If valid: Parse claims and set authentication context
5. If invalid: Return 401 Unauthorized

#### 3.3 Service Layer (Profile Fetch)
**File:** `AuthService.java:252-273`
**Method:** `getProfile(HttpServletRequest request): UserProfileResponse`

| Step | Action |
|------|--------|
| 1 | Extract access token from Authorization header |
| 2 | Validate it's an access token (not refresh) |
| 3 | Extract email & ID from token claims |
| 4 | Verify session is valid (exists and not expired) |
| 5 | Fetch user from database by email & ID |
| 6 | Verify user not deleted |
| 7 | Validate email matches one of user's email fields (email, googleEmail, or facebookEmail) |
| 8 | Return `UserProfileResponse` with user data |

---

### 4. LOGOUT FLOW

#### 4.1 Request Entry
**File:** `AuthController.java:126-135`

```
POST /api/v1/auth/logout
Authorization: Bearer <access-token>
Cookie: refreshToken=<refresh-token>
```

#### 4.2 Service Processing
**File:** `AuthService.java:242-250`
**Method:** `logout(HttpServletRequest request, HttpServletResponse response)`

| Step | Action | Detail |
|------|--------|--------|
| 1 | Extract & validate access token | `extractAndValidateAccessToken(request)` - must be valid access token |
| 2 | Blacklist access token | `tokenBlacklistService.blacklistToken(accessToken, expiryDate)` |
| 3 | Invalidate session | `sessionService.invalidateSession(email)` - deletes session record |
| 4 | Invalidate from Redis | `redisTokenService.invalidateToken(accessToken, null)` |
| 5 | Extract refresh token from cookie | `jwtAuthenticationUtils.extractRefreshTokenFromCookie(request)` |
| 6 | Process refresh token (if exists) | `processRefreshToken(refreshToken, response)` |
|    | - Blacklist refresh token | Calls `tokenBlacklistService.blacklistToken()` |
|    | - Delete refresh token cookie | Calls `jwtAuthenticationUtils.deleteRefreshTokenCookie()` (sets MaxAge=0) |

**Response:** `200 OK` with message "Logout successful"

---

## JWT Token Structure

### Access Token
**File:** `JwtTokenProvider.java:37-71`

```
Header: {
  "alg": "HS512"
}

Claims: {
  "sub": "user@example.com",          // email
  "roles": "[\"USER\"]",               // JSON array string
  "permissions": "[\"USER_READ\", ...]", // JSON array string
  "type": "access",                    // discriminator
  "id": "user-uuid",                   // user ID
  "jti": "uuid",                       // JWT ID (unique)
  "iat": 1703087200,                   // issued at
  "exp": 1703090800                    // expires at (now + 3600s)
}

Signature: HMAC-SHA512(header.payload, jwt.signerKey)
```

**Expiration:** Configured at `jwtExpirationInSeconds = 3600` (1 hour)

### Refresh Token
**File:** `JwtTokenProvider.java:73-107`

Same structure as access token, but:
- `"type": "refresh"` instead of "access"
- **Expiration:** Configured at `jwtRefreshExpirationInSeconds = 604800` (7 days)
- Stored in both:
  - **Redis** (for quick lookup by email)
  - **Database** (table: `tokens`, columns: `refresh_token`, `user_id`, `createDate`, `updatedDate`)

---

## Token Validation & Blacklisting

### Validation Flow
**File:** `JwtTokenProvider.java:173-185`
**Method:** `validateToken(String authToken): boolean`

```java
public boolean validateToken(String authToken) {
  try {
    if (isTokenBlacklisted(authToken)) {        // Check blacklist first
      return false;
    }
    Jwts.parser()
        .setSigningKey(jwtSecret)
        .parseClaimsJws(authToken);              // Validate signature & expiration
    return true;
  } catch (Exception e) {
    return false;
  }
}
```

### Blacklist Storage
**File:** `TokenBlacklist.java`

| Column | Type | Details |
|--------|------|---------|
| `id` | UUID | Primary key |
| `token` | TEXT | Full JWT string (unique) |
| `expiryDate` | LocalDateTime | Expiration time of the token (for cleanup) |
| `createdDate` | LocalDateTime | When token was blacklisted |

**Cleanup:** `TokenBlacklistRepository.deleteByExpiryDateBefore(LocalDateTime.now())` removes expired entries.

---

## Session Management

### Session Entity
**File:** `Session.java`

| Field | Type | Purpose |
|-------|------|---------|
| `id` | UUID | Session identifier |
| `createdDate` | LocalDateTime | When session was created |
| `expiresDate` | LocalDateTime | Session expiration (1 hour from creation) |
| `users` | User (FK) | Owner of the session |

### Session Lifecycle
**File:** `SessionService.java`

| Operation | Method | Behavior |
|-----------|--------|----------|
| Create | `createSession(User user)` | Creates new session with 1-hour TTL |
| Validate | `isSessionValid(String email)` | Checks if session exists and not expired |
| Fetch | `getSession(String email)` | Gets active session for user (ignores expired ones) |
| Invalidate | `invalidateSession(String email)` | Deletes single session for user |
| Invalidate All | `invalidateAllSessions(String email)` | Deletes all sessions for user (enforces single-session policy) |

**Single Session Policy:** When a user logs in, all previous sessions are invalidated, ensuring only one active session per user.

---

## OAuth2 Social Login Flows

### Google OAuth2 Login
**File:** `AuthController.java:176-180`
**Files:** `AuthService.java:275-328`

**Flow:**
1. Client initiates: `GET /api/v1/auth/oauth2/google?code=<auth-code>`
2. AuthService exchanges code for Google access token via Google's token endpoint
3. Fetches user info from Google's userinfo endpoint with the access token
4. Finds or creates user with `googleEmail` (dedicated field, not main email)
5. If user exists: Links Google account if not already linked
6. Generates access & refresh tokens (same as regular login)
7. Creates session and returns `LoginGoogleResponse { accessToken, sessionResponse, googleId }`

**Database Fields for Google OAuth:**
- `User.googleEmail`: Email from Google OAuth
- `User.googleId`: Encrypted Google user ID (using `passwordEncoder.encode()`)

### Facebook OAuth2 Login
**File:** `AuthService.java:407-493`

Similar flow to Google but:
- Creates or finds user with `facebookEmail` (dedicated field)
- Fetches user avatar from Facebook's picture endpoint
- Stores `User.facebookId` (encrypted)
- Returns `LoginFacebookResponse`

**Account Linking Logic:**
- If regular user exists with same email and logs in via Facebook: Automatically links Facebook account
- Cross-provider linking checks: Prevents linking mismatched accounts (raises `LINKED_ACCOUNT_MISMATCH`)

---

## Cookie Management

### Refresh Token Cookie
**File:** `JwtAuthenticationUtils.java:28-35`

**Cookie Properties:**
```
Name: refreshToken
Value: <JWT>
HttpOnly: true          // Not accessible via JavaScript
Secure: true            // HTTPS only
Path: /api/v1/auth/     // Only sent to auth endpoints
MaxAge: 604800 seconds  // 7 days
SameSite: (not explicitly set, browser default)
```

**Deletion:** `deleteRefreshTokenCookie()` sets `MaxAge=0` and sends the cookie again to overwrite it.

---

## Error Handling

### Error Codes & Scenarios
**File:** `ErrorCode.java` (enumeration)

| Error Code | HTTP Status | Scenario | Thrown From |
|------------|------------|----------|------------|
| EMAIL_EXIST | 400 | User with email already registered | `registerUser()` |
| EMAIL_NOT_EXIST | 404 | Email not found in system | `loginUser()`, `getProfile()` |
| PASSWORD_INCORRECT | 401 | Password doesn't match | `loginUser()` |
| INVALID_REFRESH_TOKEN | 401 | Refresh token invalid/expired/blacklisted | `refreshToken()` |
| INVALID_TOKEN | 401 | Access token invalid | `getProfile()` |
| SESSION_INVALID | 401 | Session expired or doesn't exist | `getProfile()` |
| ACCOUNT_LOCKED | 403 | User account disabled | `JwtAuthenticationFilter.handleToken()` |
| UNAUTHORIZED_ACCESS | 403 | User trying to access other user's data | `getProfile()` |
| USER_NOT_FOUND | 404 | User deleted or doesn't exist | Multiple endpoints |

---

## Files Analyzed - Summary

### Controllers
- **AuthController.java** (262 lines) - All auth endpoints: register, login, refresh, logout, profile, OAuth2, password reset

### Services
- **AuthService.java** (701 lines) - Core business logic for all auth operations
- **SessionService.java** (65 lines) - Session lifecycle management
- **TokenService.java** (43 lines) - Refresh token database storage
- **TokenBlacklistService.java** (38 lines) - Token invalidation management

### Utilities & Configuration
- **JwtTokenProvider.java** (216 lines) - JWT generation, validation, claims extraction
- **JwtAuthenticationFilter.java** (91 lines) - Request-level JWT validation
- **JwtAuthenticationUtils.java** (45 lines) - Cookie management helpers

### Data Models
- **Token.java** - Refresh token storage (table: tokens)
- **TokenBlacklist.java** - Invalidated token storage (table: token_blacklists)
- **Session.java** - User session storage (table: sessions)

### Repositories
- **TokenRepository.java** - Find/delete refresh tokens by user
- **TokenBlacklistRepository.java** - Find/delete blacklisted tokens
- **SessionRepository.java** - CRUD operations on sessions

### DTOs (Request/Response)
- **UserRequestLogin.java** - { email, password }
- **LoginResponse.java** - { accessToken, sessionResponse }
- **RefreshResponse.java** - { accessToken, sessionResponse }
- **LoginGoogleResponse.java** - { accessToken, sessionResponse, googleId }
- **LoginFacebookResponse.java** - { accessToken, sessionResponse, facebookId }

### Configuration
- **application.properties** - JWT expiry times, Redis, OAuth2 provider configs

### Tests
- **AuthServiceRefreshTokenTest.java** (280 lines) - 10 comprehensive refresh token rotation tests
- **AuthControllerRefreshTokenTest.java** (64 lines) - 2 controller-level tests

---

## Test Coverage Analysis

### AuthServiceRefreshTokenTest.java
**File:** `src/test/java/milky_way/service/auth/AuthServiceRefreshTokenTest.java`

**Test Count:** 10 tests (labeled A-J)

| Test ID | Test Name | Fact/Assumption | Status |
|---------|-----------|-----------------|--------|
| A | `refreshToken_withValidRefreshToken_returnsNewAccessToken` | **Fact**: Valid refresh returns new access token with correct email/ID | Cannot verify (Maven POM error) |
| B | `refreshToken_withValidRefreshToken_setsNewRefreshTokenCookie` | **Fact**: New refresh token is set in cookie with correct properties (HttpOnly, Secure, Path, MaxAge) | Cannot verify |
| C | `refreshToken_withValidRefreshToken_blacklistsOldRefreshToken` | **Fact**: Old token is added to blacklist table with correct expiry | Cannot verify |
| D | `refreshToken_withAlreadyRotatedRefreshToken_isRejected` | **Fact**: Attempting to reuse rotated token (now blacklisted) fails with INVALID_REFRESH_TOKEN | Cannot verify |
| E | `refreshToken_withRotatedNewRefreshToken_canRefreshAgain` | **Fact**: New token from rotation can be used for next refresh; old token remains blacklisted | Cannot verify |
| F | `refreshToken_withoutRefreshTokenCookie_isRejected` | **Fact**: Missing or wrong cookie name returns INVALID_REFRESH_TOKEN | Cannot verify |
| G | `refreshToken_withAccessToken_isRejected` | **Fact**: Using access token instead of refresh token is rejected | Cannot verify |
| H | `refreshToken_withExpiredRefreshToken_isRejected` | **Fact**: Expired tokens fail validation | Cannot verify |
| I | `refreshToken_withBlacklistedRefreshToken_isRejected` | **Fact**: Pre-blacklisted tokens are rejected | Cannot verify |
| J | `refreshToken_withMalformedToken_isRejected` | **Fact**: Malformed JWT format, invalid signature, different secret key all fail | Cannot verify |

**Test Setup Highlights:**
- Mocks: UserRepository, TokenBlacklistRepository, SessionService, PermissionService
- Real implementations: JwtTokenProvider, JwtAuthenticationUtils, TokenBlacklistService
- Secret key: Random 64-byte secret per test
- Expiration times: 3600s (access), 604800s (refresh)
- Assertions: Validate token type, email, ID, blacklist status, cookie properties, JTI uniqueness

---

## Facts vs. Assumptions

### FACTS (Verified from Source Code)

1. **JWT Structure**: Access and refresh tokens include `type` claim ("access"/"refresh") to distinguish them
2. **Refresh Token Rotation**: Each refresh operation blacklists the old token and issues a new one
3. **Token Storage**: Refresh tokens stored in both Redis (for speed) and database (for persistence)
4. **Session Policy**: User's old sessions invalidated on login (enforces single active session)
5. **Blacklist Table**: TokenBlacklist stores full JWT and expiry date for validation
6. **Cookie Security**: Refresh token cookie is HttpOnly, Secure, Path-restricted
7. **Token Expiry**: 3600s (1 hour) for access tokens, 604800s (7 days) for refresh tokens
8. **Validation Chain**: Tokens checked against blacklist before signature validation
9. **OAuth2 Fields**: Separate email fields (googleEmail, facebookEmail) for social login users
10. **Public Endpoints**: /register, /login, password reset, OAuth2 callbacks bypass JWT filter

### ASSUMPTIONS (Not Fully Verified)

1. **Redis Performance**: Redis is available and performant for token storage (configuration at application.properties:18-20, but no testing of failure modes)
2. **Token Cleanup**: Automatic cleanup of expired tokens from blacklist occurs (method exists: `removeExpiredTokens()`, but no indication of when/how it's scheduled)
3. **Cookie Default Behavior**: Assuming SameSite default is sufficient (not explicitly configured)
4. **Concurrent Requests**: Assumption that database and Redis stay in sync under high concurrency (no explicit locking observed)
5. **Email Field Uniqueness**: Assuming email fields are unique across regular and OAuth2 users (business logic indicates it, but no database constraint check)
6. **Token JTI Uniqueness**: Assuming UUID.randomUUID() provides sufficient uniqueness for JTI (standard Java practice, not verified)

---

## Open Questions & Risks

### Missing Implementation Details

1. **Token Cleanup Scheduling**: 
   - `TokenBlacklistService.removeExpiredTokens()` exists but no indication of when it runs
   - **Risk**: Blacklist table could grow unbounded if cleanup is not scheduled
   - **Recommendation**: Verify if scheduled task is configured (likely via `@Scheduled` annotation or Spring Task)

2. **Redis Failure Handling**:
   - No try-catch around Redis operations in AuthService
   - **Risk**: Redis outage could cause login failures
   - **Recommendation**: Check RedisService implementation for fallback behavior

3. **OAuth2 Account Linking Edge Cases**:
   - `linkGoogleOAuthProviderIfNecessary()` throws `LINKED_ACCOUNT_MISMATCH` if IDs don't match
   - **Question**: What if user logs in via Google, then later via Facebook with same email? Both providers linked?
   - **Recommendation**: Document intended behavior and verify with test scenarios

4. **Session Expiration Enforcement**:
   - Session created with 1-hour TTL, but no automatic cleanup observed
   - **Question**: Are old sessions cleaned up by cron job or database cleanup?
   - **Risk**: Session table could grow with stale entries

5. **Refresh Token Invalidation Race Condition**:
   - Two simultaneous refresh requests with same token: first blacklists it, second gets invalid token error
   - **Question**: Is this desired (preventing token reuse) or a bug?
   - **Recommendation**: Verify this is tested (likely covered by test D or I)

6. **Access Token in Cookie Fallback**:
   - `JwtAuthenticationFilter.getJwtFromRequest()` tries Bearer token first, then falls back to refresh token cookie
   - **Question**: Is this intentional? Could allow using refresh token as access token bypass?
   - **Recommendation**: Verify refresh token type checking is always enforced

### Configuration Concerns

1. **Secret Key Storage**: 
   - `jwt.signerKey` stored in plaintext in application.properties (visible in source)
   - **Risk**: If source code is exposed, secret is compromised
   - **Recommendation**: Use environment variable or Spring Cloud Config

2. **HTTPS Enforcement**:
   - Cookie marked as `Secure`, but no indication server enforces HTTPS-only
   - **Risk**: Could be bypassed if server runs on HTTP

3. **CORS Configuration**:
   - `@CrossOrigin(origins = "http://localhost:8000")` hardcoded in AuthController
   - **Risk**: Only localhost:8000 allowed, production would need reconfiguration

### Database Constraints

1. **Refresh Token Uniqueness**: 
   - TokenBlacklist.token is marked unique, ensuring no duplicates
   - But Token.refreshToken is not unique - multiple rows could have same token
   - **Risk**: Database consistency issue

2. **User Deletion Handling**:
   - Soft delete via `deleteDate` field
   - Tokens/sessions of deleted users not automatically cleaned up
   - **Risk**: Deleted users' tokens still valid until blacklist cleanup

---

## Verification Status

### Tests Available for Verification
- ✅ **10 comprehensive refresh token tests** exist (AuthServiceRefreshTokenTest.java)
- ✅ **2 controller-level tests** for refresh endpoint (AuthControllerRefreshTokenTest.java)
- ❌ **Cannot Execute**: Maven POM has duplicate repository IDs (line 37 and 41), prevents `mvn test`

**POM Error Details:**
```
ERROR: 'repositories.repository.id' must be unique: repository-id
   location: line 41, column 8
```

Both `<id>repository-id</id>` entries in repositories section are identical, causing build failure.

### Recommended Verification Steps (for other developers)
1. Fix Maven POM by renaming one duplicate repository ID
2. Run: `mvn test -Dtest=AuthServiceRefreshTokenTest`
3. Run: `mvn test -Dtest=AuthControllerRefreshTokenTest`
4. Run with coverage: `mvn clean jacoco:prepare-agent test jacoco:report`

---

## Integration Points

### PHP Backend Integration
**File:** `milky_way-php/app/Services/AuthService.php`

PHP backend proxies auth via Guzzle HTTP client:
- PHP receives register/login requests
- PHP forwards to Java API via POST to `JAVA_API_URL/api/v1/auth/register` or `...login`
- PHP returns Java's response to client

**Implication**: PHP does not authenticate locally; all auth is delegated to Java.

### Frontend Integration (Inferred)
Based on token structure and cookie usage:
1. **Login**: Frontend receives `accessToken` in response + `refreshToken` in HttpOnly cookie
2. **API Calls**: Frontend sends `Authorization: Bearer <accessToken>` header for protected endpoints
3. **Refresh**: Frontend sends Cookie with `refreshToken` implicitly; backend returns new access token
4. **Logout**: Frontend calls logout endpoint; refresh token cookie is deleted

---

## Configuration Reference

### JWT Configuration (application.properties)
```properties
jwt.signerKey=8xQocsla3cNidwIzG2amdrWOyyhJE5pkN2nw7odRkk...
jwtExpirationInSeconds=3600
jwtRefreshExpirationInSeconds=604800
```

### Redis Configuration (application.properties)
```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.timeout=5000
```

### Database Configuration (application.properties)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/milky_way
spring.datasource.username=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### OAuth2 Providers (application.properties)
- **Google**: client-id, client-secret, authorization-uri, token-uri, user-info-uri
- **Facebook**: Similar configuration for Facebook OAuth2

---

## Comparison with Common Patterns

### Strengths
✅ Refresh token rotation enforces token compromise recovery  
✅ Separate token types (access/refresh) via claims  
✅ Token blacklist prevents reuse of invalidated tokens  
✅ Single session per user prevents concurrent session attacks  
✅ HttpOnly cookies prevent XSS token theft  
✅ Social login with account linking support  
✅ Comprehensive test coverage for refresh flow  

### Potential Improvements
⚠️ Token cleanup scheduling not explicit in code  
⚠️ Redis dependency not error-handled  
⚠️ No rate limiting on login/refresh endpoints  
⚠️ Session table not automatically cleaned  
⚠️ Secret key stored in source code  
⚠️ CORS hardcoded to localhost  

---

## References for Code Inspection

### Key Files Map
| Module | File | Lines | Purpose |
|--------|------|-------|---------|
| Controller | AuthController.java | 262 | HTTP endpoints |
| Service | AuthService.java | 701 | Business logic |
| JWT | JwtTokenProvider.java | 216 | Token ops |
| Filter | JwtAuthenticationFilter.java | 91 | Request validation |
| Session | SessionService.java | 65 | Session lifecycle |
| Repository | TokenBlacklistRepository.java | 15 | Blacklist queries |
| Entity | TokenBlacklist.java | 32 | Blacklist table |
| Entity | Token.java | 30 | Refresh token storage |
| Entity | Session.java | ~40 | Session storage (not fully read) |
| Test | AuthServiceRefreshTokenTest.java | 280 | Unit tests |
| Test | AuthControllerRefreshTokenTest.java | 64 | Integration tests |
| Config | application.properties | 50+ | Settings |

### How to Navigate
1. **To understand login**: Read `AuthController.login()` → `AuthService.loginUser()`
2. **To understand refresh token rotation**: Read `AuthServiceRefreshTokenTest.java` test cases first, then `AuthService.refreshToken()`
3. **To understand token validation**: Read `JwtTokenProvider.validateToken()` → `JwtAuthenticationFilter.doFilterInternal()`
4. **To debug token issues**: Check `TokenBlacklist` table and `JwtTokenProvider.isTokenBlacklisted()`

---

**Document Created:** 2026-10-20  
**Last Verified:** 2026-10-20  
**Status:** Ready for peer review and verification
