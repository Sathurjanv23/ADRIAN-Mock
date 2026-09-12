// ============================================================
// PROJECT NOVA — API Service Layer
// Spring Boot REST API integration layer (mock-first)
// ============================================================

// ─── Backend Configuration ────────────────────────────────────
// Spring Boot backend running on http://localhost:8080/api
const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
const AI_BASE = process.env.NEXT_PUBLIC_AI_URL || 'http://localhost:8001';

// ─── Generic Fetch Wrapper ───────────────────────────────────

async function apiFetch<T>(
  endpoint: string,
  options?: RequestInit,
  base: string = API_BASE
): Promise<T> {
  const url = `${base}${endpoint}`;
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getAuthToken()}`,
      ...options?.headers,
    },
    ...options,
  });

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Unknown error' }));
    throw new Error(error.message || `API error: ${res.status}`);
  }

  return res.json();
}

function getAuthToken(): string {
  if (typeof window === 'undefined') return '';
  return localStorage.getItem('nova_token') || '';
}

// ─── Auth API ────────────────────────────────────────────────

export const authApi = {
  login: (credentials: { email: string; password: string }) =>
    apiFetch('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),

  register: (data: Record<string, unknown>) =>
    apiFetch('/auth/register', { method: 'POST', body: JSON.stringify(data) }),

  logout: () =>
    apiFetch('/auth/logout', { method: 'POST' }),

  refreshToken: (refreshToken: string) =>
    apiFetch('/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) }),
};

// ─── Incidents API ───────────────────────────────────────────

export const incidentsApi = {
  getAll: (params?: { severity?: string; status?: string; page?: number }) => {
    const query = params ? `?${new URLSearchParams(params as Record<string, string>)}` : '';
    return apiFetch(`/incidents${query}`);
  },

  getById: (id: string) => apiFetch(`/incidents/${id}`),

  getByTrackingCode: (code: string) => apiFetch(`/incidents/tracking/${encodeURIComponent(code)}`),

  create: (data: Record<string, unknown>) =>
    apiFetch('/incidents', { method: 'POST', body: JSON.stringify(data) }),

  reportEmergency: async (formData: FormData) => {
    const token = getAuthToken();
    const url = `${API_BASE}/incidents/report`;
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: formData,
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({ message: 'Failed to submit emergency report.' }));
      throw new Error(error.message || `HTTP ${res.status}`);
    }

    const json = await res.json();
    return json.data ?? json;
  },

  update: (id: string, data: Record<string, unknown>) =>
    apiFetch(`/incidents/${id}`, { method: 'PATCH', body: JSON.stringify(data) }),

  assignTeam: (incidentId: string, teamId: string, teamName?: string) =>
    apiFetch(`/incidents/${incidentId}/assign`, { method: 'POST', body: JSON.stringify({ teamId, teamName }) }),

  acceptMission: (incidentId: string, teamId: string, teamName?: string, officerName?: string) =>
    apiFetch(`/incidents/${incidentId}/accept`, { method: 'POST', body: JSON.stringify({ teamId, teamName, officerName }) }),

  updateMissionStatus: (incidentId: string, status: string, teamId?: string, notes?: string) =>
    apiFetch(`/incidents/${incidentId}/status`, { method: 'POST', body: JSON.stringify({ status, teamId, notes }) }),

  acknowledge: (id: string, agency?: string, officerName?: string) =>
    apiFetch(`/incidents/${id}/acknowledge`, { method: 'POST', body: JSON.stringify({ agency, officerName }) }),

  cancel: (id: string, reason?: string) =>
    apiFetch(`/incidents/${id}/cancel`, { method: 'POST', body: JSON.stringify({ reason }) }),

  escalate: (id: string, reason: string) =>
    apiFetch(`/incidents/${id}/escalate`, { method: 'POST', body: JSON.stringify({ reason }) }),

  resolve: (id: string, resolution: string) =>
    apiFetch(`/incidents/${id}/resolve`, { method: 'POST', body: JSON.stringify({ resolution }) }),
};

// ─── AI API (FastAPI) ────────────────────────────────────────

export const aiApi = {
  analyzeText: (text: string, language?: string) =>
    apiFetch('/ai/analyze/text', { method: 'POST', body: JSON.stringify({ text, language }) }, AI_BASE),

  analyzeVoice: (audioBlob: Blob) => {
    const formData = new FormData();
    formData.append('audio', audioBlob, 'recording.webm');
    return apiFetch('/ai/analyze/voice', {
      method: 'POST',
      body: formData,
      headers: { Authorization: `Bearer ${getAuthToken()}` },
    }, AI_BASE);
  },

  analyzeImage: (imageBlob: Blob) => {
    const formData = new FormData();
    formData.append('image', imageBlob, 'image.jpg');
    return apiFetch('/ai/analyze/image', {
      method: 'POST',
      body: formData,
      headers: { Authorization: `Bearer ${getAuthToken()}` },
    }, AI_BASE);
  },

  copilotChat: (message: string, context?: Record<string, unknown>) =>
    apiFetch('/ai/copilot', { method: 'POST', body: JSON.stringify({ message, context }) }, AI_BASE),

  generateAfterActionReport: (params: { startDate: string; endDate: string; district?: string }) =>
    apiFetch('/ai/after-action-report', { method: 'POST', body: JSON.stringify(params) }, AI_BASE),

  analyzeIncident: (data: {
    incidentId?: string;
    type: string;
    description?: string;
    peopleAffected?: number;
    lat?: number;
    lng?: number;
    hasPhoto?: boolean;
    hasAudio?: boolean;
    detectedLanguage?: string;
  }) =>
    apiFetch('/ai/analyze-incident', { method: 'POST', body: JSON.stringify(data) }),

  copilot: (query: string, context = '{}') =>
    apiFetch('/ai/copilot', { method: 'POST', body: JSON.stringify({ query, context }) }),

  reliefRecommendation: (reliefRequestId: string) =>
    apiFetch('/ai/relief-recommendation', {
      method: 'POST',
      body: JSON.stringify({ reliefRequestId }),
    }),

  getStatus: () =>
    apiFetch('/ai/status'),
};

// ─── Predictions API ──────────────────────────────────────────

export const predictionsApi = {
  getAll: () => apiFetch('/predictions'),
  getByZone: (zone: string) => apiFetch(`/predictions/zone/${zone}`),
  getDigitalTwin: (hoursAhead: number) => apiFetch(`/predictions/digital-twin?hours=${hoursAhead}`),
};

// ─── Rescue Teams API ─────────────────────────────────────────

export const rescueTeamsApi = {
  getAll: () => apiFetch('/rescue-teams'),
  getById: (id: string) => apiFetch(`/rescue-teams/${id}`),
  updateStatus: (id: string, status: string) =>
    apiFetch(`/rescue-teams/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  assign: (teamId: string, incidentId: string) =>
    apiFetch('/rescue-teams/assign', { method: 'POST', body: JSON.stringify({ teamId, incidentId }) }),
};

// ─── Hospitals API ────────────────────────────────────────────

export const hospitalsApi = {
  getAll: () => apiFetch('/hospitals'),
  getById: (id: string) => apiFetch(`/hospitals/${id}`),
  updateCapacity: (id: string, data: Record<string, unknown>) =>
    apiFetch(`/hospitals/${id}/capacity`, { method: 'PATCH', body: JSON.stringify(data) }),
  dispatchAmbulance: (hospitalId: string, incidentId: string) =>
    apiFetch(`/hospitals/${hospitalId}/ambulances/dispatch`, {
      method: 'POST', body: JSON.stringify({ incidentId })
    }),
};

// ─── Resources API ────────────────────────────────────────────

export const resourcesApi = {
  getAll: () => apiFetch('/resources'),
  allocate: (resourceId: string, quantity: number, incidentId: string) =>
    apiFetch('/resources/allocate', {
      method: 'POST', body: JSON.stringify({ resourceId, quantity, incidentId })
    }),
};

// ─── Map API ──────────────────────────────────────────────────

export const mapApi = {
  getIncidents: () => apiFetch('/map/incidents'),
  getTeamLocations: () => apiFetch('/map/teams'),
  getHospitals: () => apiFetch('/map/hospitals'),
  getRiskZones: () => apiFetch('/map/risk-zones'),
};

// ─── Analytics API ────────────────────────────────────────────

export const analyticsApi = {
  getSummary: (period: string) => apiFetch(`/analytics?period=${period}`),
  getIncidentTrends: () => apiFetch('/analytics/incidents/trends'),
  getResponseTimes: () => apiFetch('/analytics/response-times'),
  getResourceUsage: () => apiFetch('/analytics/resources'),
};

// ─── Admin API ────────────────────────────────────────────────

export const adminApi = {
  getUsers: () => apiFetch('/admin/users'),
  createUser: (data: Record<string, unknown>) =>
    apiFetch('/admin/users', { method: 'POST', body: JSON.stringify(data) }),
  updateUser: (id: string, data: Record<string, unknown>) =>
    apiFetch(`/admin/users/${id}`, { method: 'PATCH', body: JSON.stringify(data) }),
  approveUser: (id: string, decision: 'APPROVE' | 'REJECT', rescueTeamId?: string) =>
    apiFetch(`/admin/users/${id}/approval`, {
      method: 'POST', body: JSON.stringify({ decision, rescueTeamId }),
    }),
  deleteUser: (id: string) =>
    apiFetch(`/admin/users/${id}`, { method: 'DELETE' }),
  getAuditLogs: () => apiFetch('/admin/audit-logs'),
  getSystemHealth: () => apiFetch('/admin/system/health'),
};

// ─── ADRN Food Sources API ────────────────────────────────────

export const foodSourcesApi = {
  getAll: (status?: string) =>
    apiFetch(`/food-sources${status ? `?status=${status}` : ''}`),

  getNearby: (lat: number, lng: number, radiusKm = 50, minMeals = 0) =>
    apiFetch(`/food-sources/nearby?lat=${lat}&lng=${lng}&radiusKm=${radiusKm}&minMeals=${minMeals}`),

  getById: (id: string) =>
    apiFetch(`/food-sources/${id}`),

  create: (data: Record<string, unknown>) =>
    apiFetch('/food-sources', { method: 'POST', body: JSON.stringify(data) }),

  update: (id: string, data: Record<string, unknown>) =>
    apiFetch(`/food-sources/${id}`, { method: 'PUT', body: JSON.stringify(data) }),

  updateStatus: (id: string, status: string) =>
    apiFetch(`/food-sources/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
};

// ─── ADRN Relief Requests API ─────────────────────────────────

export const reliefRequestsApi = {
  getAll: (status?: string) =>
    apiFetch(`/relief-requests${status ? `?status=${status}` : ''}`),

  getById: (id: string) =>
    apiFetch(`/relief-requests/${id}`),

  create: (data: Record<string, unknown>) =>
    apiFetch('/relief-requests', { method: 'POST', body: JSON.stringify(data) }),

  createFromIncident: (incidentId: string) =>
    apiFetch(`/relief-requests/from-incident/${incidentId}`, { method: 'POST' }),

  updateStatus: (id: string, status: string) =>
    apiFetch(`/relief-requests/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),

  getStats: () =>
    apiFetch('/relief-requests/stats'),
};

// ─── ADRN Relief Missions API ─────────────────────────────────

export const reliefMissionsApi = {
  getAll: (status?: string) =>
    apiFetch(`/relief-missions${status ? `?status=${status}` : ''}`),

  getActive: () =>
    apiFetch('/relief-missions/active'),

  getById: (id: string) =>
    apiFetch(`/relief-missions/${id}`),

  create: (data: {
    reliefRequestId: string;
    foodSourceId: string;
    vehicleId?: string;
    vehicleName?: string;
    driverName?: string;
    driverContact?: string;
  }) =>
    apiFetch('/relief-missions', { method: 'POST', body: JSON.stringify(data) }),

  updateStatus: (id: string, status: string, notes?: string) =>
    apiFetch(`/relief-missions/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status, notes }),
    }),

  updateLocation: (id: string, lat: number, lng: number) =>
    apiFetch(`/relief-missions/${id}/location`, {
      method: 'PUT',
      body: JSON.stringify({ lat, lng }),
    }),
};


