import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import Register from "./Register";

vi.mock("../contexts/AuthContext", () => ({
  useAuth: () => ({ setUser: vi.fn() }),
}));

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...(actual as object),
    useNavigate: () => vi.fn(),
  };
});

vi.mock("../lib/api", () => ({
  register: vi.fn(),
  authStorage: { setTokens: vi.fn() },
  getMe: vi.fn(),
}));

const routerFuture = {
  v7_startTransition: true,
  v7_relativeSplatPath: true,
} as const;

describe("Register", () => {
  it("renders create account form", () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Register />
      </MemoryRouter>
    );
    expect(screen.getByRole("heading", { name: /Create account/i })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: /email/i })).toBeInTheDocument();
    expect(document.getElementById("password")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Create account/i })).toBeInTheDocument();
  });

  it("shows password validation error when password too short", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter future={routerFuture}>
        <Register />
      </MemoryRouter>
    );
    await user.type(screen.getByRole("textbox", { name: /email/i }), "test@example.com");
    await user.type(document.getElementById("password")!, "short");
    const submitBtn = screen.getByRole("button", { name: /Create account/i });
    await user.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/Password must be at least 8 characters/i)).toBeInTheDocument();
    });
  });

  it("has required consent checkboxes", () => {
    render(
      <MemoryRouter future={routerFuture}>
        <Register />
      </MemoryRouter>
    );
    expect(screen.getByRole("checkbox", { name: /Terms of Service/i })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /Privacy Policy/i })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /parent or guardian/i })).toBeInTheDocument();
  });
});
