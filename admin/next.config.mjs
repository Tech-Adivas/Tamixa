/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  async redirects() {
    return [
      { source: "/dashboard/story-for-review", destination: "/dashboard/stories/approve", permanent: true },
      { source: "/dashboard/story-to-speech", destination: "/dashboard/stories/to-speech", permanent: true },
    ];
  },
  // No rewrites: use app/api/[...path]/route.ts proxy instead. Rewrites do NOT
  // forward Authorization header, causing 401 on Submit for review. Our proxy does.
  // async rewrites() {
  //   const apiUrl = process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
  //   return { beforeFiles: [{ source: "/api/:path*", destination: `${apiUrl}/api/:path*` }] };
  // },
};

export default nextConfig;
