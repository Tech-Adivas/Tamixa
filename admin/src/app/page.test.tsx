import { render, screen } from "@testing-library/react";
import HomePage from "./page";

describe("Admin HomePage", () => {
  it("renders Tamixa Admin heading", () => {
    render(<HomePage />);
    expect(screen.getByRole("heading", { name: /Tamixa Admin/i })).toBeInTheDocument();
  });

  it("renders Admin login link", () => {
    render(<HomePage />);
    expect(screen.getByRole("link", { name: /Admin login/i })).toHaveAttribute("href", "/login");
  });

  it("renders tagline", () => {
    render(<HomePage />);
    expect(screen.getByText(/Listen • Learn • Shine/i)).toBeInTheDocument();
  });
});
