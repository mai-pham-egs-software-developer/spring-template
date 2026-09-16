import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { RequireAuth } from "./auth/RequireAuth";
import { CrmLayout } from "./layout/CrmLayout";
import { OrgProvider } from "./org/OrgContext";
import { RequireOrg } from "./org/RequireOrg";
import { Home } from "./pages/Home";
import { SelectOrgPage } from "./pages/SelectOrgPage";
import { ProfilePage } from "./pages/crm/ProfilePage";

function App() {
  return (
    <AuthProvider>
      <OrgProvider>
        <BrowserRouter>
          <RequireAuth>
            <Routes>
              {/* Outside RequireOrg -- reachable once logged in but before an org is picked. */}
              <Route path="select-org" element={<SelectOrgPage />} />

              <Route
                element={
                  <RequireOrg>
                    <CrmLayout />
                  </RequireOrg>
                }
              >
                <Route index element={<Navigate to="/profile" replace />} />
                <Route path="profile" element={<ProfilePage />} />
                <Route path="api-demo" element={<Home />} />
              </Route>
            </Routes>
          </RequireAuth>
        </BrowserRouter>
      </OrgProvider>
    </AuthProvider>
  );
}

export default App;
