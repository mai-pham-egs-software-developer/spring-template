import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { RequireAuth } from "./auth/RequireAuth";
import { CrmLayout } from "./layout/CrmLayout";
import { Home } from "./pages/Home";
import { CasbinModelConfigPage } from "./pages/crm/CasbinModelConfigPage";
import { CasbinPoliciesPage } from "./pages/crm/CasbinPoliciesPage";
import { FileDetailPage } from "./pages/crm/FileDetailPage";
import { FilesPage } from "./pages/crm/FilesPage";
import { OrganizationDetailPage } from "./pages/crm/OrganizationDetailPage";
import { OrganizationsPage } from "./pages/crm/OrganizationsPage";
import { ProfilePage } from "./pages/crm/ProfilePage";
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
            <Route path="organizations" element={<OrganizationsPage />} />
            <Route path="organizations/:organizationId" element={<OrganizationDetailPage />} />
            <Route path="casbin-policies" element={<CasbinPoliciesPage />} />
            <Route path="casbin-model-config" element={<CasbinModelConfigPage />} />
            <Route path="api-demo" element={<Home />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
