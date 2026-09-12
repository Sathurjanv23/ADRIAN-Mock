'use client';

import { motion, AnimatePresence } from 'framer-motion';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useState, useEffect } from 'react';
import { useNovaStore } from '@/lib/store/nova-store';
import { signOut, getSession } from '@/lib/auth';
import { cn } from '@/lib/utils';
import { Bell, Search, ChevronDown, ChevronLeft, Zap, AlertTriangle, Play, Square, Globe, LogOut } from 'lucide-react';
import type { UserRole } from '@/types';
import { useTranslation } from '@/lib/i18n';

// ─── NOVA Logo ───────────────────────────────────────────────

export function NovaLogo({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) {
  const sizeMap = { sm: 'w-6 h-6', md: 'w-8 h-8', lg: 'w-10 h-10' };
  const textMap = { sm: 'text-sm', md: 'text-base', lg: 'text-xl' };

  return (
    <Link href="/" className="flex items-center gap-2.5 group">
      <div className={cn('relative flex-shrink-0', sizeMap[size])}>
        <svg viewBox="0 0 32 32" fill="none" className="w-full h-full">
          <polygon points="16,2 30,28 16,22 2,28" fill="none" stroke="#00d4ff" strokeWidth="1.5" />
          <polygon points="16,8 26,26 16,20 6,26" fill="rgba(0,212,255,0.08)" stroke="#2563eb" strokeWidth="1" />
          <circle cx="16" cy="14" r="2.5" fill="#00d4ff" style={{ filter: 'drop-shadow(0 0 4px #00d4ff)' }} />
        </svg>
      </div>
      <div>
        <div className={cn('font-bold font-display text-nova-text tracking-wider', textMap[size])}>
          PROJECT <span className="text-nova-cyan text-glow-cyan">NOVA</span>
        </div>
        {size !== 'sm' && (
          <div className="text-[9px] text-nova-text-muted tracking-widest uppercase">
            AI Emergency Response Network
          </div>
        )}
      </div>
    </Link>
  );
}

// ─── Alert Ticker ────────────────────────────────────────────

export function AlertTicker() {
  const { localize, t } = useTranslation();
  const { alerts, loading, errors } = useNovaStore();
  const activeAlerts = alerts.map((alert) => {
    let title = alert.title;
    if (!title || title.trim() === '' || title.toLowerCase() === 'notification') {
      if (alert.severity === 'critical') title = '🔴 CRITICAL EMERGENCY';
      else if (alert.severity === 'high') title = '🟠 HIGH PRIORITY';
      else title = '⚠️ EMERGENCY ALERT';
    }
    return `${title}: ${alert.message}`;
  });

  const displayList = activeAlerts.length > 0
    ? activeAlerts
    : errors.alerts
      ? [`Live alerts unavailable: ${errors.alerts}`]
      : loading.alerts
        ? ['Loading live alerts...']
        : ['All operational sectors normal — No active critical emergency alerts'];

  return (
    <div className={cn(
      "border-y py-1.5 overflow-hidden relative transition-colors",
      activeAlerts.length > 0
        ? "bg-red-950/50 border-red-500/20"
        : "bg-nova-surface border-nova-border"
    )}>
      <div className="flex items-center gap-3 px-4">
        <span className={cn(
          "text-[10px] font-bold uppercase tracking-widest flex-shrink-0 px-2 py-0.5 rounded",
          activeAlerts.length > 0 ? "bg-red-500/20 text-red-400" : "bg-nova-cyan/15 text-nova-cyan"
        )}>
          {activeAlerts.length > 0 ? t('common.live') : 'STATUS'}
        </span>
        <div className="ticker-wrapper flex-1">
          <div className="ticker-content">
            {[...displayList, ...displayList].map((msg, i) => (
              <span key={i} className={cn(
                "inline-flex items-center text-xs mr-16",
                activeAlerts.length > 0 ? "text-red-200/80" : "text-nova-text-dim"
              )}>
                {localize(msg)}
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Notification Bell ───────────────────────────────────────

function NotificationBell() {
  const [open, setOpen] = useState(false);
  const { notifications, unreadCount, markNotificationRead, markAllRead } = useNovaStore();
  const { t, localize, timeAgo } = useTranslation();
  const recent = notifications.slice(0, 8);

  return (
    <div className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="relative w-9 h-9 flex items-center justify-center rounded-lg bg-nova-surface hover:bg-nova-surface2 transition-colors border border-nova-border hover:border-nova-border2"
        aria-label="Notifications"
      >
        <Bell className="w-4 h-4 text-nova-text-dim" />
        {unreadCount > 0 && (
          <motion.span
            className="absolute -top-1 -right-1 min-w-4 h-4 px-1 bg-red-500 rounded-full text-[9px] font-bold text-white flex items-center justify-center shadow-sm"
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </motion.span>
        )}
      </button>

      <AnimatePresence>
        {open && (
          <>
            <div className="fixed inset-0 z-[99998]" onClick={() => setOpen(false)} />
            <motion.div
              className="absolute right-0 mt-2 w-[360px] max-w-[90vw] bg-[#0c1322] border border-nova-border rounded-xl shadow-2xl z-[99999] overflow-hidden"
              initial={{ opacity: 0, y: -8, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -8, scale: 0.97 }}
              transition={{ duration: 0.15 }}
            >
              {/* Header */}
              <div className="flex items-center justify-between px-4 py-3 bg-[#0f172a] border-b border-nova-border">
                <div className="flex items-center gap-2">
                  <Bell className="w-4 h-4 text-nova-cyan" />
                  <span className="text-xs font-bold text-nova-text tracking-wide uppercase">
                    {t('common.notifications')}
                  </span>
                  {unreadCount > 0 && (
                    <span className="text-[10px] font-bold bg-red-500/20 text-red-400 border border-red-500/30 px-1.5 py-0.2 rounded-full">
                      {unreadCount} new
                    </span>
                  )}
                </div>
                {unreadCount > 0 && (
                  <button
                    onClick={markAllRead}
                    className="text-[11px] text-nova-cyan hover:underline font-medium"
                  >
                    {t('common.mark_all_read')}
                  </button>
                )}
              </div>

              {/* Notification List */}
              <div className="max-h-80 overflow-y-auto divide-y divide-nova-border/30">
                {recent.length > 0 ? (
                  recent.map((notif) => (
                    <button
                      key={notif.id}
                      onClick={() => markNotificationRead(notif.id)}
                      className={cn(
                        'w-full text-left px-4 py-3 hover:bg-nova-surface2/60 transition-colors flex items-start gap-3',
                        !notif.read ? 'bg-nova-cyan/5' : 'bg-transparent'
                      )}
                    >
                      <div
                        className={cn(
                          'w-2.5 h-2.5 rounded-full mt-1 flex-shrink-0',
                          notif.severity === 'critical' ? 'bg-red-400 shadow-[0_0_6px_rgba(248,113,113,0.6)]' :
                          notif.severity === 'high' ? 'bg-orange-400 shadow-[0_0_6px_rgba(251,146,60,0.6)]' :
                          notif.severity === 'medium' ? 'bg-yellow-400' : 'bg-nova-cyan'
                        )}
                      />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-1">
                          <p className="text-xs font-semibold text-nova-text truncate">
                            {localize(notif.title)}
                          </p>
                          {!notif.read && (
                            <span className="w-1.5 h-1.5 rounded-full bg-nova-cyan flex-shrink-0" />
                          )}
                        </div>
                        <p className="text-[11px] text-nova-text-dim mt-0.5 line-clamp-2 leading-relaxed">
                          {localize(notif.message)}
                        </p>
                        {notif.createdAt && (
                          <p className="text-[9px] text-nova-text-muted mt-1 font-mono">
                            {timeAgo(notif.createdAt)}
                          </p>
                        )}
                      </div>
                    </button>
                  ))
                ) : (
                  <div className="py-8 text-center px-4">
                    <Bell className="w-8 h-8 text-nova-text-muted mx-auto opacity-40 mb-2" />
                    <p className="text-xs font-medium text-nova-text-dim">No notifications</p>
                    <p className="text-[10px] text-nova-text-muted mt-0.5">Emergency alerts will appear here</p>
                  </div>
                )}
              </div>

              {/* Footer */}
              <div className="px-4 py-2 bg-[#0f172a] border-t border-nova-border flex justify-between items-center text-xs">
                <Link
                  href="/command/alerts"
                  className="text-[11px] text-nova-cyan hover:underline font-medium"
                  onClick={() => setOpen(false)}
                >
                  {t('common.view_all_notifications')} →
                </Link>
                <button
                  onClick={() => setOpen(false)}
                  className="text-[11px] text-nova-text-muted hover:text-nova-text"
                >
                  Close
                </button>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Simulation Control Button ───────────────────────────────

export function SimulationControl() {
  const { simulation, startSimulation, stopSimulation } = useNovaStore();
  const { t } = useTranslation();

  return (
    <motion.button
      onClick={simulation.isActive ? stopSimulation : startSimulation}
      className={cn(
        'flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all',
        simulation.isActive
          ? 'bg-red-500/20 border-red-500/40 text-red-300 hover:bg-red-500/30'
          : 'bg-nova-cyan/10 border-nova-cyan/30 text-nova-cyan hover:bg-nova-cyan/20'
      )}
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
    >
      {simulation.isActive ? (
        <>
          <motion.div
            className="w-2 h-2 rounded-sm bg-red-400"
            animate={{ opacity: [1, 0.4, 1] }}
            transition={{ duration: 0.8, repeat: Infinity }}
          />
          {t('btn.stop_sim')}
        </>
      ) : (
        <>
          <Play className="w-3 h-3" />
          {t('btn.start_sim')}
        </>
      )}
    </motion.button>
  );
}

// ─── Role Switcher ───────────────────────────────────────────

const ROLE_LINKS: Record<UserRole, string> = {
  citizen: '/citizen',
  officer: '/command',
  rescue_team: '/rescue',
  hospital: '/hospital',
  admin: '/admin',
};

const ROLE_LABELS: Record<UserRole, string> = {
  citizen: 'Citizen',
  officer: 'Emergency Officer',
  rescue_team: 'Rescue Team',
  hospital: 'Hospital',
  admin: 'Administrator',
};

// ─── Main Top Navigation Bar ─────────────────────────────────

interface TopNavProps {
  role?: UserRole;
  showTicker?: boolean;
  title?: string;
  subtitle?: string;
}

export function TopNav({ role, showTicker = true, title, subtitle }: TopNavProps) {
  const pathname = usePathname();
  const router = useRouter();
  const { language, setLanguage, currentUser, isAuthenticated, logout } = useNovaStore();
  const [searchOpen, setSearchOpen] = useState(false);
  const [roleMenuOpen, setRoleMenuOpen] = useState(false);
  const { t } = useTranslation();

  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    setMounted(true);
  }, []);

  // Determine if the user is a hospital coordinator or rescue team member visiting command center
  const effectiveUser = currentUser || (mounted ? getSession() : null);
  const effectiveName = effectiveUser?.name;
  const avatarInitials = effectiveName
    ? effectiveName.split(' ').map((w) => w[0]).join('').slice(0, 2).toUpperCase()
    : 'DP';

  const userOriginalRole = effectiveUser?.role;
  const isVisitingFromOtherPortal = Boolean(userOriginalRole && ['hospital', 'rescue_team', 'citizen'].includes(userOriginalRole));
  const isCommandSection = Boolean(role === 'officer' || pathname?.startsWith('/command'));

  const handleSignOut = () => {
    signOut();
    logout();
    setRoleMenuOpen(false);
    router.push('/login');
  };

  const TRANSLATED_ROLES: Record<UserRole, string> = {
    citizen: t('portal.citizen'),
    officer: t('portal.command'),
    rescue_team: t('portal.rescue'),
    hospital: t('portal.hospital'),
    admin: t('portal.admin'),
  };

  const LANG_OPTIONS = [
    { code: 'en', label: 'English', short: 'EN' },
    { code: 'ta', label: 'தமிழ்', short: 'TA' },
    { code: 'si', label: 'සිංහල', short: 'SI' },
  ] as const;

  return (
    <header className="sticky top-0 z-[10000]">
      {showTicker && <AlertTicker />}
      <div className="bg-nova-bg/95 backdrop-blur-lg border-b border-nova-border">
        <div className="flex items-center justify-between px-4 h-14 gap-4">
          <NovaLogo size="md" />

          {/* Search */}
          <div className="flex-1 max-w-xs hidden md:block">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-nova-text-muted" />
              <input
                type="text"
                placeholder={t('common.search_placeholder')}
                className="w-full bg-nova-surface border border-nova-border rounded-lg pl-9 pr-3 py-1.5 text-xs text-nova-text placeholder:text-nova-text-muted focus:outline-none focus:border-nova-cyan/40 transition-colors"
              />
            </div>
          </div>

          {/* Right Controls */}
          <div className="flex items-center gap-2">
            {/* Go to Command Center button (shown outside Command Center) */}
            {mounted && !isCommandSection && (
              <Link
                href="/command"
                className="flex items-center gap-1.5 text-xs font-semibold bg-nova-cyan/10 text-nova-cyan border border-nova-cyan/30 rounded-lg px-2.5 py-1.5 hover:bg-nova-cyan/20 transition-all mr-1"
                title="Go to Command Center"
              >
                <Globe className="w-3.5 h-3.5" />
                <span>Command Center</span>
              </Link>
            )}

            {/* Back button (shown inside Command Center when user belongs to Hospital or Rescue) */}
            {mounted && isCommandSection && isVisitingFromOtherPortal && (
              <Link
                href={ROLE_LINKS[userOriginalRole as UserRole]}
                className="flex items-center gap-1.5 text-xs font-semibold bg-nova-cyan/15 text-nova-cyan border border-nova-cyan/40 rounded-lg px-3 py-1.5 hover:bg-nova-cyan/25 transition-all mr-1 shadow-sm"
                title={`Return to ${TRANSLATED_ROLES[userOriginalRole as UserRole]}`}
              >
                <ChevronLeft className="w-4 h-4" />
                <span>Back to {TRANSLATED_ROLES[userOriginalRole as UserRole]}</span>
              </Link>
            )}

            {/* Language Switcher */}
            <div className="flex items-center gap-0.5 bg-nova-surface border border-nova-border rounded-lg p-0.5">
              {LANG_OPTIONS.map((opt) => (
                <button
                  key={opt.code}
                  onClick={() => setLanguage(opt.code)}
                  className={cn(
                    'px-2 py-1 rounded-md text-[10px] font-bold transition-all',
                    language === opt.code
                      ? 'bg-nova-cyan text-nova-bg'
                      : 'text-nova-text-muted hover:text-nova-text'
                  )}
                >
                  {opt.short}
                </button>
              ))}
            </div>

            <NotificationBell />

            {/* Interactive Role Switcher Dropdown */}
            <div className="relative">
              <button
                onClick={() => setRoleMenuOpen(!roleMenuOpen)}
                className="flex items-center gap-1.5 text-xs font-medium text-nova-text-dim bg-nova-surface border border-nova-border rounded-lg px-3 py-1.5 hover:border-nova-border2 hover:text-nova-text transition-colors"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-nova-cyan animate-pulse-slow" />
                {role ? TRANSLATED_ROLES[role] : t('btn.select_portal')}
                <ChevronDown className="w-3.5 h-3.5 text-nova-text-muted ml-0.5" />
              </button>

              <AnimatePresence>
                {roleMenuOpen && (
                  <>
                    <div className="fixed inset-0 z-40" onClick={() => setRoleMenuOpen(false)} />
                    <motion.div
                      className="absolute right-0 top-9 w-52 nova-card border border-nova-border rounded-xl shadow-nova z-50 overflow-hidden p-1.5 space-y-0.5"
                      initial={{ opacity: 0, y: -5, scale: 0.97 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: -5, scale: 0.97 }}
                      transition={{ duration: 0.15 }}
                    >
                      <div className="px-2.5 py-1.5 text-[9px] font-bold text-nova-text-muted uppercase tracking-wider">
                        {t('btn.select_portal')}
                      </div>
                      {Object.entries(ROLE_LINKS).map(([key, href]) => (
                        <Link
                          key={key}
                          href={href}
                          onClick={() => setRoleMenuOpen(false)}
                          className={cn(
                            'flex items-center gap-2 w-full text-left px-2.5 py-2 rounded-lg text-xs font-medium transition-colors',
                            role === key
                              ? 'bg-nova-cyan/10 text-nova-cyan'
                              : 'text-nova-text-dim hover:bg-nova-surface2 hover:text-nova-text'
                          )}
                        >
                          <div className={cn('w-1.5 h-1.5 rounded-full', role === key ? 'bg-nova-cyan' : 'bg-nova-border')} />
                          {TRANSLATED_ROLES[key as UserRole]}
                        </Link>
                      ))}
                      <div className="border-t border-nova-border my-1" />
                      <Link
                        href="/"
                        onClick={() => setRoleMenuOpen(false)}
                        className="flex items-center gap-2 w-full text-left px-2.5 py-2 rounded-lg text-xs font-medium text-nova-text-muted hover:text-nova-text hover:bg-nova-surface2 transition-colors"
                      >
                        🚪 {t('btn.home')}
                      </Link>
                      {currentUser && isAuthenticated ? (
                        <button
                          onClick={handleSignOut}
                          className="flex items-center gap-2 w-full text-left px-2.5 py-2 rounded-lg text-xs font-medium text-red-400 hover:text-red-300 hover:bg-red-500/10 transition-colors"
                        >
                          <LogOut className="w-3.5 h-3.5" /> {t('btn.sign_out')}
                        </button>
                      ) : (
                        <Link
                          href="/login"
                          onClick={() => setRoleMenuOpen(false)}
                          className="flex items-center gap-2 w-full text-left px-2.5 py-2 rounded-lg text-xs font-medium text-nova-cyan hover:bg-nova-surface2 transition-colors"
                        >
                          🔑 Sign In
                        </Link>
                      )}
                    </motion.div>
                  </>
                )}
              </AnimatePresence>
            </div>

            {/* Profile avatar or Sign In button */}
            {currentUser && isAuthenticated ? (
              <div
                title={currentUser?.name ?? 'Profile'}
                className="w-8 h-8 rounded-full bg-gradient-to-br from-nova-cyan to-nova-blue flex items-center justify-center text-nova-bg text-xs font-bold cursor-pointer hover:scale-105 transition-transform flex-shrink-0"
                onClick={() => setRoleMenuOpen(!roleMenuOpen)}
              >
                {avatarInitials}
              </div>
            ) : (
              <Link
                href="/login?portal=citizen"
                className="text-xs font-bold bg-nova-cyan text-nova-bg px-3.5 py-1.5 rounded-lg hover:bg-nova-cyan-dim transition-all whitespace-nowrap shadow-sm ml-1"
              >
                Sign In
              </Link>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}

