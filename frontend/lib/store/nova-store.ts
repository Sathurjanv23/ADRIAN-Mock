'use client';

import { create } from 'zustand';
import { subscribeWithSelector } from 'zustand/middleware';
import type {
  Incident, RescueTeam, Hospital, Resource, RiskPrediction,
  Notification, OperationalAlert, SimulationState, User, UserRole, Language
} from '@/types';
import { getToken } from '@/lib/auth';


// ─── API Base ─────────────────────────────────────────────────

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

export class ApiError extends Error {
  status: number;
  code?: string;
  data?: unknown;

  constructor(message: string, status: number, code?: string, data?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.data = data;
  }
}

async function apiFetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const token = getToken();
  const headers = new Headers(options?.headers);
  if (!headers.has('Content-Type') && !(options?.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (res.status === 204) {
    return null as unknown as T;
  }

  if (!res.ok) {
    let errMsg = `Error ${res.status}`;
    let errCode = `HTTP_${res.status}`;
    let errData: any = null;

    try {
      const text = await res.text();
      if (text) {
        try {
          const json = JSON.parse(text);
          errMsg = json.message || json.error || errMsg;
          errCode = json.code || errCode;
          errData = json.data ?? json;
        } catch {
          errMsg = text;
        }
      }
    } catch { }

    throw new ApiError(errMsg, res.status, errCode, errData);
  }

  const text = await res.text();
  if (!text) {
    return null as unknown as T;
  }

  const json = JSON.parse(text);
  // Backend wraps in { data, success, ... }
  return json.data !== undefined ? json.data : json;
}

// ─── App State ──────────────────────────────────────────────

interface AppState {
  // Auth
  currentUser: User | null;
  isAuthenticated: boolean;
  login: (user: User) => void;
  logout: () => void;

  // Language
  language: Language;
  setLanguage: (lang: Language) => void;

  // Loading states
  loading: {
    incidents: boolean;
    teams: boolean;
    hospitals: boolean;
    resources: boolean;
    predictions: boolean;
    notifications: boolean;
    alerts: boolean;
  };
  errors: {
    incidents: string | null;
    teams: string | null;
    resources: string | null;
    hospitals: string | null;
    alerts: string | null;
  };

  // Incidents
  incidents: Incident[];
  fetchIncidents: (params?: { severity?: string; status?: string }) => Promise<void>;
  addIncident: (incident: Incident) => void;
  updateIncident: (id: string, updates: Partial<Incident>) => void;
  acceptMission: (incidentId: string, teamId: string, teamName?: string) => Promise<void>;
  updateMissionStatus: (incidentId: string, status: string, teamId?: string, notes?: string) => Promise<void>;

  // Teams
  rescueTeams: RescueTeam[];
  fetchTeams: () => Promise<void>;
  updateTeam: (id: string, updates: Partial<RescueTeam>) => void;

  // Hospitals
  hospitals: Hospital[];
  fetchHospitals: () => Promise<void>;
  updateHospital: (id: string, updates: Partial<Hospital>) => void;

  // Resources
  resources: Resource[];
  fetchResources: () => Promise<void>;
  updateResource: (id: string, updates: Partial<Resource>) => void;

  // Risk Predictions
  riskPredictions: RiskPrediction[];
  fetchPredictions: () => Promise<void>;
  updateRiskPrediction: (id: string, updates: Partial<RiskPrediction>) => void;

  // Notifications
  notifications: Notification[];
  fetchNotifications: () => Promise<void>;
  addNotification: (notif: Notification) => void;
  markNotificationRead: (id: string) => void;
  markAllRead: () => void;
  unreadCount: number;
  alerts: OperationalAlert[];
  fetchAlerts: () => Promise<void>;

  // Initial data fetch (called once on app mount)
  fetchAllData: () => Promise<void>;

  // Simulation (kept as frontend-only feature)
  simulation: SimulationState;
  startSimulation: () => void;
  stopSimulation: () => void;
  advanceSimulation: () => void;

  // Global stats (live counters)
  stats: {
    activeIncidents: number;
    criticalIncidents: number;
    peopleAffected: number;
    teamsDeployed: number;
    avgResponseTime: number;
    aiPredictions: number;
  };
  refreshStats: () => void;
}

// ─── Simulation Steps (frontend-only — no backend needed) ─────

const SIMULATION_STEPS_DATA = [
  {
    id: 1,
    title: 'Rainfall intensifying over Western Province',
    description: 'Meteorological AI detects abnormal rainfall pattern. Zone 04 flood risk rising.',
    delay: 0,
    action: (state: AppState) => {
      state.updateRiskPrediction('rp001', { floodRisk: 82, overallRisk: 78, rainfall: 187, riverLevel: 4.8 });
      state.addNotification({
        id: `sim-notif-${Date.now()}`,
        type: 'prediction_alert',
        title: '⚠️ SIMULATION: Zone 04 Flood Risk Rising',
        message: 'AI detected critical rainfall pattern. Zone 04 at 82% flood risk.',
        severity: 'critical',
        read: false,
        createdAt: new Date().toISOString(),
      });
    },
  },
  {
    id: 2,
    title: 'River Station 02 exceeds alert threshold',
    description: 'Kelani River at 5.2m — Emergency threshold breached. AI escalates risk.',
    delay: 3000,
    action: (state: AppState) => {
      state.updateRiskPrediction('rp001', { floodRisk: 88, riverLevel: 5.2, riskLevel: 'critical' });
    },
  },
  {
    id: 3,
    title: 'Citizen SOS Report received',
    description: 'Tamil-language voice + image report received. AI processing multimodal input.',
    delay: 6000,
    action: (state: AppState) => {
      const newIncident: Incident = {
        id: 'NOV-SIM-001',
        type: 'flood',
        severity: 'critical',
        status: 'reported',
        title: '[SIM] SIMULATION: House flooding — Kelani River bank',
        description: 'Water entering ground floor. Family of 4 trapped. Elderly father cannot walk.',
        location: { lat: 6.9450, lng: 79.9700, address: 'Simulation: 12 River View Lane, Kaduwela', district: 'Colombo', zone: 'Zone 04' },
        reportedBy: 'sim-citizen',
        reporterName: 'Simulation Citizen',
        reportedAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        peopleAffected: 4,
        notes: [],
        attachments: [],
        updates: [{ id: 'sim-upd1', status: 'reported', message: 'SIMULATION incident received.', updatedBy: 'System', updatedAt: new Date().toISOString() }],
        resourcesAllocated: [],
        priority: 1,
        isSimulation: true,
      };
      state.addIncident(newIncident);
      state.addNotification({
        id: `sim-notif-${Date.now() + 1}`,
        type: 'critical_incident',
        title: '🔴 SIMULATION: New Critical Incident',
        message: 'Flood emergency reported via voice+image in Zone 04. AI analyzing.',
        severity: 'critical',
        read: false,
        createdAt: new Date().toISOString(),
        relatedId: 'NOV-SIM-001',
        relatedType: 'incident',
      });
    },
  },
  {
    id: 4,
    title: 'NOVA AI analyzing multimodal report',
    description: 'Speech-to-text, image analysis, language normalization — Critical detected.',
    delay: 10000,
    action: (state: AppState) => {
      state.updateIncident('NOV-SIM-001', {
        status: 'ai_analyzed',
        severity: 'critical',
        aiAnalysis: {
          id: 'ai-sim-001',
          incidentId: 'NOV-SIM-001',
          emergencyType: 'flood',
          severity: 'critical',
          confidenceScore: 93,
          peopleAffected: 4,
          vulnerablePersons: [{ type: 'elderly', count: 1 }],
          requiredResources: [{ type: 'Rescue Boat', quantity: 1, priority: 'immediate' }],
          recommendedAction: 'Deploy nearest rescue team immediately.',
          detectedLanguage: 'ta',
          processedAt: new Date().toISOString(),
          inputModalities: ['voice', 'image'],
          riskFactors: ['Elderly occupant', 'Rising water'],
          estimatedResponseTime: 10,
        },
      });
    },
  },
  {
    id: 5,
    title: 'Incident appears on Live Map',
    description: 'Critical incident marker pulsing on command center GIS map.',
    delay: 13000,
    action: (state: AppState) => {
      state.updateIncident('NOV-SIM-001', { status: 'prioritized', priority: 1 });
    },
  },
  {
    id: 6,
    title: 'AI assigns Priority: CRITICAL',
    description: 'NOVA Copilot recommends Team Alpha based on proximity and equipment.',
    delay: 16000,
    action: (state: AppState) => {
      state.addNotification({
        id: `sim-notif-${Date.now() + 2}`,
        type: 'critical_incident',
        title: '🤖 AI Recommendation: Deploy Team Alpha',
        message: 'NOVA Copilot: Team Alpha recommended for NOV-SIM-001. Distance: 1.8km. ETA: 6 min.',
        severity: 'critical',
        read: false,
        createdAt: new Date().toISOString(),
      });
    },
  },
  {
    id: 7,
    title: 'Team Alpha dispatched to incident',
    description: 'Emergency officer approves AI recommendation. Team Alpha assigned and en route.',
    delay: 20000,
    action: (state: AppState) => {
      state.updateIncident('NOV-SIM-001', {
        status: 'en_route',
        assignedTeam: 'RT-ALPHA-01',
        assignedTeamName: 'Team Alpha',
        eta: 6,
        updates: [
          { id: 'sim-upd2', status: 'en_route', message: '[SIM] Team Alpha dispatched. ETA: 6 minutes.', updatedBy: 'Officer', updatedAt: new Date().toISOString() },
        ],
      });
      state.updateTeam('RT-ALPHA-01', { status: 'en_route', currentIncident: 'NOV-SIM-001', eta: 6 });
    },
  },
  {
    id: 8,
    title: 'Hospital alerted for incoming patient',
    description: 'Kelaniya Hospital receives automatic alert for elderly patient.',
    delay: 23000,
    action: (state: AppState) => {
      state.addNotification({
        id: `sim-notif-${Date.now() + 3}`,
        type: 'hospital_capacity',
        title: '🏥 Hospital Alert: Incoming Patient',
        message: 'Kelaniya Hospital: Incoming elderly patient from NOV-SIM-001. Prepare emergency bay.',
        severity: 'high',
        read: false,
        createdAt: new Date().toISOString(),
        relatedId: 'h003',
        relatedType: 'hospital',
      });
    },
  },
  {
    id: 9,
    title: 'Resources allocated automatically',
    description: 'AI deploys rescue boat and medical kit. Water supply reserve activated.',
    delay: 26000,
    action: (state: AppState) => {
      const r001 = state.resources.find(r => r.id === 'r001');
      if (r001) {
        state.updateResource('r001', { deployed: r001.deployed + 1, available: r001.available - 1 });
      }
    },
  },
  {
    id: 10,
    title: 'Citizen notified: Team En Route',
    description: 'Citizen tracking timeline updated. \"Rescue Team En Route — ETA 6 minutes.\"',
    delay: 29000,
    action: () => { /* tracking update visual */ },
  },
  {
    id: 11,
    title: 'Command center live update',
    description: 'All dashboards reflect real-time status. River level continues rising.',
    delay: 33000,
    action: (state: AppState) => {
      state.updateRiskPrediction('rp001', { riverLevel: 5.6, floodRisk: 91 });
    },
  },
  {
    id: 12,
    title: 'Team Alpha on scene',
    description: 'Rescue team arrives. Extraction operation begins.',
    delay: 40000,
    action: (state: AppState) => {
      state.updateIncident('NOV-SIM-001', { status: 'responding', eta: 0 });
      state.updateTeam('RT-ALPHA-01', { status: 'on_scene' });
    },
  },
  {
    id: 13,
    title: 'After-action analytics generated',
    description: 'NOVA AI compiles operational report. Response time: 10.2 minutes.',
    delay: 48000,
    action: (state: AppState) => {
      state.addNotification({
        id: `sim-notif-${Date.now() + 4}`,
        type: 'simulation',
        title: '✅ SIMULATION COMPLETE',
        message: 'Flood response simulation completed. Avg response time: 10.2 min. 4 persons assisted.',
        severity: 'low',
        read: false,
        createdAt: new Date().toISOString(),
      });
    },
  },
];

// ─── Store ───────────────────────────────────────────────────

let fetchAllDataPromise: Promise<void> | null = null;
const acceptingMissionIds = new Set<string>();

export const useNovaStore = create<AppState>()(
  subscribeWithSelector((set, get) => ({
    // Auth
    currentUser: null,
    isAuthenticated: false,
    login: (user) => set({ currentUser: user, isAuthenticated: true }),
    logout: () => set({ currentUser: null, isAuthenticated: false }),

    // Language
    language: 'en',
    setLanguage: (lang) => {
      if (typeof window !== 'undefined') {
        try {
          localStorage.setItem('adrian_lang', lang);
        } catch {}
      }
      set({ language: lang });
    },

    // Loading states
    loading: {
      incidents: false,
      teams: false,
      hospitals: false,
      resources: false,
      predictions: false,
      notifications: false,
      alerts: false,
    },
    errors: {
      incidents: null,
      teams: null,
      resources: null,
      hospitals: null,
      alerts: null,
    },

    // ─── Incidents ──────────────────────────────────────────
    incidents: [],
    fetchIncidents: async (params) => {
      set(s => ({ loading: { ...s.loading, incidents: true }, errors: { ...s.errors, incidents: null } }));
      try {
        const query = params
          ? '?' + new URLSearchParams(params as Record<string, string>).toString()
          : '';
        const incidents = await apiFetch<Incident[]>(`/incidents${query}`);
        if (Array.isArray(incidents)) {
          // Merge with local state to avoid wiping out freshly dispatched reports
          set((state) => {
            const backendIds = new Set(incidents.map((i) => i.id));
            const backendCodes = new Set(incidents.map((i) => i.trackingCode).filter(Boolean));
            const localOnly = state.incidents.filter(
              (i) => !backendIds.has(i.id) && (!i.trackingCode || !backendCodes.has(i.trackingCode))
            );
            return { incidents: [...localOnly, ...incidents] };
          });
        }
      } catch (err) {
        console.warn('Backend incidents API error:', err);
        set(s => ({ errors: { ...s.errors, incidents: err instanceof Error ? err.message : 'Unable to load incidents.' } }));
      } finally {
        set(s => ({ loading: { ...s.loading, incidents: false } }));
        get().refreshStats();
      }
    },
    addIncident: (incident) =>
      set((state) => {
        // Prevent duplicate incidents by id or trackingCode
        const filtered = state.incidents.filter(
          (i) => i.id !== incident.id && (!incident.trackingCode || i.trackingCode !== incident.trackingCode)
        );
        const updated = [incident, ...filtered];

        // Link incident to rescue team if assigned
        let updatedTeams = state.rescueTeams;
        const effectiveTeamId = incident.assignedTeamId || incident.assignedTeam;
        if (effectiveTeamId) {
          updatedTeams = state.rescueTeams.map((t) => {
            if (t.id === effectiveTeamId) {
              return {
                ...t,
                status: (t.status === 'available' ? 'assigned' : t.status) as any,
                currentIncident: incident.id,
                lastUpdated: new Date().toISOString(),
              };
            }
            return t;
          });
        }

        // Broadcast to other open browser tabs
        if (typeof window !== 'undefined' && 'BroadcastChannel' in window) {
          try {
            const bc = new BroadcastChannel('nova-emergency-bus');
            bc.postMessage({ type: 'incident_created', data: incident });
            bc.close();
          } catch { }
        }
        return { incidents: updated, rescueTeams: updatedTeams };
      }),
    updateIncident: (id, updates) =>
      set((state) => {
        const updated = state.incidents.map((inc) =>
          inc.id === id || inc.trackingCode === id ? { ...inc, ...updates, updatedAt: new Date().toISOString() } : inc
        );
        const updatedInc = updated.find((inc) => inc.id === id || inc.trackingCode === id);
        // Broadcast to other open browser tabs
        if (updatedInc && typeof window !== 'undefined' && 'BroadcastChannel' in window) {
          try {
            const bc = new BroadcastChannel('nova-emergency-bus');
            bc.postMessage({ type: 'incident_updated', data: updatedInc });
            bc.close();
          } catch { }
        }
        return { incidents: updated };
      }),

    acceptMission: async (incidentId, teamId, teamName) => {
      if (acceptingMissionIds.has(incidentId)) {
        throw new ApiError('Mission acceptance is already in progress.', 409, 'REQUEST_IN_PROGRESS');
      }

      const state = get();
      const incident = state.incidents.find((item) => item.id === incidentId || item.trackingCode === incidentId);
      const team = state.rescueTeams.find((item) => item.id === teamId);

      if (incident) {
        const normalizedStatus = incident.status.toLowerCase();
        if (['resolved', 'closed', 'cancelled'].includes(normalizedStatus)) {
          throw new ApiError('This mission is no longer active.', 409, 'INVALID_STATUS_TRANSITION');
        }
        const assignedTeamId = incident.assignedTeamId || incident.assignedTeam;
        if (assignedTeamId && assignedTeamId !== teamId && ['acknowledged', 'en_route', 'on_scene', 'transporting', 'responding'].includes(normalizedStatus)) {
          throw new ApiError('This mission has already been accepted by another team.', 409, 'INCIDENT_ALREADY_ASSIGNED');
        }
      }

      if (team) {
        if (team.status === 'unavailable') {
          throw new ApiError(`Rescue team '${team.name}' is currently unavailable.`, 409, 'TEAM_BUSY');
        }
        const isCurrentIncident = team.currentIncident === incidentId
          || team.currentIncident === incident?.id
          || team.currentIncident === incident?.trackingCode;
        if (team.currentIncident && !isCurrentIncident && team.status !== 'available') {
          throw new ApiError(`Rescue team '${team.name}' is already assigned to another mission.`, 409, 'TEAM_BUSY');
        }
      }

      acceptingMissionIds.add(incidentId);
      try {
        // 1. Send request to backend and await confirmed entity
        const confirmedIncident = await apiFetch<Incident>(`/incidents/${incidentId}/accept`, {
          method: 'POST',
          body: JSON.stringify({ teamId, teamName }),
        });

        // 2. Update store only upon confirmed backend response
        set((state) => {
          const updatedIncidents = state.incidents.map((inc) =>
            inc.id === confirmedIncident.id || (confirmedIncident.trackingCode && inc.trackingCode === confirmedIncident.trackingCode)
              ? confirmedIncident
              : inc
          );

          const updatedTeams = state.rescueTeams.map((t) => {
            if (t.id === teamId) {
              return { ...t, status: 'assigned' as const, currentIncident: confirmedIncident.id, lastUpdated: new Date().toISOString() };
            }
            return t;
          });

          return { incidents: updatedIncidents, rescueTeams: updatedTeams };
        });

        // 3. Broadcast confirmed update to other tabs
        if (typeof window !== 'undefined' && 'BroadcastChannel' in window) {
          try {
            const bc = new BroadcastChannel('nova-emergency-bus');
            bc.postMessage({
              type: 'mission_accepted',
              data: confirmedIncident,
            });
            bc.close();
          } catch { }
        }

        // 4. Refresh both incidents and teams from server
        await Promise.allSettled([get().fetchIncidents(), get().fetchTeams()]);
        get().refreshStats();
      } catch (err: any) {
        // Re-throw the original error (preserving ApiError status/code for UI)
        throw err;
      } finally {
        acceptingMissionIds.delete(incidentId);
      }
    },

    updateMissionStatus: async (incidentId, status, teamId, notes) => {
      try {
        // 1. Send request to backend and await confirmed entity
        const confirmedIncident = await apiFetch<Incident>(`/incidents/${incidentId}/status`, {
          method: 'POST',
          body: JSON.stringify({ status, teamId, notes }),
        });

        // 2. Update store only upon confirmed backend response
        set((state) => {
          const updatedIncidents = state.incidents.map((inc) =>
            inc.id === confirmedIncident.id || (confirmedIncident.trackingCode && inc.trackingCode === confirmedIncident.trackingCode)
              ? confirmedIncident
              : inc
          );

          const updatedTeams = state.rescueTeams.map((t) => {
            if (t.id === teamId || t.currentIncident === confirmedIncident.id) {
              if (confirmedIncident.status === 'resolved') {
                return { ...t, status: 'available' as const, currentIncident: undefined, lastUpdated: new Date().toISOString() };
              } else if (confirmedIncident.status === 'on_scene') {
                return { ...t, status: 'on_scene' as const, lastUpdated: new Date().toISOString() };
              } else if (confirmedIncident.status === 'en_route') {
                return { ...t, status: 'en_route' as const, lastUpdated: new Date().toISOString() };
              }
            }
            return t;
          });

          return { incidents: updatedIncidents, rescueTeams: updatedTeams };
        });

        // 3. Broadcast confirmed update to other tabs
        if (typeof window !== 'undefined' && 'BroadcastChannel' in window) {
          try {
            const bc = new BroadcastChannel('nova-emergency-bus');
            bc.postMessage({
              type: 'mission_status_updated',
              data: confirmedIncident,
            });
            bc.close();
          } catch { }
        }

        // 4. Refresh both incidents and teams from server after key status changes
        await Promise.allSettled([get().fetchIncidents(), get().fetchTeams()]);
        get().refreshStats();
      } catch (err: any) {
        // Re-throw the original error (preserving ApiError status/code for UI)
        throw err;
      }
    },

    // ─── Teams ──────────────────────────────────────────────
    rescueTeams: [],
    fetchTeams: async () => {
      set(s => ({ loading: { ...s.loading, teams: true }, errors: { ...s.errors, teams: null } }));
      try {
        const teams = await apiFetch<RescueTeam[]>('/rescue-teams');
        if (Array.isArray(teams)) {
          set({ rescueTeams: teams });
        }
      } catch (err) {
        console.warn('Backend rescue teams API error:', err);
        set(s => ({ errors: { ...s.errors, teams: err instanceof Error ? err.message : 'Unable to load rescue teams.' } }));
      } finally {
        set(s => ({ loading: { ...s.loading, teams: false } }));
        get().refreshStats();
      }
    },
    updateTeam: (id, updates) =>
      set((state) => ({
        rescueTeams: state.rescueTeams.map((t) =>
          t.id === id ? { ...t, ...updates, lastUpdated: new Date().toISOString() } : t
        ),
      })),

    // ─── Hospitals ──────────────────────────────────────────
    hospitals: [],
    fetchHospitals: async () => {
      set(s => ({ loading: { ...s.loading, hospitals: true }, errors: { ...s.errors, hospitals: null } }));
      try {
        const hospitals = await apiFetch<Hospital[]>('/hospitals');
        if (Array.isArray(hospitals)) {
          set({ hospitals });
        }
      } catch (err) {
        console.warn('Backend hospitals API error:', err);
        set(s => ({ errors: { ...s.errors, hospitals: err instanceof Error ? err.message : 'Unable to load hospitals.' } }));
      } finally {
        set(s => ({ loading: { ...s.loading, hospitals: false } }));
      }
    },
    updateHospital: (id, updates) =>
      set((state) => ({
        hospitals: state.hospitals.map((h) =>
          h.id === id ? { ...h, ...updates, lastUpdated: new Date().toISOString() } : h
        ),
      })),

    // ─── Resources ──────────────────────────────────────────
    resources: [],
    fetchResources: async () => {
      set(s => ({ loading: { ...s.loading, resources: true }, errors: { ...s.errors, resources: null } }));
      try {
        const resources = await apiFetch<Resource[]>('/resources');
        if (Array.isArray(resources)) {
          set({ resources });
        }
      } catch (err) {
        console.warn('Backend resources API error:', err);
        set(s => ({ errors: { ...s.errors, resources: err instanceof Error ? err.message : 'Unable to load resources.' } }));
      } finally {
        set(s => ({ loading: { ...s.loading, resources: false } }));
      }
    },
    updateResource: (id, updates) =>
      set((state) => ({
        resources: state.resources.map((r) =>
          r.id === id ? { ...r, ...updates } : r
        ),
      })),

    // ─── Risk Predictions ────────────────────────────────────
    riskPredictions: [],
    fetchPredictions: async () => {
      set(s => ({ loading: { ...s.loading, predictions: true } }));
      try {
        const predictions = await apiFetch<RiskPrediction[]>('/predictions');
        if (Array.isArray(predictions)) {
          set({ riskPredictions: predictions });
        }
      } catch (err) {
        console.warn('Backend predictions API error:', err);
      } finally {
        set(s => ({ loading: { ...s.loading, predictions: false } }));
        get().refreshStats();
      }
    },
    updateRiskPrediction: (id, updates) =>
      set((state) => ({
        riskPredictions: state.riskPredictions.map((rp) =>
          rp.id === id ? { ...rp, ...updates, updatedAt: new Date().toISOString() } : rp
        ),
      })),

    // ─── Notifications ───────────────────────────────────────
    notifications: [],
    fetchNotifications: async () => {
      set(s => ({ loading: { ...s.loading, notifications: true } }));
      try {
        const notifications = await apiFetch<Notification[]>('/notifications');
        if (Array.isArray(notifications)) {
          set({
            notifications,
            unreadCount: notifications.filter((n) => !n.read).length,
          });
        }
      } catch (err) {
        console.warn('Backend notifications API error:', err);
      } finally {
        set(s => ({ loading: { ...s.loading, notifications: false } }));
      }
    },
    addNotification: (notif) =>
      set((state) => ({
        notifications: [notif, ...state.notifications],
        unreadCount: state.unreadCount + 1,
      })),
    markNotificationRead: (id) =>
      set((state) => ({
        notifications: state.notifications.map((n) =>
          n.id === id ? { ...n, read: true } : n
        ),
        unreadCount: Math.max(0, state.unreadCount - 1),
      })),
    markAllRead: () =>
      set((state) => ({
        notifications: state.notifications.map((n) => ({ ...n, read: true })),
        unreadCount: 0,
      })),
    unreadCount: 0,
    alerts: [],
    fetchAlerts: async () => {
      set(s => ({ loading: { ...s.loading, alerts: true }, errors: { ...s.errors, alerts: null } }));
      try {
        const alerts = await apiFetch<OperationalAlert[]>('/alerts/live');
        if (Array.isArray(alerts)) set({ alerts });
      } catch (err) {
        console.warn('Backend alerts API error:', err);
        set(s => ({ errors: { ...s.errors, alerts: err instanceof Error ? err.message : 'Unable to load alerts.' } }));
      } finally {
        set(s => ({ loading: { ...s.loading, alerts: false } }));
      }
    },

    // ─── Fetch All Data (called on app mount with request deduplication) ─────────────────
    fetchAllData: async () => {
      if (fetchAllDataPromise) {
        return fetchAllDataPromise;
      }

      fetchAllDataPromise = (async () => {
        const state = get();
        const userRole = state.currentUser?.role;

        if (userRole === 'citizen') {
          await Promise.allSettled([
            state.fetchIncidents(),
            state.fetchNotifications(),
            state.fetchAlerts(),
          ]);
        } else {
          await Promise.allSettled([
            state.fetchIncidents(),
            state.fetchTeams(),
            state.fetchHospitals(),
            state.fetchResources(),
            state.fetchPredictions(),
            state.fetchNotifications(),
            state.fetchAlerts(),
          ]);
        }

        get().refreshStats();
        // Clear promise cache after 4 seconds to allow fresh updates
        setTimeout(() => {
          fetchAllDataPromise = null;
        }, 4000);
      })();

      return fetchAllDataPromise;
    },

    // ─── Simulation (frontend-only) ──────────────────────────
    simulation: {
      isActive: false,
      currentStep: 0,
      elapsedTime: 0,
      startTime: undefined,
      scenario: 'flood_colombo',
      incidents: [],
      riskLevel: 0,
      riverLevel: 0,
      rainfall: 0,
    },
    startSimulation: () => {
      const state = get();
      const now = Date.now();
      set({ simulation: { ...state.simulation, isActive: true, currentStep: 0, startTime: now } });

      SIMULATION_STEPS_DATA.forEach((step) => {
        setTimeout(() => {
          const currentState = get();
          if (!currentState.simulation.isActive) return;
          step.action(currentState);
          set((s) => ({
            simulation: { ...s.simulation, currentStep: step.id },
          }));
          currentState.refreshStats();
        }, step.delay);
      });

      setTimeout(() => {
        set((s) => ({ simulation: { ...s.simulation, isActive: false } }));
      }, 55000);
    },
    stopSimulation: () =>
      set((state) => ({ simulation: { ...state.simulation, isActive: false } })),
    advanceSimulation: () => { },

    // ─── Stats ───────────────────────────────────────────────
    stats: {
      activeIncidents: 0,
      criticalIncidents: 0,
      peopleAffected: 0,
      teamsDeployed: 0,
      avgResponseTime: 0,
      aiPredictions: 0,
    },
    refreshStats: () => {
      const state = get();
      const activeInc = state.incidents.filter((i) => !['resolved', 'closed', 'cancelled'].includes(i.status?.toLowerCase()));
      const critInc = activeInc.filter((i) => i.severity?.toLowerCase() === 'critical');
      const people = activeInc.reduce((sum, i) => sum + (i.peopleAffected || 0), 0);
      const deployed = state.rescueTeams.filter((t) => t.status !== 'available' && t.status !== 'unavailable');
      const activeWithEta = activeInc.filter((i) => typeof i.eta === 'number' && i.eta > 0);
      const avgTime = activeWithEta.length > 0
        ? +(activeWithEta.reduce((sum, i) => sum + (i.eta || 0), 0) / activeWithEta.length).toFixed(1)
        : 0;

      set({
        stats: {
          activeIncidents: activeInc.length,
          criticalIncidents: critInc.length,
          peopleAffected: people > 0 ? people : state.incidents.reduce((sum, i) => sum + (i.peopleAffected || 0), 0),
          teamsDeployed: deployed.length,
          avgResponseTime: avgTime,
          aiPredictions: state.riskPredictions.length,
        },
      });
    },
  }))
);

// ─── Computed Selectors ──────────────────────────────────────

export const selectCriticalIncidents = (state: AppState) =>
  state.incidents.filter((i) => i.severity === 'critical' && i.status !== 'resolved');

export const selectActiveIncidents = (state: AppState) =>
  state.incidents.filter((i) => i.status !== 'resolved' && i.status !== 'closed');

export const selectAvailableTeams = (state: AppState) =>
  state.rescueTeams.filter((t) => t.status === 'available');

export const selectLowResources = (state: AppState) =>
  state.resources.filter((r) => r.status === 'low_stock' || r.status === 'critical_stock');
