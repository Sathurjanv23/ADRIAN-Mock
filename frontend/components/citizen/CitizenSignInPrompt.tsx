'use client';

import { motion } from 'framer-motion';
import Link from 'next/link';
import { Shield, Lock, Radio, ArrowRight, UserCheck, AlertTriangle, FileText, ChevronRight } from 'lucide-react';
import { NovaLogo } from '@/components/shared/TopNav';

interface CitizenSignInPromptProps {
  feature?: 'dashboard' | 'reports' | 'general';
}

export function CitizenSignInPrompt({ feature = 'general' }: CitizenSignInPromptProps) {
  const titles = {
    dashboard: 'Citizen Dashboard — Sign In Required',
    reports: 'My Reports & Tracking — Sign In Required',
    general: 'Citizen Portal — Sign In Required',
  };

  const descriptions = {
    dashboard:
      'Anyone can report an emergency immediately without an account. However, accessing the Citizen Dashboard, tracking emergency dispatch updates, and viewing personalized safety alerts requires you to sign in.',
    reports:
      'Your emergency report history and live responder dispatches are protected. Please sign in to your citizen account to track and manage your reports.',
    general:
      'Emergency reporting is open to all citizens without signing in. However, full access to the Citizen Portal (personal dashboard, report history, live responder tracking, and safety resources) requires you to sign in.',
  };

  return (
    <div className="min-h-screen bg-nova-bg text-nova-text flex flex-col justify-between">
      {/* Top Simple Header */}
      <header className="border-b border-nova-border/60 bg-nova-bg/90 backdrop-blur-md px-6 py-4 flex items-center justify-between">
        <NovaLogo size="md" />
        <div className="flex items-center gap-3">
          <Link
            href="/citizen/sos"
            className="flex items-center gap-1.5 text-xs font-bold bg-red-500/15 border border-red-500/40 text-red-300 px-3 py-1.5 rounded-lg hover:bg-red-500/25 transition-all"
          >
            <Radio className="w-3.5 h-3.5" />
            Report Emergency (No Sign-In)
          </Link>
          <Link
            href="/login?portal=citizen"
            className="text-xs font-semibold bg-nova-cyan text-nova-bg px-4 py-1.5 rounded-lg hover:bg-nova-cyan-dim transition-all"
          >
            Sign In
          </Link>
        </div>
      </header>

      {/* Main Card */}
      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <motion.div
          className="max-w-lg w-full nova-card border border-nova-border/80 rounded-2xl p-8 shadow-nova text-center relative overflow-hidden"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          {/* Subtle background glow */}
          <div className="absolute -top-24 -left-24 w-48 h-48 rounded-full bg-nova-cyan/10 blur-3xl pointer-events-none" />
          <div className="absolute -bottom-24 -right-24 w-48 h-48 rounded-full bg-red-500/10 blur-3xl pointer-events-none" />

          {/* Icon Badge */}
          <div className="relative mx-auto mb-6 w-20 h-20 rounded-2xl bg-nova-surface2 border border-nova-border flex items-center justify-center">
            <div className="absolute inset-0 rounded-2xl bg-nova-cyan/5 animate-pulse" />
            <div className="w-12 h-12 rounded-xl bg-nova-cyan/10 border border-nova-cyan/30 flex items-center justify-center">
              <Lock className="w-6 h-6 text-nova-cyan" />
            </div>
            <div className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-red-500/20 border border-red-500/40 flex items-center justify-center">
              <Shield className="w-3.5 h-3.5 text-red-400" />
            </div>
          </div>

          {/* Badge */}
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-nova-cyan/10 border border-nova-cyan/30 text-[11px] font-bold text-nova-cyan uppercase tracking-wider mb-4">
            <span className="w-1.5 h-1.5 rounded-full bg-nova-cyan animate-ping-slow" />
            Citizen Portal Access Control
          </div>

          {/* Title */}
          <h1 className="text-2xl font-bold font-display text-nova-text mb-3">
            {titles[feature]}
          </h1>

          {/* Description */}
          <p className="text-sm text-nova-text-dim leading-relaxed mb-8">
            {descriptions[feature]}
          </p>

          {/* Action Buttons */}
          <div className="space-y-3">
            <Link
              href="/login?portal=citizen"
              className="w-full flex items-center justify-center gap-2 bg-nova-cyan text-nova-bg font-bold py-3.5 px-6 rounded-xl hover:bg-nova-cyan-dim transition-all shadow-nova-cyan text-sm"
            >
              <UserCheck className="w-4 h-4" />
              Sign In to Citizen Portal
              <ArrowRight className="w-4 h-4" />
            </Link>

            <Link
              href="/register?role=citizen"
              className="w-full flex items-center justify-center gap-2 bg-nova-surface2 border border-nova-border text-nova-text font-semibold py-3 px-6 rounded-xl hover:border-nova-border2 hover:bg-nova-surface transition-all text-sm"
            >
              Create a Citizen Account
            </Link>
          </div>

          {/* Divider */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-nova-border/60" />
            </div>
            <div className="relative flex justify-center text-[10px] uppercase tracking-wider">
              <span className="bg-nova-surface px-3 text-nova-text-muted">In an active emergency?</span>
            </div>
          </div>

          {/* Emergency SOS Bypass */}
          <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-left flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-red-500/20 border border-red-500/40 flex items-center justify-center flex-shrink-0">
                <Radio className="w-5 h-5 text-red-400 animate-pulse" />
              </div>
              <div>
                <p className="text-xs font-bold text-red-300">Need immediate help?</p>
                <p className="text-[11px] text-nova-text-muted">You do not need to sign in to report emergency.</p>
              </div>
            </div>
            <Link
              href="/citizen/sos"
              className="flex items-center gap-1 text-xs font-bold text-red-400 hover:text-red-300 transition-colors whitespace-nowrap"
            >
              Report Now <ChevronRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {/* Footer Back Link */}
          <div className="mt-6 text-center">
            <Link href="/" className="text-xs text-nova-text-muted hover:text-nova-text transition-colors">
              ← Return to Homepage
            </Link>
          </div>
        </motion.div>
      </main>

      <footer className="border-t border-nova-border/40 py-4 text-center text-xs text-nova-text-muted">
        PROJECT NOVA — AI Emergency Response Network · Safe &amp; Verified Citizen Portal
      </footer>
    </div>
  );
}
