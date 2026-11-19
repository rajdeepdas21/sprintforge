import { Navigate, Outlet, Route, Routes } from "react-router";
import { useAuth } from "./auth/AuthContext";
import AuthPage from "./pages/AuthPage";
import DashboardPage from "./pages/DashboardPage";
import WorkspaceProjectsPage from "./pages/WorkspaceProjectsPage";
import WorkspaceMembersPage from "./pages/WorkspaceMembersPage";
import ProjectDetailsPage from "./pages/ProjectDetailsPage";

function RequireAuthentication() {
  const { isAuthenticated } = useAuth();

  return isAuthenticated ? <Outlet /> : <Navigate replace to="/login" />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate replace to="/app" />} />
      <Route path="/login" element={<AuthPage mode="login" />} />
      <Route path="/register" element={<AuthPage mode="register" />} />

      <Route element={<RequireAuthentication />}>
        <Route path="/app" element={<DashboardPage />} />
        <Route
          path="/workspaces/:workspaceId"
          element={<WorkspaceProjectsPage />}
        />
        <Route
          path="/workspaces/:workspaceId/members"
          element={<WorkspaceMembersPage />}
        />
        <Route path="/projects/:projectId" element={<ProjectDetailsPage />} />
      </Route>

      <Route path="*" element={<Navigate replace to="/app" />} />
    </Routes>
  );
}
