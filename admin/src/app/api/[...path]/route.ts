/**
 * Proxy for /api/* → backend. Forwards Authorization header so authenticated
 * requests (Submit for review, etc.) work. Next.js rewrites do NOT forward
 * client headers, causing "Unauthorized. Please log in."
 */
import type { NextRequest } from "next/server";

const BACKEND_URL =
  process.env.API_URL ||
  process.env.NEXT_PUBLIC_API_URL ||
  "http://localhost:8080";

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> }
) {
  return proxy(request, context);
}

export async function POST(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> }
) {
  return proxy(request, context);
}

export async function PUT(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> }
) {
  return proxy(request, context);
}

export async function PATCH(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> }
) {
  return proxy(request, context);
}

export async function DELETE(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> }
) {
  return proxy(request, context);
}

async function proxy(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> }
) {
  const { path } = await context.params;
  const pathStr = path?.length ? path.join("/") : "";
  const search = request.nextUrl.search;
  const url = `${BACKEND_URL.replace(/\/$/, "")}/api/${pathStr}${search}`;

  const headers = new Headers();
  const forwardKeys = ["authorization", "content-type", "x-request-id"];
  request.headers.forEach((value, key) => {
    if (forwardKeys.includes(key.toLowerCase()) && value) {
      headers.set(key, value);
    }
  });
  // Content-Type if not already set (for JSON body)
  if (!headers.has("content-type") && request.method !== "GET") {
    headers.set("Content-Type", "application/json");
  }

  const init: RequestInit = {
    method: request.method,
    headers,
    cache: "no-store",
  };
  if (request.method !== "GET" && request.body) {
    init.body = await request.arrayBuffer();
  }

  const res = await fetch(url, init);
  const resHeaders = new Headers(res.headers);
  resHeaders.delete("content-encoding");
  return new Response(res.body, {
    status: res.status,
    statusText: res.statusText,
    headers: resHeaders,
  });
}
