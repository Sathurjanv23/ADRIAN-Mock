'use client';

import { Suspense, useEffect, useState, useRef, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import { useNovaStore } from '@/lib/store/nova-store';
import { NovaLogo } from '@/components/shared/TopNav';
import {
  Loader2, Shield, AlertTriangle, Truck, Building2,
  CheckCircle, XCircle, Phone, MapPin, ChevronDown, ArrowRight,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  exchangeOAuthCode, completeGoogleRegistration,
  SL_DISTRICTS, validatePhone,
} from '@/lib/auth';
import type { AuthUser } from '@/lib/auth';
import { toast } from 'sonner';

const ROLE_OPTIONS: {
  value: 'citizen' | 'officer' | 'rescue_team' | 'hospital';
  label: string;
  badge: string;
  desc: string;
  icon: React.ReactNode;
  color: string;
}[] = [
  {
    value: 'citizen',
    label: 'Citizen',
    badge: 'Immediate Access',
    desc: 'Report emergencies, receive district disaster alerts, and request immediate rescue assistance.',
    icon: <Shield className="w-5 h-5" />,
    color: 'border-er-blue/30 text-er-blue bg-er-blue-light hover:border-nova-cyan',
  },
  {
    value: 'officer',
    label: 'Emergency Officer',
    badge: 'Command Access',
    desc: 'Incident management, resource deployment, AI situation assessment, and cross-unit coordination.',
    icon: <AlertTriangle className="w-5 h-5" />,
    color: 'border-purple-400/50 text-purple-400 bg-purple-400/10 hover:border-purple-400',
  },
  {
    value: 'rescue_team',
    label: 'Rescue Team',
    badge: 'Field Ops',
    desc: 'Real-time task dispatch, field GPS navigation, live rescue status logging, and victim extraction.',
    icon: <Truck className="w-5 h-5" />,
    color: 'border-orange-400/50 text-orange-400 bg-orange-400/10 hover:border-orange-400',
  },
  {
    value: 'hospital',
    label: 'Hospital Unit',
    badge: 'Medical Coordination',
    desc: 'Emergency room capacity, ICU triage, incoming casualty tracking, and blood bank management.',
    icon: <Building2 className="w-5 h-5" />,
    color: 'border-pink-400/50 text-pink-400 bg-pink-400/10 hover:border-pink-400',
  },
];

const ROLE_HREF: Record<string, string> = {
  citizen: '/citizen',
  officer: '/command',
  rescue_team: '/rescue',
  hospital: '/hospital',
  admin: '/admin',
};

function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useNovaStore();

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // New Google User intent state
  const [isNewUser, setIsNewUser] = useState(false);
  const [intentToken, setIntentToken] = useState('');
  const [googleEmail, setGoogleEmail] = useState('');
  const [googleName, setGoogleName] = useState('');

  // Role selection form state
  const [selectedRole, setSelectedRole] = useState<'citizen' | 'officer' | 'rescue_team' | 'hospital'>('citizen');
  const [phone, setPhone] = useState('');
  const [district, setDistrict] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Ref to prevent React StrictMode double execution
  const hasExchangedRef = useRef<string | null>(null);

  const handleSuccessfulAuth = useCallback((user: AuthUser, isNew: boolean) => {
    login({
      id: user.id,
      name: user.name,
      email: user.email,
      role: user.role,
      language: user.language || 'en',
      createdAt: user.createdAt,
      lastActive: user.lastActive,
      isActive: user.isActive,
      rescueTeamId: user.rescueTeamId,
      approvalStatus: user.approvalStatus,
    });

    if (isNew) {
      if (user.status === 'PENDING_VERIFICATION') {
        toast.info(`Account Created (${user.role.toUpperCase()})`, {
          description: 'Your specialized role registration is pending command verification.',
        });
      } else {
        toast.success(`Welcome to ADRIAN, ${user.name}!`, {
          description: 'Google account registered successfully.',
        });
      }
    } else {
      toast.success(`Welcome back, ${user.name}!`, {
        description: `Signed in with Google as ${user.role.replace('_', ' ').toUpperCase()}`,
      });
    }

    const destination = ROLE_HREF[user.role] || '/command';
    router.replace(destination);
  }, [login, router]);

  useEffect(() => {
    const code = searchParams.get('code');
    const intent = searchParams.get('intent');
    const email = searchParams.get('email');
    const name = searchParams.get('name');

    if (code) {
      // Guard against React StrictMode double-invocation.
      // IMPORTANT: must also call setLoading(false) here so the duplicate
      // execution never leaves the spinner frozen on-screen.
      if (hasExchangedRef.current === code) {
        setLoading(false);
        return;
      }
      hasExchangedRef.current = code;

      // ─── Case 1: Existing Google User (Exchange Code for JWT) ───
      setLoading(true);
      exchangeOAuthCode(code)
        .then((result) => {
          setLoading(false);
          if (!result.success) {
            setErrorMessage(result.error);
            toast.error(result.error || 'Google sign-in failed. Please try again.');
            setTimeout(() => router.replace('/login?error=oauth_failed'), 2500);
            return;
          }
          handleSuccessfulAuth(result.user, false);
        })
        .catch((err) => {
          setLoading(false);
          const msg = err instanceof Error ? err.message : 'Unexpected error during sign-in.';
          setErrorMessage(msg);
          toast.error(msg);
          setTimeout(() => router.replace('/login?error=oauth_failed'), 2500);
        });
    } else if (intent) {
      // ─── Case 2: New Google User (Mandatory Role Selection) ───
      setIsNewUser(true);
      setIntentToken(intent);
      setGoogleEmail(email ? decodeURIComponent(email) : '');
      setGoogleName(name ? decodeURIComponent(name) : 'Emergency Responder');
      setLoading(false);
    } else if (searchParams.size > 0) {
      // Only treat as invalid once searchParams have actually loaded
      // (searchParams.size === 0 means they haven't been parsed yet)
      setLoading(false);
      setErrorMessage('Invalid authentication callback. Please sign in again.');
      setTimeout(() => router.replace('/login'), 2000);
    }
  }, [searchParams, router, handleSuccessfulAuth]);

  const handleCompleteRegistration = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const phoneErr = validatePhone(phone);
    if (phoneErr) {
      setFormError(phoneErr);
      return;
    }

    setSubmitting(true);
    const result = await completeGoogleRegistration({
      registrationIntent: intentToken,
      role: selectedRole,
      phone: phone || undefined,
      district: district || undefined,
    });
    setSubmitting(false);

    if (!result.success) {
      setFormError(result.error);
      toast.error(result.error);
      return;
    }

    handleSuccessfulAuth(result.user, true);
  };

  // ─── Loading View ──────────────────────────────────────────
  if (loading) {
    return (
      <div className="flex flex-col items-center gap-4 text-center p-8">
        <Loader2 className="w-12 h-12 text-er-blue animate-spin" />
        <h2 className="text-xl font-bold text-nova-text">Authenticating with Google…</h2>
        <p className="text-xs text-em-text-dim max-w-sm">
          Verifying security credentials and retrieving command clearance from ADRIAN.
        </p>
      </div>
    );
  }

  // ─── Error View ────────────────────────────────────────────
  if (errorMessage) {
    return (
      <div className="flex flex-col items-center gap-4 text-center p-8 max-w-md">
        <div className="w-14 h-14 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center">
          <XCircle className="w-8 h-8 text-red-400" />
        </div>
        <h2 className="text-xl font-bold text-nova-text">Authentication Error</h2>
        <p className="text-sm text-red-400">{errorMessage}</p>
        <p className="text-xs text-em-text-muted">Redirecting back to login screen…</p>
      </div>
    );
  }

  // ─── Mandatory Role Selection (New Google User) ─────────────
  if (isNewUser) {
    return (
      <motion.div
        className="w-full max-w-lg relative p-4"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        <div className="em-card border border-em-border rounded-2xl p-7 shadow-nova">
          {/* Header */}
          <div className="text-center mb-6">
            <div className="flex justify-center mb-3">
              <NovaLogo size="md" />
            </div>
            <h1 className="text-2xl font-bold text-nova-text">Select Your Operational Role</h1>
            <p className="text-xs text-em-text-dim mt-1">
              Google Account Verified:{' '}
              <span className="text-er-blue font-semibold">{googleEmail}</span>
            </p>
          </div>

          <form onSubmit={handleCompleteRegistration} className="space-y-5">
            {/* Role List */}
            <div>
              <label className="text-xs font-semibold text-em-text-muted uppercase tracking-wider block mb-3">
                Choose Your Role in the Network <span className="text-red-400">*</span>
              </label>

              <div className="space-y-2.5">
                {ROLE_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setSelectedRole(opt.value)}
                    className={cn(
                      'w-full flex items-start gap-3.5 p-3.5 rounded-xl border text-left transition-all',
                      selectedRole === opt.value
                        ? opt.color
                        : 'border-em-border text-em-text-dim hover:border-em-border-strong hover:text-nova-text bg-white'
                    )}
                  >
                    <div className="mt-0.5">{opt.icon}</div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-bold text-nova-text">{opt.label}</span>
                        <span className="text-[10px] px-2 py-0.5 rounded-full bg-em-subtle border border-em-border font-medium">
                          {opt.badge}
                        </span>
                      </div>
                      <p className="text-[11px] opacity-75 mt-1 leading-snug">{opt.desc}</p>
                    </div>
                    {selectedRole === opt.value && (
                      <CheckCircle className="w-4 h-4 text-er-blue flex-shrink-0 mt-0.5" />
                    )}
                  </button>
                ))}
              </div>
            </div>

            {/* Optional Fields: Phone & District */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
              <div>
                <label className="text-xs font-medium text-em-text-dim block mb-1.5">
                  Phone Number (optional)
                </label>
                <div className="relative">
                  <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-em-text-muted pointer-events-none" />
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+94 77 123 4567"
                    className="w-full bg-white border border-em-border rounded-xl pl-10 pr-4 py-2 text-xs text-nova-text placeholder:text-em-text-muted focus:outline-none focus:border-er-blue/30 transition-all"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-medium text-em-text-dim block mb-1.5">
                  District (optional)
                </label>
                <div className="relative">
                  <MapPin className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-em-text-muted pointer-events-none" />
                  <select
                    value={district}
                    onChange={(e) => setDistrict(e.target.value)}
                    className="w-full bg-white border border-em-border rounded-xl pl-10 pr-8 py-2 text-xs text-nova-text focus:outline-none focus:border-er-blue/30 transition-all appearance-none"
                  >
                    <option value="">Select district…</option>
                    {SL_DISTRICTS.map((d) => (
                      <option key={d} value={d}>{d}</option>
                    ))}
                  </select>
                  <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-em-text-muted pointer-events-none" />
                </div>
              </div>
            </div>

            {/* Form Error */}
            <AnimatePresence>
              {formError && (
                <motion.div
                  className="flex items-center gap-2 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs"
                  initial={{ opacity: 0, y: -6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                >
                  <XCircle className="w-4 h-4 flex-shrink-0" /> {formError}
                </motion.div>
              )}
            </AnimatePresence>

            {/* Submit Button */}
            <motion.button
              type="submit"
              disabled={submitting}
              className="w-full bg-nova-cyan text-nova-bg font-bold py-3 rounded-xl hover:bg-nova-cyan-dim transition-all flex items-center justify-center gap-2 disabled:opacity-50 mt-4"
              whileHover={!submitting ? { scale: 1.01 } : {}}
              whileTap={!submitting ? { scale: 0.99 } : {}}
            >
              {submitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Creating Account…
                </>
              ) : (
                <>
                  Complete Registration &amp; Launch NOVA
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </motion.button>
          </form>
        </div>
      </motion.div>
    );
  }

  return null;
}

export default function AuthCallbackPage() {
  return (
    <div className="min-h-screen bg-em-bg flex items-center justify-center p-4 relative overflow-hidden">
      <div className="absolute inset-0 opacity-10 opacity-20 pointer-events-none" />
      <Suspense fallback={
        <div className="flex flex-col items-center gap-3 text-center p-6">
          <Loader2 className="w-10 h-10 text-er-blue animate-spin" />
          <h2 className="text-lg font-bold text-nova-text">Authenticating…</h2>
        </div>
      }>
        <AuthCallbackContent />
      </Suspense>
    </div>
  );
}
