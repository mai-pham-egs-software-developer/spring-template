import { pill } from "../../layout/crmStyles";

// Mirrors backend's UserStatus (com.my.craft.security.domain) -- tracks the outbox-driven async
// sync to Keycloak, see backend/docs/user-outbox.md.
const STYLES = {
  PENDING: pill("var(--color-warning-bg)", "var(--color-warning)"),
  ACTIVE: pill("var(--color-mist)", "var(--color-fg-2)"),
  CONFLICT: pill("var(--color-error-bg)", "var(--color-error)"),
  FAILED: pill("var(--color-error-bg)", "var(--color-error)"),
};

export function UserStatusPill({ status }) {
  return <span style={STYLES[status] ?? STYLES.PENDING}>{status}</span>;
}
