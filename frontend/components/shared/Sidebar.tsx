'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';
import {
  LayoutDashboard, Map, AlertTriangle, Brain, BarChart3, Users,
  Truck, Building2, Cpu, Bell, FileText, Activity, Shield,
  Settings, ClipboardList, HeartPulse, Package, Radio, UserCheck,
  Navigation, Waves, ChevronLeft, ChevronRight, UtensilsCrossed
} from 'lucide-react';
import { useState, useMemo } from 'react';
import type { UserRole } from '@/types';
import { useTranslation } from '@/lib/i18n';
import { useNovaStore } from '@/lib/store/nova-store';

interface NavItem {
  href: string;
  label: string;
  icon: React.ReactNode;
  badge?: string | number;
  badgeVariant?: 'critical' | 'cyan' | 'warning';
}

interface NavSection {
  title?: string;
  items: NavItem[];
}

interface BadgeCounts {
  officerIncidents: number;
  officerAlerts: number;
  rescueIncidents: number;
  hospitalEmergencies: number;
}

function getNavSections(role: UserRole, badges: BadgeCounts): NavSection[] {
  switch (role) {
    case 'officer':
      return [
        {
          title: 'Command Center',
          items: [
            { href: '/command', label: 'Dashboard', icon: <LayoutDashboard className="w-4 h-4" /> },
            { href: '/command/map', label: 'Live Map', icon: <Map className="w-4 h-4" /> },
            {
              href: '/command/incidents',
              label: 'Incidents',
              icon: <AlertTriangle className="w-4 h-4" />,
              badge: badges.officerIncidents > 0 ? badges.officerIncidents : undefined,
              badgeVariant: 'critical',
            },
            { href: '/command/ai-analysis', label: 'AI Analysis', icon: <Brain className="w-4 h-4" /> },
          ],
        },
        {
          title: 'Operations',
          items: [
            { href: '/command/prediction', label: 'Prediction', icon: <Waves className="w-4 h-4" /> },
            { href: '/command/rescue', label: 'Rescue Ops', icon: <Truck className="w-4 h-4" /> },
            { href: '/command/resources', label: 'Resources', icon: <Package className="w-4 h-4" /> },
            { href: '/command/hospitals', label: 'Hospitals', icon: <HeartPulse className="w-4 h-4" /> },
          ],
        },
        {
          title: 'Intelligence',
          items: [
            { href: '/command/copilot', label: 'NOVA Copilot', icon: <Cpu className="w-4 h-4" /> },
            {
              href: '/command/alerts',
              label: 'Alerts',
              icon: <Bell className="w-4 h-4" />,
              badge: badges.officerAlerts > 0 ? badges.officerAlerts : undefined,
              badgeVariant: 'critical',
            },
            { href: '/command/analytics', label: 'Analytics', icon: <BarChart3 className="w-4 h-4" /> },
          ],
        },
        {
          title: '🟠 Relief Logistics',
          items: [
            { href: '/relief', label: 'Relief Dashboard', icon: <UtensilsCrossed className="w-4 h-4" /> },
            { href: '/relief/food-sources', label: 'Food Sources', icon: <Package className="w-4 h-4" /> },
            { href: '/relief/missions', label: 'Missions', icon: <Truck className="w-4 h-4" /> },
          ],
        },
      ];

    case 'citizen':
      return [
        {
          items: [
            { href: '/citizen', label: 'Dashboard', icon: <LayoutDashboard className="w-4 h-4" /> },
            { href: '/citizen/report', label: 'Report Emergency', icon: <AlertTriangle className="w-4 h-4" /> },
            { href: '/citizen/sos', label: 'SOS', icon: <Radio className="w-4 h-4" /> },
            { href: '/citizen/reports', label: 'My Reports', icon: <ClipboardList className="w-4 h-4" /> },
            { href: '/citizen/safety', label: 'Safety Guide', icon: <Shield className="w-4 h-4" /> },
          ],
        },
      ];

    case 'rescue_team':
      return [
        {
          items: [
            { href: '/rescue', label: 'Dashboard', icon: <LayoutDashboard className="w-4 h-4" /> },
            {
              href: '/rescue/incidents',
              label: 'Assigned Incidents',
              icon: <AlertTriangle className="w-4 h-4" />,
              badge: badges.rescueIncidents > 0 ? badges.rescueIncidents : undefined,
              badgeVariant: 'critical',
            },
            { href: '/rescue/navigation', label: 'Navigation', icon: <Navigation className="w-4 h-4" /> },
            { href: '/rescue/team', label: 'Team Status', icon: <Users className="w-4 h-4" /> },
            { href: '/relief/missions', label: 'Relief Missions', icon: <Truck className="w-4 h-4" /> },
          ],
        },
      ];

    case 'hospital':
      return [
        {
          items: [
            { href: '/hospital', label: 'Dashboard', icon: <LayoutDashboard className="w-4 h-4" /> },
            {
              href: '/hospital/emergencies',
              label: 'Incoming',
              icon: <AlertTriangle className="w-4 h-4" />,
              badge: badges.hospitalEmergencies > 0 ? badges.hospitalEmergencies : undefined,
              badgeVariant: 'critical',
            },
            { href: '/hospital/triage', label: 'Triage Queue', icon: <ClipboardList className="w-4 h-4" /> },
            { href: '/hospital/capacity', label: 'Capacity', icon: <Activity className="w-4 h-4" /> },
            { href: '/hospital/ambulances', label: 'Ambulances', icon: <Truck className="w-4 h-4" /> },
          ],
        },
      ];

    case 'admin':
      return [
        {
          title: 'System',
          items: [
            { href: '/admin', label: 'Dashboard', icon: <LayoutDashboard className="w-4 h-4" /> },
            { href: '/admin/users', label: 'Users', icon: <Users className="w-4 h-4" /> },
            { href: '/admin/organizations', label: 'Organizations', icon: <Building2 className="w-4 h-4" /> },
          ],
        },
        {
          title: 'Configuration',
          items: [
            { href: '/admin/resources', label: 'Resources', icon: <Package className="w-4 h-4" /> },
            { href: '/admin/ai-config', label: 'AI Config', icon: <Cpu className="w-4 h-4" /> },
            { href: '/admin/monitoring', label: 'Monitoring', icon: <Activity className="w-4 h-4" /> },
            { href: '/admin/audit-logs', label: 'Audit Logs', icon: <FileText className="w-4 h-4" /> },
          ],
        },
      ];

    default:
      return [];
  }
}

// ─── Sidebar Component ───────────────────────────────────────

interface SidebarProps {
  role: UserRole;
  collapsed?: boolean;
  onToggle?: () => void;
}

export function Sidebar({ role, collapsed = false, onToggle }: SidebarProps) {
  const pathname = usePathname();
  const { t } = useTranslation();
  const { incidents, notifications, unreadCount, rescueTeams, currentUser, isAuthenticated } = useNovaStore();
  const isCitizenGuest = role === 'citizen' && (!isAuthenticated || currentUser?.role !== 'citizen');

  const badges: BadgeCounts = useMemo(() => {
    const officerIncidents = incidents.filter(
      (i) => i.status !== 'resolved' && i.status !== 'closed' && i.status !== 'cancelled'
    ).length;
    const officerAlerts = unreadCount || notifications.filter((n) => !n.read).length;

    const myTeam = rescueTeams.find((t) => t.id === 'RT-ALPHA-02') || rescueTeams[0];
    const rescueIncidents = incidents.filter((i) => {
      if (i.status === 'resolved' || i.status === 'closed' || i.status === 'cancelled') return false;
      const isAssignedToMe = i.assignedTeamId === myTeam?.id || i.assignedTeam === myTeam?.id;
      const isAvailableForRescue =
        !i.assignedTeamId &&
        (i.recommendedAgencies?.includes('search_rescue') ||
          i.recommendedAgencies?.includes('fire_rescue') ||
          i.recommendedAgencies?.includes('disaster_response') ||
          ['flood', 'landslide', 'fire', 'building_collapse', 'road_accident', 'unknown'].includes(i.type));
      return isAssignedToMe || isAvailableForRescue;
    }).length;

    const hospitalEmergencies = incidents.filter(
      (i) =>
        (i.type === 'medical' || i.type === 'road_accident' || i.type === 'building_collapse' || i.type === 'fire') &&
        i.status !== 'resolved' &&
        i.status !== 'closed' &&
        i.status !== 'cancelled'
    ).length;

    return { officerIncidents, officerAlerts, rescueIncidents, hospitalEmergencies };
  }, [incidents, notifications, unreadCount, rescueTeams]);

  const rawSections = getNavSections(role, badges);

  // If guest viewing citizen routes, annotate locked vs open items
  if (isCitizenGuest && rawSections[0]?.items) {
    rawSections[0].items = rawSections[0].items.map((it) => {
      if (it.href === '/citizen' || it.href === '/citizen/reports') {
        return { ...it, badge: '🔒 Sign In', badgeVariant: 'warning' as const };
      }
      if (it.href === '/citizen/sos') {
        return { ...it, badge: 'OPEN', badgeVariant: 'critical' as const };
      }
      return it;
    });
  }

  const getTranslationKey = (label: string) => {
    const map: Record<string, string> = {
      'Dashboard': 'nav.dashboard',
      'Live Map': 'nav.map',
      'Incidents': 'nav.incidents',
      'Assigned Incidents': 'nav.incidents',
      'AI Analysis': 'nav.ai_analysis',
      'Prediction': 'nav.prediction',
      'Rescue Ops': 'nav.rescue_ops',
      'Resources': 'nav.resources',
      'Hospitals': 'nav.hospitals',
      'NOVA Copilot': 'nav.copilot',
      'Alerts': 'nav.alerts',
      'Analytics': 'nav.analytics',
      'SOS': 'nav.sos',
      'Report Emergency': 'btn.report',
      'My Reports': 'nav.reports',
      'Safety Guide': 'nav.safety',
      'Team Status': 'nav.team_status',
      'Navigation': 'nav.navigation',
      'Triage Queue': 'nav.triage',
      'Incoming': 'nav.incoming',
      'Capacity': 'nav.capacity',
      'Ambulances': 'nav.ambulances',
      'Users': 'nav.users',
      'Organizations': 'nav.organizations',
      'Audit Logs': 'nav.audit_logs',
      'AI Config': 'nav.ai_config',
      'Monitoring': 'nav.monitoring',
    };
    return map[label] || label;
  };

  const getSectionTitleKey = (title?: string) => {
    if (!title) return '';
    const map: Record<string, string> = {
      'Command Center': 'section.command_center',
      'Operations': 'section.operations',
      'Intelligence': 'section.intelligence',
      'System': 'section.system',
      'Configuration': 'section.configuration',
    };
    return map[title] || title;
  };

  const sections = rawSections.map((section) => ({
    ...section,
    title: section.title ? t(getSectionTitleKey(section.title)) : undefined,
    items: section.items.map((item) => ({
      ...item,
      label: t(getTranslationKey(item.label)),
    })),
  }));

  const roleColors: Record<UserRole, string> = {
    citizen: 'text-nova-cyan border-nova-cyan/30 bg-nova-cyan/10',
    officer: 'text-purple-400 border-purple-400/30 bg-purple-400/10',
    rescue_team: 'text-orange-400 border-orange-400/30 bg-orange-400/10',
    hospital: 'text-pink-400 border-pink-400/30 bg-pink-400/10',
    admin: 'text-red-400 border-red-400/30 bg-red-400/10',
  };

  const roleLabels: Record<UserRole, string> = {
    citizen: 'CITIZEN',
    officer: 'OFFICER',
    rescue_team: 'RESCUE',
    hospital: 'HOSPITAL',
    admin: 'ADMIN',
  };

  const userOriginalRole = currentUser?.role;
  const isVisitingCommand = role === 'officer' && Boolean(userOriginalRole && ['hospital', 'rescue_team'].includes(userOriginalRole));

  return (
    <motion.aside
      className={cn(
        'flex flex-col bg-nova-surface border-r border-nova-border h-full transition-all duration-300',
        collapsed ? 'w-16' : 'w-56'
      )}
    >
      {/* Role Badge */}
      {!collapsed && (
        <div className="px-4 py-3 border-b border-nova-border flex items-center justify-between">
          <span className={cn('text-[10px] font-bold px-2 py-1 rounded border', isCitizenGuest ? 'text-orange-400 border-orange-500/30 bg-orange-500/10' : roleColors[role])}>
            {isCitizenGuest ? 'GUEST REPORTING' : `${roleLabels[role]} PORTAL`}
          </span>
          {isCitizenGuest && (
            <Link href="/login?portal=citizen" className="text-[10px] text-nova-cyan hover:underline font-bold">
              Sign In →
            </Link>
          )}
        </div>
      )}

      {/* Return to original portal banner */}
      {!collapsed && isVisitingCommand && (
        <div className="px-3 py-2 bg-nova-cyan/10 border-b border-nova-cyan/25 flex items-center justify-between">
          <Link
            href={userOriginalRole === 'hospital' ? '/hospital' : '/rescue'}
            className="flex items-center gap-1 text-[11px] font-semibold text-nova-cyan hover:underline"
          >
            <ChevronLeft className="w-3.5 h-3.5" />
            <span>Back to {userOriginalRole === 'hospital' ? 'Hospital' : 'Rescue'}</span>
          </Link>
        </div>
      )}

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-3 px-2">
        {sections.map((section, sIdx) => (
          <div key={sIdx} className="mb-4">
            {section.title && !collapsed && (
              <p className="text-[10px] font-bold text-nova-text-muted uppercase tracking-widest px-2 mb-1.5">
                {section.title}
              </p>
            )}
            <div className="space-y-0.5">
              {section.items.map((item) => {
                const isRootPath = ['/command', '/citizen', '/rescue', '/hospital', '/admin'].includes(item.href);
                const isActive = pathname === item.href || (!isRootPath && pathname?.startsWith(`${item.href}/`));

                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      'flex items-center gap-2.5 px-2.5 py-2 rounded-lg text-sm transition-all duration-150 group relative',
                      isActive
                        ? 'bg-nova-cyan/10 text-nova-cyan border border-nova-cyan/20'
                        : 'text-nova-text-dim hover:bg-nova-surface2 hover:text-nova-text border border-transparent'
                    )}
                  >
                    <span className={cn(isActive ? 'text-nova-cyan' : 'text-nova-text-muted group-hover:text-nova-text')}>
                      {item.icon}
                    </span>
                    {!collapsed && (
                      <>
                        <span className="flex-1 font-medium text-xs">{item.label}</span>
                        {item.badge && (
                          <span className={cn(
                            'text-[9px] font-bold px-1.5 py-0.5 rounded-full',
                            item.badgeVariant === 'critical' ? 'bg-red-500 text-white' :
                            item.badgeVariant === 'warning' ? 'bg-orange-500 text-white' :
                            'bg-nova-cyan text-nova-bg'
                          )}>
                            {item.badge}
                          </span>
                        )}
                      </>
                    )}
                    {isActive && (
                      <motion.div
                        className="absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-5 bg-nova-cyan rounded-full"
                        layoutId={`active-indicator-${role}`}
                      />
                    )}
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      {/* Bottom toggle */}
      <div className="border-t border-nova-border p-2">
        <button
          onClick={onToggle}
          className="w-full flex items-center justify-center h-8 rounded-lg hover:bg-nova-surface2 text-nova-text-muted hover:text-nova-text transition-colors"
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>
      </div>
    </motion.aside>
  );
}

// ─── Dashboard Shell ─────────────────────────────────────────

interface DashboardShellProps {
  children: React.ReactNode;
  role: UserRole;
}

export function DashboardShell({ children, role }: DashboardShellProps) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  return (
    <div className="flex h-[calc(100vh-116px)] overflow-hidden">
      <Sidebar role={role} collapsed={sidebarCollapsed} onToggle={() => setSidebarCollapsed(!sidebarCollapsed)} />
      <main className="flex-1 overflow-y-auto">
        {children}
      </main>
    </div>
  );
}
