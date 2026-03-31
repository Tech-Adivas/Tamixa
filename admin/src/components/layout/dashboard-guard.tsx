"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/contexts/auth-context";
import { reconcileAdminAuthCookie } from "@/lib/api";
import { PipelineActiveProvider } from "@/contexts/pipeline-active-context";
import { SidebarProvider } from "@/contexts/sidebar-context";
import { PipelineStatusBanner } from "./pipeline-status-banner";
import { Sidebar } from "./sidebar";
import { Header } from "./header";
import { isAdminRole } from "@/lib/admin-roles";

export function DashboardGuard({ children }: { children: React.ReactNode }) {
  const { user, loading, error } = useAuth();
  const router = useRouter();

  useEffect(() => {
    reconcileAdminAuthCookie();
  }, []);

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace("/login");
      return;
    }
    if (!isAdminRole(user.role)) {
      router.replace("/login?error=forbidden");
      return;
    }
  }, [user, loading, router]);

  if (loading) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-tamixa-page">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-primary border-t-transparent" />
        <span className="text-sm font-medium text-muted-foreground">{error || "Loading…"}</span>
      </div>
    );
  }
  if (!user || !isAdminRole(user.role)) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-tamixa-page">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-primary border-t-transparent" />
        <span className="text-sm font-medium text-muted-foreground">Redirecting to login…</span>
      </div>
    );
  }

  return (
    <PipelineActiveProvider userRole={user.role}>
      <SidebarProvider>
        <div className="min-h-screen bg-tamixa-page">
          <a
            href="#main-content"
            className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-[100] focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          >
            Skip to main content
          </a>
          <Sidebar />
          <div className="pl-0 md:pl-56 transition-[padding] duration-200">
            <div className="sticky top-0 z-30 bg-tamixa-page">
              <Header />
              <PipelineStatusBanner />
            </div>
            <main
              id="main-content"
              className="min-h-screen p-4 sm:p-6 w-full max-w-[1600px] mx-auto"
              aria-label="Main content"
            >
              {children}
            </main>
          </div>
        </div>
      </SidebarProvider>
    </PipelineActiveProvider>
  );
}
