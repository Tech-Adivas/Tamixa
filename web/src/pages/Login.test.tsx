import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Login from "./Login";

vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => ({ setUser: vi.fn() }),
}));

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual as object,
    useNavigate: () => vi.fn(),
  };
});

vi.mock("../lib/api", () => ({
  login: vi.fn(),
  authStorage: { setTokens: vi.fn() },
  getMe: vi.fn(),
  requestPasswordlessCode: vi.fn(),
  verifyPasswordlessCode: vi.fn(),
}));

const routerFuture = {
  v7_startTransition: true,
  v7_relativeSplatPath: true,
} as const;

describe("Login", () => {
  it("renders passwordless login with email field", () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Login />
      </MemoryRouter>
    );
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign-in code/i })).toBeInTheDocument();
  });

  it("has accessible form controls with proper labels", () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Login />
      </MemoryRouter>
    );
    expect(screen.getByPlaceholderText(/you@example\.com/i)).toHaveAttribute("type", "email");
    expect(screen.getByRole("button", { name: /sign-in code/i })).toBeInTheDocument();
  });
});
