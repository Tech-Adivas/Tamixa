import { describe, it, expect, vi } from "vitest";
import { render, screen, act } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Dashboard from "./Dashboard";

vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => ({ user: { email: "parent@example.com", role: "PARENT" } }),
}));

vi.mock("../lib/api", () => ({
  getRecommendedStories: vi.fn().mockResolvedValue([]),
  getRecentPlayback: vi.fn().mockResolvedValue([]),
  getLibraryStories: vi.fn().mockResolvedValue([]),
  resolveCoverUrl: (u: string | null | undefined) => u ?? null,
  getListeningProgress: vi.fn().mockResolvedValue({
    periodDays: 30,
    storiesStarted: 2,
    storiesCompleted: 1,
    completionRate: 0.5,
  }),
}));

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...(actual as object),
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
      <a href={to}>{children}</a>
    ),
  };
});

const routerFuture = {
  v7_startTransition: true,
  v7_relativeSplatPath: true,
} as const;

async function flushDashboard() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
  });
}

describe("Dashboard", () => {
  it("renders welcome heading and overview", async () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Dashboard />
      </MemoryRouter>
    );
    await flushDashboard();
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(/Welcome back/i);
    expect(screen.getByText(/Jump back in, discover tonight.s picks/i)).toBeInTheDocument();
  });

  it("shows Continue listening section", async () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Dashboard />
      </MemoryRouter>
    );
    await flushDashboard();
    expect(screen.getByRole("heading", { name: /Resume a story/i })).toBeInTheDocument();
  });

  it("shows Recommended for you section", async () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Dashboard />
      </MemoryRouter>
    );
    await flushDashboard();
    expect(screen.getByRole("heading", { name: /Tonight's picks/i })).toBeInTheDocument();
  });
});
