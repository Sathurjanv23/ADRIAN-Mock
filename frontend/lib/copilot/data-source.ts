import type { Incident, RescueTeam, Hospital, Resource, RiskPrediction, Notification } from '@/types';

const BACKEND_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

async function fetchBackendData<T>(endpoint: string): Promise<T[]> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000);

    const res = await fetch(`${BACKEND_URL}${endpoint}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      signal: controller.signal,
      cache: 'no-store',
    });

    clearTimeout(timeoutId);

    if (!res.ok) {
      return [];
    }

    const json = await res.json();
    const data = json.data ?? json;
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

export async function getLiveOperationalData() {
  const [incidents, rescueTeams, hospitals, resources, riskPredictions, alerts] = await Promise.all([
    fetchBackendData<Incident>('/incidents'),
    fetchBackendData<RescueTeam>('/rescue-teams'),
    fetchBackendData<Hospital>('/hospitals'),
    fetchBackendData<Resource>('/resources'),
    fetchBackendData<RiskPrediction>('/predictions'),
    fetchBackendData<Notification>('/notifications'),
  ]);

  return {
    incidents,
    rescueTeams,
    hospitals,
    resources,
    riskPredictions,
    alerts,
  };
}
