'use client';

import { useState, useEffect } from 'react';
import { TopNav } from '@/components/shared/TopNav';
import { DashboardShell } from '@/components/shared/Sidebar';
import { AuthGuard } from '@/components/shared/AuthGuard';
import { useNovaStore } from '@/lib/store/nova-store';
import { StatCard } from '@/components/emergency/StatCard';
import { SeverityBadge } from '@/components/emergency/SeverityBadge';
import { motion } from 'framer-motion';
import { Bed, Users, Activity, Truck, Clock, AlertTriangle, HeartPulse, Phone } from 'lucide-react';
import { cn, formatTimeAgo } from '@/lib/utils';
import { toast } from 'sonner';
import Link from 'next/link';

import { useTranslation } from '@/lib/i18n';

export default function HospitalDashboardPage() {
  const { hospitals, updateHospital, currentUser, fetchHospitals } = useNovaStore();
  const { t, localize, statusLabel } = useTranslation();

  useEffect(() => {
    fetchHospitals();
  }, [fetchHospitals]);

  // Selected hospital state (defaults to user's assigned hospitalId or matched district hospital or h001)
  const userHospital = hospitals.find((h) => h.id === (currentUser as any)?.hospitalId) ||
    hospitals.find((h) => currentUser?.district && h.district?.toLowerCase() === currentUser.district.toLowerCase()) ||
    hospitals[0];

  const [selectedHospitalId, setSelectedHospitalId] = useState<string>('');

  const activeHospitalId = selectedHospitalId || userHospital?.id || 'h001';
  const myHospital = hospitals.find((h) => h.id === activeHospitalId) || userHospital || hospitals[0];

  if (!myHospital) {
    return (
      <AuthGuard allowedRoles={['hospital']}>
        <div className="min-h-screen bg-nova-bg">
          <TopNav role="hospital" />
          <DashboardShell role="hospital">
            <div className="p-12 text-center space-y-3">
              <HeartPulse className="w-12 h-12 text-nova-text-muted mx-auto opacity-40" />
              <h2 className="text-lg font-bold text-nova-text">No hospital capacity data available</h2>
              <p className="text-xs text-nova-text-dim max-w-md mx-auto">
                No active hospital facility profile is assigned or registered in the database. Real-time telemetry will appear when hospital records are connected.
              </p>
            </div>
          </DashboardShell>
        </div>
      </AuthGuard>
    );
  }

  const bedUsagePercent = Math.round(((myHospital.totalBeds - myHospital.availableBeds) / myHospital.totalBeds) * 100);
  const icuUsagePercent = Math.round(((myHospital.icuTotal - myHospital.icuAvailable) / myHospital.icuTotal) * 100);

  const updateBeds = (delta: number) => {
    const newAvailable = Math.max(0, Math.min(myHospital.totalBeds, myHospital.availableBeds + delta));
    updateHospital(myHospital.id, { availableBeds: newAvailable });
    toast.success(`Bed count updated for ${myHospital.name}: ${newAvailable} ${t('status.available')}`);
  };

  return (
    <AuthGuard allowedRoles={['hospital']}>
      <div className="min-h-screen bg-nova-bg">
        <TopNav role="hospital" />
        <DashboardShell role="hospital">
          <div className="p-5 space-y-5">
            {/* Header with Hospital Selector */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-nova-surface/40 p-4 rounded-2xl border border-nova-border">
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-2xl font-bold font-display text-nova-text">{t('heading.hospital_dashboard')}</h1>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-nova-cyan/10 border border-nova-cyan/30 text-nova-cyan uppercase">
                    {myHospital.type} Facility
                  </span>
                </div>
                <p className="text-sm text-nova-text-dim mt-0.5">{localize(myHospital.name)} — {myHospital.district} District</p>
              </div>

              <div className="flex items-center gap-3">
                {/* Hospital Switcher Dropdown */}
                <div className="flex items-center gap-2 bg-nova-surface border border-nova-border rounded-xl px-3 py-1.5 text-xs text-nova-text">
                  <HeartPulse className="w-4 h-4 text-nova-cyan flex-shrink-0" />
                  <select
                    value={myHospital.id}
                    onChange={(e) => setSelectedHospitalId(e.target.value)}
                    className="bg-transparent text-nova-text font-bold focus:outline-none cursor-pointer pr-2"
                  >
                    {hospitals.map((h) => (
                      <option key={h.id} value={h.id} className="bg-nova-surface text-nova-text">
                        🏥 {h.name} ({h.district})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="flex items-center gap-1.5 text-xs text-nova-low px-2.5 py-1 rounded-lg bg-green-500/10 border border-green-500/20">
                  <div className="w-1.5 h-1.5 rounded-full bg-nova-low animate-pulse" />
                  {t('common.ready')}
                </div>
              </div>
            </div>

            {/* KPIs */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <StatCard label={t('stats.available_beds')} value={myHospital.availableBeds} icon={<Bed className="w-4 h-4" />} variant={myHospital.availableBeds < 50 ? 'critical' : 'default'} />
              <StatCard label={t('stats.icu_available')} value={myHospital.icuAvailable} icon={<HeartPulse className="w-4 h-4" />} variant={myHospital.icuAvailable < 5 ? 'critical' : 'warning'} />
              <StatCard label={t('stats.medical_teams')} value={myHospital.emergencyTeams} icon={<Users className="w-4 h-4" />} variant="cyan" />
              <StatCard label={t('stats.incoming_cases')} value={myHospital.incomingCases.length} icon={<AlertTriangle className="w-4 h-4" />} variant={myHospital.incomingCases.length > 0 ? 'critical' : 'default'} />
            </div>

            {/* Capacity Bars */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Emergency Beds */}
              <div className="nova-card border border-nova-border rounded-xl p-4">
                <div className="flex items-center justify-between mb-3">
                  <p className="text-xs font-bold text-nova-text">{t('heading.ward_capacity')}</p>
                  <div className="flex items-center gap-1.5">
                    <button onClick={() => updateBeds(-1)} className="w-6 h-6 rounded bg-nova-surface border border-nova-border text-nova-text-dim hover:text-nova-text flex items-center justify-center text-sm">-</button>
                    <button onClick={() => updateBeds(1)} className="w-6 h-6 rounded bg-nova-surface border border-nova-border text-nova-text-dim hover:text-nova-text flex items-center justify-center text-sm">+</button>
                  </div>
                </div>
                <div className="flex items-end justify-between mb-2">
                  <span className="text-2xl font-bold font-mono text-nova-text">{myHospital.availableBeds}</span>
                  <span className="text-xs text-nova-text-muted">of {myHospital.totalBeds} total</span>
                </div>
                <div className="h-3 bg-nova-border rounded-full overflow-hidden">
                  <motion.div
                    className={cn('h-full rounded-full', bedUsagePercent > 90 ? 'bg-red-500' : bedUsagePercent > 70 ? 'bg-orange-500' : 'bg-green-500')}
                    style={{ width: `${bedUsagePercent}%` }}
                    initial={{ width: 0 }}
                    animate={{ width: `${bedUsagePercent}%` }}
                    transition={{ duration: 1 }}
                  />
                </div>
                <p className="text-xs text-nova-text-muted mt-1">{bedUsagePercent}% occupied</p>
              </div>

              {/* ICU */}
              <div className="nova-card border border-nova-border rounded-xl p-4">
                <p className="text-xs font-bold text-nova-text mb-3">{t('stats.icu_available')}</p>
                <div className="flex items-end justify-between mb-2">
                  <span className={cn('text-2xl font-bold font-mono', myHospital.icuAvailable <= 3 ? 'text-red-400' : 'text-nova-text')}>
                    {myHospital.icuAvailable}
                  </span>
                  <span className="text-xs text-nova-text-muted">of {myHospital.icuTotal} total</span>
                </div>
                <div className="h-3 bg-nova-border rounded-full overflow-hidden">
                  <motion.div
                    className={cn('h-full rounded-full', icuUsagePercent > 90 ? 'bg-red-500' : icuUsagePercent > 70 ? 'bg-orange-500' : 'bg-nova-cyan')}
                    style={{ width: `${icuUsagePercent}%` }}
                    initial={{ width: 0 }}
                    animate={{ width: `${icuUsagePercent}%` }}
                    transition={{ duration: 1 }}
                  />
                </div>
                <p className="text-xs text-nova-text-muted mt-1">{icuUsagePercent}% occupied</p>
                {myHospital.icuAvailable <= 3 && (
                  <p className="text-xs text-red-400 mt-2 font-semibold">⚠️ {t('stats.icu_available')} {t('severity.critical')}!</p>
                )}
              </div>
            </div>

            {/* Incoming Emergency Cases */}
            {myHospital.incomingCases.length > 0 && (
              <div className="nova-card border border-red-500/25 rounded-xl p-5">
                <p className="text-xs font-bold text-red-400 uppercase tracking-wider mb-4">🚨 {t('heading.incoming_emergencies')}</p>
                <div className="space-y-3">
                  {myHospital.incomingCases.map((ic) => (
                    <div key={ic.id} className="p-4 rounded-xl bg-nova-surface/70 border border-nova-border">
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <p className="text-xs font-mono text-nova-text-muted">{ic.incidentId}</p>
                          <p className="text-sm font-bold text-nova-text mt-0.5">{localize(ic.condition)}</p>
                        </div>
                        <div className="text-right">
                          <SeverityBadge severity={ic.severity} size="sm" />
                          <p className="text-sm font-bold text-nova-high mt-1">{ic.eta} {t('common.min')}</p>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {ic.requiredCare.map((care) => (
                          <span key={care} className="text-[10px] bg-nova-cyan/10 text-nova-cyan border border-nova-cyan/20 px-2 py-0.5 rounded-full">
                            {care}
                          </span>
                        ))}
                      </div>
                      <div className="flex items-center gap-2 mt-3">
                        <p className="text-xs text-nova-text-muted">Patients: {ic.patientCount}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Ambulances */}
            {(() => {
              const defaultPrefix = (myHospital?.district ? myHospital.district.substring(0, 3) : 'SL').toUpperCase();
              const ambulances = (myHospital?.ambulances && myHospital.ambulances.length > 0)
                ? myHospital.ambulances
                : [
                  { id: `amb-${myHospital.id}-1`, code: `${defaultPrefix}-AMB-01`, status: 'available', eta: null, assignedCase: null },
                  { id: `amb-${myHospital.id}-2`, code: `${defaultPrefix}-AMB-02`, status: 'dispatched', eta: 8, assignedCase: 'NOV-1041' },
                  { id: `amb-${myHospital.id}-3`, code: `${defaultPrefix}-AMB-03`, status: 'available', eta: null, assignedCase: null },
                  { id: `amb-${myHospital.id}-4`, code: `${defaultPrefix}-AMB-04`, status: 'dispatched', eta: 12, assignedCase: 'NOV-1039' },
                  { id: `amb-${myHospital.id}-5`, code: `${defaultPrefix}-AMB-05`, status: 'available', eta: null, assignedCase: null },
                  { id: `amb-${myHospital.id}-6`, code: `${defaultPrefix}-AMB-06`, status: 'maintenance', eta: null, assignedCase: null },
                ];

              return (
                <div className="nova-card border border-nova-border rounded-xl p-4">
                  <p className="text-xs font-bold text-nova-text-muted uppercase tracking-wider mb-3">{t('heading.ambulance_fleet')} ({ambulances.length} Units)</p>
                  <div className="space-y-2">
                    {ambulances.map((amb) => (
                      <div key={amb.id} className="flex items-center justify-between p-2.5 rounded-lg bg-nova-surface/50 border border-nova-border/50">
                        <div className="flex items-center gap-3">
                          <Truck className={cn('w-4 h-4', {
                            'text-green-400': amb.status === 'available',
                            'text-orange-400': amb.status === 'dispatched',
                            'text-nova-text-muted': amb.status === 'maintenance',
                          })} />
                          <div>
                            <p className="text-xs font-bold text-nova-text font-mono">{amb.code}</p>
                            {amb.assignedCase && <p className="text-[10px] text-nova-cyan font-mono">→ {amb.assignedCase}</p>}
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          {amb.eta && <span className="text-xs text-nova-high font-semibold">{amb.eta} {t('common.min')}</span>}
                          <span className={cn('text-[10px] font-bold px-2 py-0.5 rounded-full', {
                            'bg-green-500/10 text-green-400 border border-green-500/20': amb.status === 'available',
                            'bg-orange-500/10 text-orange-400 border border-orange-500/20': amb.status === 'dispatched',
                            'bg-nova-border text-nova-text-muted border border-nova-border': amb.status === 'maintenance',
                          })}>
                            {statusLabel(amb.status)}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })()}
          </div>
        </DashboardShell>
      </div>
    </AuthGuard>
  );
}
