import type { Metadata } from "next";
import { Nunito, Inter } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/contexts/auth-context";
import { ActionResultProvider } from "@/contexts/action-result-context";
import { Providers } from "@/components/providers";

const nunito = Nunito({
  subsets: ["latin"],
  weight: ["400", "600", "700", "800"],
  variable: "--font-sans",
});

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "Tamixa Admin",
  description: "Admin dashboard for Tamixa",
  // favicon: app/favicon.ico (generated from app icon). Avoid listing favicon.svg — browsers prefer SVG and it was the old mark.
  icons: {
    icon: [{ url: "/favicon.png", sizes: "32x32", type: "image/png" }],
    shortcut: "/favicon.ico",
    apple: "/tamixa-app-icon.png",
  },
};

export const viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 5,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${nunito.variable} ${inter.variable} font-sans antialiased`}>
        <Providers>
          <AuthProvider>
            <ActionResultProvider>
              {children}
            </ActionResultProvider>
          </AuthProvider>
        </Providers>
      </body>
    </html>
  );
}
