import type { Metadata } from "next";
import { IBM_Plex_Sans } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/contexts/auth-context";
import { ActionResultProvider } from "@/contexts/action-result-context";
import { Providers } from "@/components/providers";

const ibmPlexSans = IBM_Plex_Sans({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-sans",
});

export const metadata: Metadata = {
  title: "Araro Admin",
  description: "Admin dashboard for Araro",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${ibmPlexSans.variable} font-sans antialiased`}>
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
