import apiClient from "./client";

// Backend's MeController (GET /) echoes back the claims of whichever
// principal authenticated the request -- here, the Keycloak access token
// this app attaches as a Bearer header (see api/client.js).
export function getMe() {
  return apiClient.get("/me").then((res) => res.data);
}
