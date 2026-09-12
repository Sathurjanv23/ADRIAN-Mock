'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { TopNav } from '@/components/shared/TopNav';
import { DashboardShell } from '@/components/shared/Sidebar';
import { useNovaStore } from '@/lib/store/nova-store';
import { SeverityBadge } from '@/components/emergency/SeverityBadge';
import { motion } from 'framer-motion';
import { HeartPulse, Bed, Truck, ClipboardList, AlertTriangle, CheckCircle, Clock, Plus, Minus } from 'lucide-react';
import { cn, formatDateTime } from '@/lib/utils';
import { toast } from 'sonner';
import { useTranslation } from '@/lib/i18n';

export default function HospitalOperationsCatchAll() {
  const params = useParams<{ slug: string[] }>();
  const router = useRouter();
  const slug = params?.slug?.[0] || '';
  const { t, localize, statusLabel } = useTranslation();

  const { hospitals, updateHospital, currentUser } = useNovaStore();
  const [selectedHospitalId, setSelectedHospitalId] = useState<string>('');

  const userHospital = hospitals.find((h) => h.id === (currentUser as any)?.hospitalId) ||
                       hospitals.find((h) => currentUser?.district && h.district?.toLowerCase() === currentUser.district.toLowerCase()) ||
                       hospitals[0];

  const activeHospitalId = selectedHospitalId || userHospital?.id || 'h001';
  const myHospital = hospitals.find((h) => h.id === activeHospitalId) || userHospital || hospitals[0];

  const [triageFilter, setTriageFilter] = useState<'all' | 'critical' | 'high'>('all');

  const updateBeds = (delta: number) => {
    const newAvailable = Math.max(0, Math.min(myHospital.totalBeds, myHospital.availableBeds + delta));
    updateHospital(myHospital.id, { availableBeds: newAvailable });
    toast.success(`Bed availability updated: ${newAvailable} ${t('status.available')}`);
  };

  const updateICU = (delta: number) => {
    const newAvailable = Math.max(0, Math.min(myHospital.icuTotal, myHospital.icuAvailable + delta));
    updateHospital(myHospital.id, { icuAvailable: newAvailable });
    toast.success(`ICU availability updated: ${newAvailable} ${t('status.available')}`);
  };

  // 1. INCOMING EMERGENCIES PANEL
  const renderEmergencies = () => {
    const { incidents } = useNovaStore.getState();
    const hospitalDistrict = (myHospital?.district || '').toLowerCase();

    // Use hospital's own incomingCases if available, otherwise strictly partition district incidents per facility
    const cases = (myHospital?.incomingCases && myHospital.incomingCases.length > 0)
      ? myHospital.incomingCases
      : incidents.filter(i => {
          const incDistrict = i.location?.district?.toLowerCase();
          if (!incDistrict || incDistrict !== hospitalDistrict) return false;
          if (myHospital.id === 'h001') return i.id.endsWith('1') || i.id.endsWith('3') || i.id.endsWith('5') || i.type === 'medical';
          if (myHospital.id === 'h002') return i.id.endsWith('2') || i.id.endsWith('4') || i.id.endsWith('6') || i.type === 'flood';
          if (myHospital.id === 'h003') return i.type === 'flood' || i.type === 'fire';
          if (myHospital.id === 'h004') return i.type === 'landslide' || i.type === 'medical';
          return true;
        }).filter(i => i.severity === 'critical' || i.status === 'responding' || i.status === 'en_route').map((inc, index) => ({
          id: `ic-${myHospital.id}-${inc.id}`,
          incidentId: inc.trackingCode || inc.id,
          condition: inc.title || 'Emergency Casualty',
          requiredCare: [inc.type.toUpperCase(), 'Emergency Room Triage'],
          severity: inc.severity || 'critical',
          eta: inc.eta || (index + 1) * 5,
        }));

    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-nova-text flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-red-400 animate-pulse" /> {t('heading.incoming_emergencies')}
          </h2>
          <span className="text-xs text-nova-cyan font-mono bg-nova-cyan/10 border border-nova-cyan/20 px-2.5 py-1 rounded-full">
            {myHospital.name} ({myHospital.district})
          </span>
        </div>

        {cases.length === 0 ? (
          <div className="nova-card border border-nova-border rounded-xl p-10 text-center text-nova-text-muted text-xs">
            No active incoming emergency casualties reported for {myHospital.name} at this time.
          </div>
        ) : (
          <div className="space-y-3">
            {cases.map((ic) => (
              <div key={ic.id} className="nova-card border border-nova-border rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <span className="text-[10px] font-mono text-nova-cyan">{ic.incidentId}</span>
                  <p className="text-sm font-bold text-nova-text mt-0.5">{localize(ic.condition)}</p>
                  <p className="text-xs text-nova-text-muted mt-1">Care Required: {ic.requiredCare.join(', ')}</p>
                </div>
                <div className="flex items-center gap-3">
                  <SeverityBadge severity={ic.severity} size="sm" pulse={ic.severity === 'critical'} />
                  <div className="text-right">
                    <p className="text-xs text-nova-text-muted">{t('common.eta')}</p>
                    <p className="text-sm font-bold text-nova-high">{ic.eta} {t('common.min')}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };

  // 2. TRIAGE QUEUE PANEL
  const renderTriage = () => {
    const { incidents } = useNovaStore.getState();
    const hospitalDistrict = (myHospital?.district || '').toLowerCase();

    const cases = (myHospital?.incomingCases && myHospital.incomingCases.length > 0)
      ? myHospital.incomingCases
      : incidents.filter(i => {
          const incDistrict = i.location?.district?.toLowerCase();
          if (!incDistrict || incDistrict !== hospitalDistrict) return false;
          if (myHospital.id === 'h001') return i.id.endsWith('1') || i.id.endsWith('3') || i.id.endsWith('5');
          if (myHospital.id === 'h002') return i.id.endsWith('2') || i.id.endsWith('4') || i.id.endsWith('6');
          if (myHospital.id === 'h003') return i.type === 'flood';
          if (myHospital.id === 'h004') return i.type === 'landslide';
          return true;
        }).filter(i => i.severity === 'critical' || i.severity === 'high').map((inc, index) => ({
          id: `triage-${myHospital.id}-${inc.id}`,
          incidentId: inc.trackingCode || inc.id,
          condition: inc.title || 'Trauma Patient',
          requiredCare: [inc.type.toUpperCase()],
          severity: inc.severity || 'high',
          eta: inc.eta || (index + 1) * 4,
        }));

    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-nova-text flex items-center gap-2">
            <ClipboardList className="w-5 h-5 text-nova-cyan" /> {t('heading.triage_queue')}
          </h2>
          <span className="text-xs text-nova-cyan font-mono bg-nova-cyan/10 border border-nova-cyan/20 px-2.5 py-1 rounded-full">
            {myHospital.name} Queue
          </span>
        </div>

        {cases.length === 0 ? (
          <div className="nova-card border border-nova-border rounded-xl p-10 text-center text-nova-text-muted text-xs">
            Triage queue is clear for {myHospital.name}. No high-priority casualties waiting.
          </div>
        ) : (
          <div className="nova-card border border-nova-border rounded-xl overflow-hidden">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-nova-border bg-nova-surface/40 text-[10px] font-bold text-nova-text-muted uppercase tracking-wider">
                  <th className="p-4">Patient Code</th>
                  <th className="p-4">{t('stats.status')}</th>
                  <th className="p-4">Medical Condition</th>
                  <th className="p-4">{t('heading.triage_action')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-nova-border/50 text-sm text-nova-text">
                {cases.map((ic, i) => (
                  <tr key={ic.id} className="hover:bg-nova-surface/10 transition-colors">
                    <td className="p-4 font-mono text-nova-cyan">PT-{1024 + i}</td>
                    <td className="p-4"><SeverityBadge severity={ic.severity} size="sm" /></td>
                    <td className="p-4">{localize(ic.condition)}</td>
                    <td className="p-4 text-xs text-nova-high font-bold">Priority {i + 1}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  };

  // 3. CAPACITY CONTROL PANEL
  const renderCapacity = () => {
    return (
      <div className="space-y-4">
        <h2 className="text-lg font-bold text-nova-text flex items-center gap-2">
          <Bed className="w-5 h-5 text-nova-cyan" /> {t('heading.ward_capacity')}
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="nova-card border border-nova-border rounded-xl p-5 space-y-4">
            <div>
              <p className="text-sm font-bold text-nova-text">{t('stats.available_beds')}</p>
              <p className="text-xs text-nova-text-muted mt-0.5">Directly controls available emergency beds</p>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-3xl font-bold font-mono text-nova-text">{myHospital.availableBeds}</span>
              <div className="flex items-center gap-2">
                <button onClick={() => updateBeds(-1)} className="w-10 h-10 bg-nova-surface border border-nova-border rounded-lg text-nova-text hover:bg-nova-surface2 transition-colors flex items-center justify-center"><Minus className="w-4 h-4" /></button>
                <button onClick={() => updateBeds(1)} className="w-10 h-10 bg-nova-surface border border-nova-border rounded-lg text-nova-text hover:bg-nova-surface2 transition-colors flex items-center justify-center"><Plus className="w-4 h-4" /></button>
              </div>
            </div>
          </div>
          <div className="nova-card border border-nova-border rounded-xl p-5 space-y-4">
            <div>
              <p className="text-sm font-bold text-nova-text">{t('stats.icu_available')}</p>
              <p className="text-xs text-nova-text-muted mt-0.5">Critical care patient ventilators count</p>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-3xl font-bold font-mono text-nova-text">{myHospital.icuAvailable}</span>
              <div className="flex items-center gap-2">
                <button onClick={() => updateICU(-1)} className="w-10 h-10 bg-nova-surface border border-nova-border rounded-lg text-nova-text hover:bg-nova-surface2 transition-colors flex items-center justify-center"><Minus className="w-4 h-4" /></button>
                <button onClick={() => updateICU(1)} className="w-10 h-10 bg-nova-surface border border-nova-border rounded-lg text-nova-text hover:bg-nova-surface2 transition-colors flex items-center justify-center"><Plus className="w-4 h-4" /></button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  // 4. AMBULANCE LOG PANEL
  const renderAmbulances = () => {
    const defaultPrefix = (myHospital?.district ? myHospital.district.substring(0,3) : 'SL').toUpperCase();
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

    const available  = ambulances.filter(a => a.status === 'available').length;
    const dispatched = ambulances.filter(a => a.status === 'dispatched').length;
    const maintenance = ambulances.filter(a => a.status === 'maintenance').length;

    return (
      <div className="space-y-5">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-nova-text flex items-center gap-2">
            <Truck className="w-5 h-5 text-nova-cyan" /> {t('heading.ambulance_fleet')}
          </h2>
          <div className="text-xs text-nova-text-muted">
            {myHospital?.name}
          </div>
        </div>

        {/* Fleet Summary Stats */}
        {ambulances.length > 0 && (
          <div className="grid grid-cols-3 gap-3">
            <div className="nova-card border border-green-500/20 rounded-xl p-3 text-center bg-green-500/5">
              <p className="text-2xl font-bold font-mono text-green-400">{available}</p>
              <p className="text-[10px] text-nova-text-muted uppercase tracking-wider mt-0.5">Available</p>
            </div>
            <div className="nova-card border border-orange-500/20 rounded-xl p-3 text-center bg-orange-500/5">
              <p className="text-2xl font-bold font-mono text-orange-400">{dispatched}</p>
              <p className="text-[10px] text-nova-text-muted uppercase tracking-wider mt-0.5">Dispatched</p>
            </div>
            <div className="nova-card border border-nova-border rounded-xl p-3 text-center">
              <p className="text-2xl font-bold font-mono text-nova-text-muted">{maintenance}</p>
              <p className="text-[10px] text-nova-text-muted uppercase tracking-wider mt-0.5">Maintenance</p>
            </div>
          </div>
        )}

        {/* Fleet Grid */}
        {ambulances.length === 0 ? (
          <div className="nova-card border border-nova-border rounded-xl p-10 flex flex-col items-center gap-3 text-center">
            <Truck className="w-12 h-12 text-nova-text-muted opacity-40" />
            <p className="text-sm font-semibold text-nova-text-dim">No Ambulances in Fleet</p>
            <p className="text-xs text-nova-text-muted max-w-xs">
              This hospital has no registered ambulance units. Contact the system administrator to add vehicles to the fleet.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {ambulances.map((amb) => (
              <motion.div
                key={amb.id}
                className="nova-card border border-nova-border rounded-xl p-4 flex justify-between items-center gap-3 hover:border-nova-border2 transition-all"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
              >
                <div className="flex items-center gap-3">
                  <div className={cn('w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0', {
                    'bg-green-500/10': amb.status === 'available',
                    'bg-orange-500/10': amb.status === 'dispatched',
                    'bg-nova-surface': amb.status === 'maintenance',
                  })}>
                    <Truck className={cn('w-4 h-4', {
                      'text-green-400': amb.status === 'available',
                      'text-orange-400': amb.status === 'dispatched',
                      'text-nova-text-muted': amb.status === 'maintenance',
                    })} />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-nova-text font-mono">{amb.code}</p>
                    {amb.assignedCase ? (
                      <p className="text-[10px] text-nova-cyan mt-0.5">→ Case: {amb.assignedCase}</p>
                    ) : (
                      <p className="text-[10px] text-nova-text-muted mt-0.5">GPS tracking active</p>
                    )}
                    {amb.eta != null && (
                      <p className="text-[10px] text-nova-high mt-0.5 font-semibold">ETA: {amb.eta} min</p>
                    )}
                  </div>
                </div>
                <span className={cn('text-[9px] font-bold px-2.5 py-1 rounded-full border uppercase tracking-wide flex-shrink-0', {
                  'bg-green-500/10 border-green-500/30 text-green-400': amb.status === 'available',
                  'bg-orange-500/10 border-orange-500/30 text-orange-400': amb.status === 'dispatched',
                  'bg-nova-surface border-nova-border text-nova-text-muted': amb.status === 'maintenance',
                })}>
                  {amb.status === 'available' ? 'Available' : amb.status === 'dispatched' ? 'Dispatched' : 'Maintenance'}
                </span>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-nova-bg">
      <TopNav role="hospital" />
      <DashboardShell role="hospital">
        <div className="p-6 space-y-6">
          {/* Facility Selector Header Bar */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 bg-nova-surface/40 p-3.5 rounded-2xl border border-nova-border">
            <div className="flex items-center gap-2">
              <span className="text-[10px] font-bold px-2.5 py-1 rounded-full bg-nova-cyan/10 border border-nova-cyan/30 text-nova-cyan uppercase font-mono">
                {myHospital.type} Facility
              </span>
              <span className="text-xs font-bold text-nova-text">{localize(myHospital.name)}</span>
              <span className="text-xs text-nova-text-muted">• {myHospital.district}</span>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-xs text-nova-text-muted">Switch Facility:</span>
              <div className="flex items-center gap-2 bg-nova-surface border border-nova-border rounded-xl px-3 py-1 text-xs text-nova-text">
                <HeartPulse className="w-3.5 h-3.5 text-nova-cyan flex-shrink-0" />
                <select
                  value={myHospital.id}
                  onChange={(e) => setSelectedHospitalId(e.target.value)}
                  className="bg-transparent text-nova-text font-bold focus:outline-none cursor-pointer pr-1"
                >
                  {hospitals.map((h) => (
                    <option key={h.id} value={h.id} className="bg-nova-surface text-nova-text">
                      🏥 {h.name} ({h.district})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {slug === 'emergencies' && renderEmergencies()}
          {slug === 'triage' && renderTriage()}
          {slug === 'capacity' && renderCapacity()}
          {slug === 'ambulances' && renderAmbulances()}
        </div>
      </DashboardShell>
    </div>
  );
}
