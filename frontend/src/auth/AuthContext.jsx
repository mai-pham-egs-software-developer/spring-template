import { createContext, useContext, useEffect, useRef, useState } from "react";
import keycloak from "./keycloak";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [initError, setInitError] = useState(null);
  const didInit = useRef(false);

  useEffect(() => {
    // React 18/19 StrictMode mounts effects twice in dev; keycloak-js's
    // init() throws if called a second time on the same instance.
    if (didInit.current) return;
    didInit.current = true;

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => keycloak.login());
    };

    keycloak
      .init({
        // "login-required" does a full top-level redirect to Keycloak rather
        // than a "check-sso" silent iframe check. The iframe approach reads
        // the Keycloak session cookie from a third-party context (Keycloak's
        // origin, framed inside this app's origin), which browsers with
        // third-party-cookie blocking enabled (Chrome, Safari default) always
        // report as "not logged in" -- even right after a successful login --
        // which sends RequireAuth into a login() redirect loop. Every route
        // here requires auth anyway, so there's no upside to the silent check.
        onLoad: "login-required",
        pkceMethod: "S256",
        checkLoginIframe: false,
      })
      .then((isAuthenticated) => {
        setAuthenticated(isAuthenticated);
        setInitialized(true);
      })
      .catch((error) => {
        // Surface this instead of silently leaving authenticated=false --
        // that used to send RequireAuth into an infinite login() redirect
        // loop, retrying the exact same failing exchange forever.
        console.error("Keycloak init failed:", error);
        setInitError(error);
        setInitialized(true);
      });
  }, []);

  const value = {
    initialized,
    authenticated,
    initError,
    keycloak,
    user: keycloak.tokenParsed,
    login: (options) => keycloak.login(options),
    logout: (options) => keycloak.logout(options ?? { redirectUri: window.location.origin }),
    hasRole: (role) => keycloak.hasRealmRole(role) || keycloak.hasResourceRole(role),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
