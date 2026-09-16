import { useEffect } from "react";
import { useAuth } from "./AuthContext";

// Wrap protected routes with this. Unauthenticated users are sent straight
// into the Keycloak login redirect rather than shown a "please log in" page.
//
// With keycloak.init({ onLoad: "login-required" }) this is mostly a no-op
// safety net -- init() itself already blocks until the user is authenticated.
export function RequireAuth({ children }) {
  const { initialized, authenticated, initError, login } = useAuth();

  useEffect(() => {
    // Only retry login once, and never after a failed init -- retrying a
    // failed token exchange just reproduces the same failure indefinitely.
    if (initialized && !authenticated && !initError) {
      login();
    }
  }, [initialized, authenticated, initError, login]);

  if (!initialized) {
    return <p>Loading...</p>;
  }

  if (initError) {
    return (
      <p>
        Login failed: {initError.message ?? String(initError)}. Check the browser console and
        network tab for details.
      </p>
    );
  }

  if (!authenticated) {
    return <p>Redirecting to login...</p>;
  }

  return children;
}
