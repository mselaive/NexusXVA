import type { NextConfig } from "next";

const backendBaseUrl = process.env.NEXUSXVA_API_BASE_URL ?? "http://localhost:8080";
const blembergBaseUrl = process.env.BLEMBERG_BASE_URL ?? "http://localhost:8081";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/nexus-api/:path*",
        destination: `${backendBaseUrl}/api/:path*`,
      },
      {
        source: "/blemberg-api/:path*",
        destination: `${blembergBaseUrl}/:path*`,
      },
    ];
  },
};

export default nextConfig;
