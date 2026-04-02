import { useState, lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { AuthProvider, useAuth } from "./contexts/AuthContext";

function RootRoute() {
  const { user, loading } = useAuth();
  if (loading) return <AppLoading />;
  if (user) return <Navigate to="/stories" replace />;
  return <Home />;
}
import { ErrorBoundary } from "./components/ErrorBoundary";
import { AppLayout } from "./components/AppLayout";
import { AppLoading } from "./components/AppLoading";
import { TamixaSplash } from "./components/TamixaSplash";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import MagicLinkVerify from "./pages/MagicLinkVerify";
import Privacy from "./pages/Privacy";
import Terms from "./pages/Terms";

const Dashboard = lazy(() => import("./pages/Dashboard"));
const Stories = lazy(() => import("./pages/Stories"));
const Subscription = lazy(() => import("./pages/Subscription"));
const Voice = lazy(() => import("./pages/Voice"));
const Settings = lazy(() => import("./pages/Settings"));
import "./index.css";

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <AppLoading />;
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  return <AppLayout><Suspense fallback={<AppLoading />}>{children}</Suspense></AppLayout>;
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <AppLoading />;
  if (user) return <Navigate to="/stories" replace />;
  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RootRoute />} />
      <Route path="/privacy" element={<Privacy />} />
      <Route path="/terms" element={<Terms />} />
      <Route path="/auth/magic-link" element={<MagicLinkVerify />} />
      <Route
        path="/login"
        element={
          <PublicRoute>
            <Login />
          </PublicRoute>
        }
      />
      <Route
        path="/register"
        element={
          <PublicRoute>
            <Register />
          </PublicRoute>
        }
      />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/stories"
        element={
          <ProtectedRoute>
            <Stories />
          </ProtectedRoute>
        }
      />
      <Route
        path="/subscription"
        element={
          <ProtectedRoute>
            <Subscription />
          </ProtectedRoute>
        }
      />
      <Route
        path="/voice"
        element={
          <ProtectedRoute>
            <Voice />
          </ProtectedRoute>
        }
      />
      <Route
        path="/settings"
        element={
          <ProtectedRoute>
            <Settings />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default function App() {
  const [splashDone, setSplashDone] = useState(false);

  if (!splashDone) {
    return (
      <ErrorBoundary>
        <TamixaSplash onDone={() => setSplashDone(true)} />
      </ErrorBoundary>
    );
  }

  return (
    <ErrorBoundary>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  );
}
