import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { RequireAuth } from "./auth/RequireAuth";
import { CrmLayout } from "./layout/CrmLayout";
import { Home } from "./pages/Home";
import { AuditDetailPage } from "./pages/crm/AuditDetailPage";
import { AuditLogPage } from "./pages/crm/AuditLogPage";
import { FileDetailPage } from "./pages/crm/FileDetailPage";
import { FilesPage } from "./pages/crm/FilesPage";
import { ProfilePage } from "./pages/crm/ProfilePage";
import { RoleDetailPage } from "./pages/crm/RoleDetailPage";
import { RolesPage } from "./pages/crm/RolesPage";
import { UserDetailPage } from "./pages/crm/UserDetailPage";
import { UsersPage } from "./pages/crm/UsersPage";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route
            element={
              <RequireAuth>
                <CrmLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Navigate to="/profile" replace />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="files" element={<FilesPage />} />
            <Route path="files/:fileId" element={<FileDetailPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="users/:userId" element={<UserDetailPage />} />
            <Route path="audit-log" element={<AuditLogPage />} />
            <Route path="audit-log/:auditId" element={<AuditDetailPage />} />
            <Route path="roles" element={<RolesPage />} />
            <Route path="roles/:roleId" element={<RoleDetailPage />} />
            <Route path="api-demo" element={<Home />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
