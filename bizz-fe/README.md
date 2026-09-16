# bizz-fe

React (Vite, JS) SPA authenticated against Keycloak via `keycloak-js`, using the
standard OIDC authorization-code + PKCE flow (no client secret in the browser).

Cloned from `../frontend` as its own site: same auth/layout scaffolding, but its
own Keycloak client and only two pages (everything else `../frontend` has --
Files, Users, Organizations, Casbin admin -- was intentionally left out).

## Structure

```text
src/
├── api/
│   ├── client.js        # axios instance; attaches a fresh Bearer token to every request
│   └── me.js             # GET /me
├── auth/
│   ├── keycloak.js       # Keycloak client instance (reads VITE_KEYCLOAK_*)
│   ├── AuthContext.jsx   # AuthProvider + useAuth() (init, token refresh, login/logout)
│   └── RequireAuth.jsx   # route guard, redirects to Keycloak login if unauthenticated
├── layout/
│   ├── CrmLayout.jsx      # sidebar shell (nav: My Profile, API Demo)
│   ├── CrmPageHeader.jsx  # shared breadcrumb/title/action header
│   └── crmStyles.js       # shared style-object constants
├── pages/
│   ├── Home.jsx           # "API Demo" -- calls GET /me on click
│   └── crm/ProfilePage.jsx # "My Profile" -- reads the Keycloak token claims
├── App.jsx                # router + AuthProvider wiring
└── main.jsx
```

## Keycloak setup

This app expects its **own public** client in Keycloak -- separate from the
backend's confidential `be-application` client, and separate from
`../frontend`'s own `fe-application` client.

In the realm configured by `VITE_KEYCLOAK_REALM` (default `bootstrap`), create a client:

- Client ID: `bizz-fe-application` (or update `.env` to match)
- Client authentication: **Off** (public client)
- Standard flow: **On**, PKCE method `S256`
- Valid redirect URIs: `http://localhost:5174/*`
- Valid post logout redirect URIs: `http://localhost:5174/*`
- Web origins: `http://localhost:5174`

The backend also needs this origin added to `app.cors.allowed-origins`
(`backend/source/applications/main/src/main/resources/application.yml`, currently
just `http://localhost:5173` for `../frontend`) or `GET /me` will fail in-browser
with a CORS error even though the request itself is valid.

## Configuration

Copy/edit `.env` (already present with local defaults):

```
VITE_KEYCLOAK_URL=http://localhost:8080
VITE_KEYCLOAK_REALM=bootstrap
VITE_KEYCLOAK_CLIENT_ID=bizz-fe-application
VITE_API_BASE_URL=http://localhost:8081
```

## Run

```bash
npm install
npm run dev
```

Dev server runs on **port 5174** (see `vite.config.js`) so it can run alongside
`../frontend` (port 5173) at the same time. Keycloak must be reachable at
`VITE_KEYCLOAK_URL` before the app loads -- `keycloak.init()` fetches the
realm's OIDC discovery document on startup.

## Notes

- `keycloak.init()` uses `onLoad: "login-required"` (a full top-level redirect to
  Keycloak), not `check-sso`. `check-sso` relies on a hidden iframe reading the
  Keycloak session cookie from a third-party context, which browsers with
  third-party-cookie blocking (Chrome/Safari defaults) always report as
  logged-out -- even right after a real login -- causing an infinite login
  redirect loop. Since every route in this app requires auth, there's no
  upside to the silent check anyway. `checkLoginIframe` is disabled for the
  same reason. `RequireAuth` still calls `keycloak.login()` as a safety net,
  but with `login-required` it should never actually need to.
- `apiClient` (`src/api/client.js`) calls `keycloak.updateToken(30)` before every
  request, refreshing the access token if it's within 30s of expiry, and forces
  a re-login if the refresh token itself is no longer valid.
- `useAuth().hasRole("some-role")` checks both realm and client (resource) roles.
