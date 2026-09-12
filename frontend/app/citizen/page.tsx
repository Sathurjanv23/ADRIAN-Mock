'use client';

import { TopNav } from '@/components/shared/TopNav';
import { DashboardShell } from '@/components/shared/Sidebar';
import { AuthGuard } from '@/components/shared/AuthGuard';
import { useNovaStore } from '@/lib/store/nova-store';
import { StatCard } from '@/components/emergency/StatCard';
import { EmergencyCard } from '@/components/emergency/EmergencyCard';
import { motion } from 'framer-motion';
import Link from 'next/link';
import { Radio, ClipboardList, Shield, MapPin, Clock, ChevronRight, AlertTriangle } from 'lucide-react';
import { IncidentTimeline } from '@/components/emergency/IncidentTimeline';

import { useTranslation } from '@/lib/i18n';

export default function CitizenDashboardPage() {
  const { incidents } = useNovaStore();
  const { t, localize } = useTranslation();
  // Simulate citizen's own reports
  const myReports = incidents.slice(0, 2);
  const activeReport = incidents.find((i) => i.id === 'NOV-1042');

  return (
    <AuthGuard allowedRoles={['citizen']}>
    <div className="min-h-screen bg-nova-bg">
      <TopNav role="citizen" showTicker={false} />
      <DashboardShell role="citizen">
        <div className="max-w-2xl mx-auto p-4 space-y-5">
          {/* SOS Quick Launch */}
          <motion.div
            className="p-5 rounded-2xl bg-gradient-to-br from-red-500/15 to-red-500/5 border border-red-500/30 flex flex-col items-center gap-4"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <div className="text-center">
              <p className="text-sm font-semibold text-nova-text-dim mb-1">Emergency? Report instantly.</p>
              <h2 className="text-2xl font-black text-nova-text">{t('nav.sos')}</h2>
            </div>
            <Link
              href="/citizen/sos"
              className="w-24 h-24 rounded-full bg-red-500 flex items-center justify-center font-black text-2xl text-white shadow-nova-critical hover:bg-red-600 transition-all"
            >
              <motion.span animate={{ scale: [1, 1.05, 1] }} transition={{ duration: 2, repeat: Infinity }}>
                SOS
              </motion.span>
            </Link>
            <div className="flex gap-3 text-xs text-nova-text-muted">
              <span className="flex items-center gap-1"><span>📱</span> Text</span>
              <span className="flex items-center gap-1"><span>🎙️</span> Voice</span>
              <span className="flex items-center gap-1"><span>📷</span> Image</span>
              <span className="flex items-center gap-1"><span>📍</span> GPS</span>
            </div>
          </motion.div>

          {/* Quick Actions */}
          <div className="grid grid-cols-3 gap-3">
            {[
              { href: '/citizen/report', icon: <AlertTriangle className="w-5 h-5" />, label: t('btn.report'), color: 'text-red-400 bg-red-400/10 border-red-400/20' },
              { href: '/citizen/reports', icon: <ClipboardList className="w-5 h-5" />, label: t('heading.my_reports'), color: 'text-nova-cyan bg-nova-cyan/10 border-nova-cyan/20' },
              { href: '/citizen/safety', icon: <Shield className="w-5 h-5" />, label: t('nav.safety'), color: 'text-green-400 bg-green-400/10 border-green-400/20' },
            ].map((action) => (
              <Link
                key={action.href}
                href={action.href}
                className={`flex flex-col items-center gap-2 p-4 rounded-xl border nova-glass transition-all hover:scale-105 ${action.color}`}
              >
                {action.icon}
                <span className="text-xs font-semibold text-center">{action.label}</span>
              </Link>
            ))}
          </div>

          {/* Active Report Tracker */}
          {activeReport && (
            <div className="nova-card border border-nova-cyan/20 rounded-2xl p-5">
              <div className="flex items-center gap-2 mb-4">
                <div className="w-2 h-2 rounded-full bg-nova-cyan animate-pulse" />
                <h3 className="text-sm font-bold text-nova-text">{t('btn.track_dispatch')}</h3>
                <span className="ml-auto text-xs font-mono text-nova-text-muted">{activeReport.id}</span>
              </div>
              <div className="mb-4 p-3 rounded-xl bg-nova-surface/70 border border-nova-border">
                <p className="text-xs text-nova-text-dim">{localize(activeReport.title)}</p>
                <div className="flex items-center gap-2 mt-2 text-xs text-nova-text-muted">
                  <MapPin className="w-3 h-3" />
                  <span>{localize(activeReport.location.address || activeReport.location.district)}</span>
                </div>
                {activeReport.eta !== undefined && (
                  <div className="flex items-center gap-2 mt-1 text-xs">
                    <Clock className="w-3 h-3 text-nova-high" />
                    <span className="text-nova-high font-semibold">
                      {localize('Team en route')} — {t('common.eta')} {activeReport.eta} {t('common.min')}
                    </span>
                  </div>
                )}
              </div>
              <IncidentTimeline
                currentStatus={activeReport.status}
                updates={activeReport.updates}
                compact
              />
            </div>
          )}

          {/* Weather/Risk Alert */}
          <div className="p-4 rounded-xl bg-orange-500/10 border border-orange-500/30">
            <div className="flex items-start gap-3">
              <span className="text-xl">⚠️</span>
              <div>
                <p className="text-sm font-bold text-orange-300">Flood Warning — Your Area</p>
                <p className="text-xs text-nova-text-dim mt-1">
                  Meteorological Department has issued a <strong>Red Alert</strong> for the Western Province.
                  Kelani River levels are above the alert threshold. Stay on higher ground.
                </p>
                <Link href="/citizen/safety" className="text-xs text-orange-300 hover:underline mt-2 inline-block">
                  View safety instructions →
                </Link>
              </div>
            </div>
          </div>

          {/* Preparedness Tip */}
          <div className="nova-card border border-nova-border rounded-xl p-4">
            <p className="text-xs font-bold text-nova-text-muted uppercase tracking-wider mb-3">💡 Preparedness Tip</p>
            <p className="text-sm text-nova-text-dim">
              Prepare an emergency kit: Water (1L per person per day), Food for 3 days,
              First aid kit, Flashlight, and important documents in a waterproof bag.
            </p>
            <Link href="/citizen/safety" className="text-xs text-nova-cyan hover:underline mt-2 inline-block">
              Full Safety Guide →
            </Link>
          </div>
        </div>
      </DashboardShell>
    </div>
    </AuthGuard>
  );
}
