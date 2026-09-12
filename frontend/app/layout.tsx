import type { Metadata } from "next";
import { Inter, Space_Grotesk, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import "leaflet/dist/leaflet.css";
import { Toaster } from "sonner";
import { AuthProvider } from "@/components/shared/AuthProvider";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

const spaceGrotesk = Space_Grotesk({
  subsets: ["latin"],
  variable: "--font-space",
  display: "swap",
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "PROJECT NOVA — AI Emergency Response Network",
    template: "%s | PROJECT NOVA",
  },
  description:
    "An intelligent emergency response network that transforms real-time citizen reports into coordinated rescue decisions. From Emergency Reports to Intelligent Action — in Seconds.",
  keywords: [
    "emergency response",
    "AI disaster management",
    "Sri Lanka",
    "flood monitoring",
    "rescue coordination",
    "PROJECT NOVA",
  ],
  openGraph: {
    title: "PROJECT NOVA — AI Emergency Response Network",
    description: "From Emergency Reports to Intelligent Action — in Seconds.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`dark ${inter.variable} ${spaceGrotesk.variable} ${jetbrainsMono.variable}`}
    >
      <body className="antialiased bg-nova-bg text-nova-text font-sans">
        <AuthProvider>
          {children}
        </AuthProvider>
        <Toaster
          theme="dark"
          toastOptions={{
            style: {
              background: '#0d1629',
              border: '1px solid #1a2744',
              color: '#e8f0fe',
            },
          }}
          position="top-right"
        />
      </body>
    </html>
  );
}
