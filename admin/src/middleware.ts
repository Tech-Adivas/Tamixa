import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Minimal middleware so Next.js generates middleware-manifest.json.
 * Add auth checks, redirects, etc. here if needed.
 */
export function middleware(request: NextRequest) {
  void request; // Minimal middleware; extend for auth, redirects, etc.
  return NextResponse.next();
}
