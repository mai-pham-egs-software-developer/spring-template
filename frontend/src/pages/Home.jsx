import { useState } from "react";
import { getMe } from "../api/me";
import { useAuth } from "../auth/AuthContext";

export function Home() {
  const { user, hasRole } = useAuth();
  const [meResult, setMeResult] = useState(null);
  const [meError, setMeError] = useState(null);
  const [loading, setLoading] = useState(false);

  function callMe() {
    setLoading(true);
    setMeError(null);
    getMe()
      .then((data) => setMeResult(data))
      .catch((err) => setMeError(err.response?.data?.message ?? err.message))
      .finally(() => setLoading(false));
  }

  return (
    <section>
      <h1>Welcome, {user?.preferred_username}</h1>
      <p>Email: {user?.email}</p>
      <p>Realm roles: {user?.realm_access?.roles?.join(", ") || "none"}</p>
      <p>Is admin: {hasRole("admin") ? "yes" : "no"}</p>

      <button type="button" onClick={callMe} disabled={loading}>
        {loading ? "Calling GET /me..." : "Call GET /me"}
      </button>
      {meError && <p style={{ color: "crimson" }}>Error: {meError}</p>}
      {meResult && <pre>{JSON.stringify(meResult, null, 2)}</pre>}
    </section>
  );
}
