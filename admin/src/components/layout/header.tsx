"use client";

import Link from "next/link";
import { useAuth } from "@/contexts/auth-context";
import { useSidebar } from "@/contexts/sidebar-context";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { LogOut, Moon, Sun, Menu } from "lucide-react";
import { useTheme } from "next-themes";

export function Header() {
  const { user, logout } = useAuth();
  const { toggle } = useSidebar();
  const { theme, setTheme } = useTheme();

  const handleLogout = () => {
    logout();
    // Replace (not push) avoids back-button returning to cached dashboard; full reload clears all state
    window.location.replace("/login");
  };

  return (
    <header className="sticky top-0 z-30 flex min-h-touch h-14 items-center justify-between border-b border-border bg-card px-4 sm:px-6 shadow-sm">
      <div className="flex items-center gap-2">
        <Button
          variant="ghost"
          size="icon"
          className="h-10 w-10 shrink-0 text-muted-foreground md:hidden"
          onClick={toggle}
          aria-label="Open menu"
        >
          <Menu className="h-5 w-5" />
        </Button>
        <Link
          href="/dashboard"
          className="flex shrink-0 items-center rounded-lg border border-border bg-muted/50 px-4 py-2 transition-colors hover:bg-muted focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
        >
          <span className="text-base font-semibold tracking-tight text-foreground">
            Tamixa
          </span>
        </Link>
      </div>
      <div className="flex-1 min-w-2" />
      <div className="flex items-center gap-2">
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground"
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          aria-label={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
        >
          <span className="relative flex h-4 w-4 items-center justify-center">
            <Sun className="h-4 w-4 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
            <Moon className="absolute h-4 w-4 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
          </span>
        </Button>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              className="flex items-center gap-2 rounded-xl px-3 text-muted-foreground hover:bg-muted hover:text-foreground"
            >
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/15 text-lg font-bold text-primary">
                {user?.email?.[0]?.toUpperCase() ?? "—"}
              </div>
              <span className="hidden max-w-[140px] truncate sm:inline">{user?.email ?? "—"}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              Log out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
