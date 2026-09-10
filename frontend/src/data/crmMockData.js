// Mock data ported verbatim from frontend/template/CRM.dc.html. No backend endpoints exist yet
// for users/audit/roles -- wiring real data is out of scope for this frontend-only UI port.
// (Files has a real backend now -- see src/api/files.js -- so it isn't mocked here.)

export const USERS = [
  { id: "u1", name: "Duc Nguyen", email: "duc.nguyen@eastgate-software.com", lastActive: "2 minutes ago" },
  { id: "u2", name: "Linh Vo", email: "linh.vo@eastgate-software.com", lastActive: "1 hour ago" },
  { id: "u3", name: "Hai Tran", email: "hai.tran@eastgate-software.com", lastActive: "Yesterday, 5:40 PM" },
  { id: "u4", name: "Yen Bui", email: "yen.bui@eastgate-software.com", lastActive: "Sep 7, 2026" },
];

export const BLANK_ROLE_MATRIX = [
  { resource: "My Profiles", view: false, create: false, edit: false, delete: false },
  { resource: "File Management", view: false, create: false, edit: false, delete: false },
  { resource: "User Management", view: false, create: false, edit: false, delete: false },
  { resource: "Audit Log", view: false, create: false, edit: false, delete: false },
];

export const ROLES = [
  {
    id: "r1",
    name: "Administrator",
    description: "Full access to all modules and settings",
    userCount: 2,
    matrix: [
      { resource: "My Profiles", view: true, create: true, edit: true, delete: true },
      { resource: "File Management", view: true, create: true, edit: true, delete: true },
      { resource: "User Management", view: true, create: true, edit: true, delete: true },
      { resource: "Audit Log", view: true, create: false, edit: false, delete: false },
    ],
  },
  {
    id: "r2",
    name: "Manager",
    description: "Manages profiles and files, no user administration",
    userCount: 5,
    matrix: [
      { resource: "My Profiles", view: true, create: true, edit: true, delete: false },
      { resource: "File Management", view: true, create: true, edit: true, delete: false },
      { resource: "User Management", view: false, create: false, edit: false, delete: false },
      { resource: "Audit Log", view: false, create: false, edit: false, delete: false },
    ],
  },
  {
    id: "r3",
    name: "Editor",
    description: "Can edit profiles and files, cannot delete",
    userCount: 8,
    matrix: [
      { resource: "My Profiles", view: true, create: true, edit: true, delete: false },
      { resource: "File Management", view: true, create: true, edit: true, delete: false },
      { resource: "User Management", view: false, create: false, edit: false, delete: false },
      { resource: "Audit Log", view: false, create: false, edit: false, delete: false },
    ],
  },
  {
    id: "r4",
    name: "Viewer",
    description: "Read-only access to profiles and files",
    userCount: 14,
    matrix: [
      { resource: "My Profiles", view: true, create: false, edit: false, delete: false },
      { resource: "File Management", view: true, create: false, edit: false, delete: false },
      { resource: "User Management", view: false, create: false, edit: false, delete: false },
      { resource: "Audit Log", view: false, create: false, edit: false, delete: false },
    ],
  },
];

export const AUDIT = [
  { id: "a1", timestamp: "Sep 10, 2026, 8:14 AM", user: "Duc Nguyen", action: "Updated profile", entity: "Profile · Pham Thu Huong", ip: "203.113.44.12", severity: "Info", before: "status: Inactive", after: "status: Active" },
  { id: "a2", timestamp: "Sep 9, 2026, 6:02 PM", user: "Linh Vo", action: "Deleted file", entity: "File · Draft SOW v2.docx", ip: "203.113.44.51", severity: "Warning", before: "file: present", after: "file: deleted" },
  { id: "a3", timestamp: "Sep 9, 2026, 11:20 AM", user: "System", action: "Failed login attempt", entity: "User · hai.tran@eastgate-software.com", ip: "118.70.22.9", severity: "Error", before: "session: none", after: "session: none" },
  { id: "a4", timestamp: "Sep 8, 2026, 3:45 PM", user: "Hai Tran", action: "Uploaded file", entity: "File · Master Services Agreement.pdf", ip: "203.113.44.30", severity: "Info", before: "—", after: "file: created" },
  { id: "a5", timestamp: "Sep 7, 2026, 9:10 AM", user: "Duc Nguyen", action: "Changed permissions", entity: "User · Yen Bui", ip: "203.113.44.12", severity: "Warning", before: "role: Viewer", after: "role: Editor" },
  { id: "a6", timestamp: "Sep 5, 2026, 4:52 PM", user: "Yen Bui", action: "Created profile", entity: "Profile · Do Minh Quan", ip: "14.161.3.88", severity: "Info", before: "—", after: "profile: created" },
];
