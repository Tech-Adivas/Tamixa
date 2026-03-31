import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const PUBLIC_PATHS = ["/login", "/", "/_next", "/favicon", "/tamixa-logo", "/araro-logo"];

function isPublicPath(pathname: string): boolean {
  return PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(p + "/") || pathname.startsWith("/_next/"));
}

/**
 * Edge middleware: redirect unauthenticated requests to /login.
 * Token presence is a lightweight check — the backend still validates the JWT on every API call.
 * This prevents unauthenticated browsers from reaching dashboard pages at all.
 */
export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Allow public paths and static assets through
  if (isPublicPath(pathname)) return NextResponse.next();

  // Allow API proxy through (backend validates auth)
  if (pathname.startsWith("/api/")) return NextResponse.next();

  // For dashboard routes: require token cookie or redirect to login
  const token = request.cookies.get("admin_access_token")?.value;
  if (!token) {
    const loginUrl = request.nextUrl.clone();
    loginUrl.pathname = "/login";
    loginUrl.search = "?expired=1";
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all paths except static files.
     * Middleware runs on: /dashboard/*, /login, etc.
     */
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)",
  ],
};
