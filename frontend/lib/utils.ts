import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { SeverityLevel, IncidentStatus, TeamStatus, EmergencyType } from '@/types';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// ─── Severity Utilities ──────────────────────────────────────

export function getSeverityColor(severity: SeverityLevel): string {
  const map: Record<SeverityLevel, string> = {
    critical: 'text-nova-critical',
    high: 'text-nova-high',
    medium: 'text-nova-medium',
    low: 'text-nova-low',
  };
  return map[severity] || 'text-nova-text-dim';
}

export function getSeverityBg(severity: SeverityLevel): string {
  const map: Record<SeverityLevel, string> = {
    critical: 'bg-red-500/10 border-red-500/30 text-red-400',
    high: 'bg-orange-500/10 border-orange-500/30 text-orange-400',
    medium: 'bg-yellow-500/10 border-yellow-500/30 text-yellow-400',
    low: 'bg-green-500/10 border-green-500/30 text-green-400',
  };
  return map[severity] || '';
}

export function getSeverityDot(severity: SeverityLevel): string {
  const map: Record<SeverityLevel, string> = {
    critical: 'bg-nova-critical shadow-[0_0_6px_rgba(255,59,59,0.8)]',
    high: 'bg-nova-high shadow-[0_0_6px_rgba(255,122,0,0.8)]',
    medium: 'bg-nova-medium shadow-[0_0_6px_rgba(255,215,0,0.8)]',
    low: 'bg-nova-low shadow-[0_0_6px_rgba(34,197,94,0.8)]',
  };
  return map[severity] || 'bg-gray-500';
}

import { localizeText, formatTimeAgo as i18nTimeAgo, getSeverityLabel as i18nSeverityLabel, getStatusLabel as i18nStatusLabel, getEmergencyTypeLabel as i18nEmergencyTypeLabel, getRiskLabel as i18nRiskLabel } from './i18n';

export function getSeverityLabel(severity: SeverityLevel, lang: string = 'en'): string {
  return i18nSeverityLabel(severity, lang);
}

// ─── Status Utilities ────────────────────────────────────────

export function getStatusLabel(status: IncidentStatus | TeamStatus | string, lang: string = 'en'): string {
  return i18nStatusLabel(status, lang);
}

export function getStatusColor(status: IncidentStatus): string {
  const colors: Record<IncidentStatus, string> = {
    submitted: 'text-nova-text-dim',
    analysing: 'text-nova-cyan',
    verified: 'text-nova-medium',
    dispatched: 'text-nova-blue-bright',
    acknowledged: 'text-nova-blue-bright',
    en_route: 'text-nova-high',
    on_scene: 'text-nova-critical',
    transporting: 'text-purple-400',
    resolved: 'text-nova-low',
    cancelled: 'text-nova-text-muted',
    reported: 'text-nova-text-dim',
    ai_analyzed: 'text-nova-cyan',
    prioritized: 'text-nova-medium',
    assigned: 'text-nova-blue-bright',
    responding: 'text-nova-critical',
    closed: 'text-nova-text-muted',
  };
  return colors[status] || 'text-nova-text-dim';
}

export function getTeamStatusColor(status: TeamStatus): string {
  const colors: Record<TeamStatus, string> = {
    available: 'text-nova-low',
    assigned: 'text-nova-blue-bright',
    en_route: 'text-nova-high',
    on_scene: 'text-nova-critical',
    unavailable: 'text-nova-text-muted',
  };
  return colors[status] || 'text-nova-text-dim';
}

// ─── Emergency Type Utilities ────────────────────────────────

export function getEmergencyTypeLabel(type: EmergencyType, lang: string = 'en'): string {
  return i18nEmergencyTypeLabel(type, lang);
}

export function getEmergencyTypeIcon(type: EmergencyType): string {
  const icons: Record<EmergencyType, string> = {
    flood: '🌊',
    landslide: '⛰️',
    fire: '🔥',
    road_accident: '🚗',
    medical: '🚑',
    crime: '🚨',
    missing_person: '👤',
    building_collapse: '🏚️',
    severe_weather: '🌪️',
    other: '⚠️',
    unknown: '⚠️',
  };
  return icons[type] || '⚠️';
}

// ─── Time Utilities ──────────────────────────────────────────

export function formatTimeAgo(dateStr: string, lang: string = 'en'): string {
  return i18nTimeAgo(dateStr, lang);
}

export function formatTime(dateStr: string): string {
  return new Date(dateStr).toLocaleTimeString('en-LK', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('en-LK', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// ─── Number Utilities ────────────────────────────────────────

export function formatNumber(n: number): string {
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`;
  return n.toString();
}

// ─── Risk Level ──────────────────────────────────────────────

export function getRiskColor(riskPercent: number): string {
  if (riskPercent >= 80) return 'text-nova-critical';
  if (riskPercent >= 60) return 'text-nova-high';
  if (riskPercent >= 40) return 'text-nova-medium';
  return 'text-nova-low';
}

export function getRiskBg(riskPercent: number): string {
  if (riskPercent >= 80) return 'bg-red-500';
  if (riskPercent >= 60) return 'bg-orange-500';
  if (riskPercent >= 40) return 'bg-yellow-500';
  return 'bg-green-500';
}

export function getRiskLabel(riskPercent: number, lang: string = 'en'): string {
  return i18nRiskLabel(riskPercent, lang);
}

// ─── Media URL Resolver ──────────────────────────────────────

export function resolveMediaUrl(url?: string): string {
  if (!url || typeof url !== 'string' || !url.trim() || url === 'null' || url === 'undefined' || url.endsWith('/null') || url.endsWith('/undefined')) {
    return '';
  }
  const trimmed = url.trim();
  if (trimmed.startsWith('data:') || trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed;
  }
  const apiBase = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
  const backendHost = apiBase.replace(/\/api\/?$/, '');
  return `${backendHost}${trimmed.startsWith('/') ? '' : '/'}${trimmed}`;
}
