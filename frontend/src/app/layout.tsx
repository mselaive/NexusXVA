import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "NexusXVA Dashboard",
  description: "Portfolio pricing, exposure, and CVA dashboard for NexusXVA",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
