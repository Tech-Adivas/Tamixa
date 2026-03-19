import { describe, it, expect, vi } from "vitest";
import { render, screen, act } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Dashboard from "./Dashboard";

vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => ({}),
}));

vi.mock("../lib/api", () => ({
  getRecommendedStories: vi.fn().mockResolvedValue([]),
  getRecentPlayback: vi.fn().mockResolvedValue([]),
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

describe("Dashboard", () => {
  it("renders Home header and subtitle", async () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Dashboard />
      </MemoryRouter>
    );
    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Home");
    expect(screen.getByText(/Continue listening, recommendations/i)).toBeInTheDocument();
  });

  it("shows Continue listening section", async () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Dashboard />
      </MemoryRouter>
    );
    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(screen.getByRole("heading", { name: /Continue listening/i })).toBeInTheDocument();
  });

  it("shows Recommended for you section", async () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Dashboard />
      </MemoryRouter>
    );
    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(screen.getByRole("heading", { name: /Recommended for you/i })).toBeInTheDocument();
  });
});
