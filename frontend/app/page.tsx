'use client';

import { motion, useScroll, useTransform, AnimatePresence } from 'framer-motion';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import { NovaLogo } from '@/components/shared/TopNav';
import {
  Zap, Map, Brain, Shield, Globe, ChevronRight, ArrowRight,
  BarChart3, Radio, Users, AlertTriangle, Activity, Cpu,
  CheckCircle, Bell, Clock, TrendingUp, Cloud, Navigation,
  Building2, Waves, ThumbsUp
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useNovaStore } from '@/lib/store/nova-store';

// ─── Counter animation hook ──────────────────────────────────

function useCounter(target: number, duration = 2000, start = false) {
  const [value, setValue] = useState(0);
  const safeTarget = Number.isFinite(target) ? target : 0;
  const safeDuration = Number.isFinite(duration) && duration > 0 ? duration : 2000;

  useEffect(() => {
    if (!start) return;
    let isMounted = true;
    const startTime = performance.now();
    let timer: ReturnType<typeof setInterval>;

    const updateValue = () => {
      if (!isMounted) return;

      const elapsed = performance.now() - startTime;
      const progress = Math.min(elapsed / safeDuration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setValue(Math.round(eased * safeTarget));

      if (progress >= 1) clearInterval(timer);
    };

    timer = setInterval(updateValue, 16);
    updateValue();

    return () => {
      isMounted = false;
      clearInterval(timer);
    };
  }, [safeTarget, safeDuration, start]);
  return value;
}

// ─── Live Stats ──────────────────────────────────────────────

function LiveStats() {
  const [started, setStarted] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(([e]) => { if (e.isIntersecting) setStarted(true); }, { threshold: 0.3 });
    if (ref.current) observer.observe(ref.current);
    return () => observer.disconnect();
  }, []);

  const activeIncidents = useCounter(1247, 2000, started);
  const peopleAssisted = useCounter(48391, 2500, started);
  const rescueTeams = useCounter(342, 1800, started);
  const averageResponse = useCounter(114, 2200, started);
  const aiPredictions = useCounter(873, 2000, started);

  const stats = [
    { value: activeIncidents, label: 'Active Incidents', unit: '', icon: <AlertTriangle className="w-5 h-5" />, color: 'text-red-400' },
    { value: peopleAssisted, label: 'People Assisted', unit: '+', icon: <Users className="w-5 h-5" />, color: 'text-nova-cyan' },
    { value: rescueTeams, label: 'Rescue Teams', unit: '', icon: <Shield className="w-5 h-5" />, color: 'text-green-400' },
    { value: averageResponse, label: 'Avg Response (min)', unit: '', icon: <Clock className="w-5 h-5" />, color: 'text-orange-400' },
    { value: aiPredictions, label: 'AI Predictions', unit: '', icon: <Brain className="w-5 h-5" />, color: 'text-purple-400' },
  ];

  return (
    <div ref={ref} className="grid grid-cols-2 md:grid-cols-5 gap-4">
      {stats.map((stat, i) => (
        <motion.div
          key={stat.label}
          className="nova-glass rounded-xl p-4 text-center"
          initial={{ opacity: 0, y: 20 }}
          animate={started ? { opacity: 1, y: 0 } : {}}
          transition={{ delay: i * 0.1, duration: 0.5 }}
        >
          <div className={cn('flex justify-center mb-2', stat.color)}>{stat.icon}</div>
          <div className={cn('text-3xl font-bold font-display tabular-nums', stat.color)}>
            {stat.value.toLocaleString()}{stat.unit}
          </div>
          <div className="text-xs text-nova-text-dim mt-1">{stat.label}</div>
        </motion.div>
      ))}
    </div>
  );
}

// ─── Animated Sri Lanka Map Visual ──────────────────────────

function HeroMapVisual() {
  const incidents = [
    { x: 45, y: 62, type: 'critical', label: 'Zone 04 · Critical' },
    { x: 38, y: 55, type: 'high', label: 'Zone 03 · High' },
    { x: 55, y: 42, type: 'medium', label: 'Zone 07 · Medium' },
    { x: 42, y: 70, type: 'critical', label: 'Colombo · Critical' },
    { x: 30, y: 48, type: 'low', label: 'Gampaha · Low' },
  ];

  const colors = { critical: '#ff3b3b', high: '#ff7a00', medium: '#ffd700', low: '#22c55e' };

  return (
    <div className="relative w-full h-full">
      {/* Map-like dark background */}
      <div className="absolute inset-0 rounded-2xl overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-[#060a14] via-[#0d1e3a] to-[#060a14]" />
        <div className="absolute inset-0 hero-grid-bg opacity-40" />

        {/* Sri Lanka silhouette SVG-like shape */}
        <svg className="absolute inset-0 w-full h-full opacity-10" viewBox="0 0 300 400" preserveAspectRatio="xMidYMid meet">
          <ellipse cx="150" cy="200" rx="80" ry="160" fill="none" stroke="#00d4ff" strokeWidth="1" />
          <ellipse cx="150" cy="200" rx="60" ry="130" fill="rgba(0,212,255,0.03)" stroke="#1a2744" strokeWidth="0.5" />
        </svg>

        {/* Scan line */}
        <motion.div
          className="absolute inset-x-0 h-0.5 bg-gradient-to-r from-transparent via-nova-cyan to-transparent opacity-60"
          animate={{ y: ['0%', '100%', '0%'] }}
          transition={{ duration: 6, repeat: Infinity, ease: 'linear' }}
        />

        {/* Grid intersection dots */}
        {Array.from({ length: 20 }, (_, i) => (
          <motion.div
            key={i}
            className="absolute w-0.5 h-0.5 rounded-full bg-nova-cyan/20"
            style={{ left: `${(i % 5) * 25 + 5}%`, top: `${Math.floor(i / 5) * 25 + 5}%` }}
            animate={{ opacity: [0.1, 0.4, 0.1] }}
            transition={{ duration: 2 + Math.random() * 2, repeat: Infinity, delay: Math.random() * 2 }}
          />
        ))}

        {/* Incident markers */}
        {incidents.map((incident, i) => (
          <div key={i} className="absolute" style={{ left: `${incident.x}%`, top: `${incident.y}%` }}>
            {/* Pulse ring */}
            <motion.div
              className="absolute -inset-3 rounded-full border"
              style={{ borderColor: colors[incident.type as keyof typeof colors] + '40' }}
              animate={{ scale: [1, 1.8, 1], opacity: [0.8, 0, 0.8] }}
              transition={{ duration: 2 + i * 0.3, repeat: Infinity }}
            />
            <motion.div
              className="absolute -inset-1.5 rounded-full border"
              style={{ borderColor: colors[incident.type as keyof typeof colors] + '60' }}
              animate={{ scale: [1, 1.4, 1], opacity: [0.6, 0, 0.6] }}
              transition={{ duration: 1.5 + i * 0.2, repeat: Infinity, delay: 0.3 }}
            />
            {/* Core dot */}
            <div
              className="w-3 h-3 rounded-full border border-white/20 relative"
              style={{ background: colors[incident.type as keyof typeof colors], boxShadow: `0 0 8px ${colors[incident.type as keyof typeof colors]}` }}
            />
            {/* Label */}
            <div className="absolute left-4 top-0 bg-nova-surface/90 border border-nova-border text-[9px] text-nova-text px-1.5 py-0.5 rounded whitespace-nowrap pointer-events-none">
              {incident.label}
            </div>
          </div>
        ))}

        {/* AI analysis lines */}
        <svg className="absolute inset-0 w-full h-full pointer-events-none" viewBox="0 0 100 100" preserveAspectRatio="none">
          <motion.line
            x1="45" y1="62" x2="42" y2="70"
            stroke="#00d4ff" strokeWidth="0.3" strokeDasharray="2,2"
            animate={{ opacity: [0.2, 0.7, 0.2] }}
            transition={{ duration: 2, repeat: Infinity }}
          />
          <motion.line
            x1="38" y1="55" x2="42" y2="70"
            stroke="#ff7a00" strokeWidth="0.3" strokeDasharray="2,2"
            animate={{ opacity: [0.2, 0.6, 0.2] }}
            transition={{ duration: 2.5, repeat: Infinity }}
          />
        </svg>

        {/* Corner HUD elements */}
        <div className="absolute top-3 left-3 text-[9px] font-mono text-nova-cyan/60 space-y-0.5">
          <div>SYS: ONLINE</div>
          <div>AI: ACTIVE</div>
          <div>TEAMS: 12</div>
        </div>
        <div className="absolute top-3 right-3 text-[9px] font-mono text-nova-cyan/60 text-right space-y-0.5">
          <div>LK-DMC-NOVA</div>
          <div>LIVE FEED</div>
          <motion.div animate={{ opacity: [1, 0.3, 1] }} transition={{ duration: 1.2, repeat: Infinity }}>
            ● REC
          </motion.div>
        </div>
        <div className="absolute bottom-3 left-3 text-[9px] font-mono text-nova-text-muted">
          6.9271°N, 79.8612°E
        </div>
      </div>
    </div>
  );
}

// ─── Feature Grid ────────────────────────────────────────────

const FEATURES = [
  { icon: <Brain className="w-6 h-6" />, title: 'Multimodal AI', desc: 'Understands text, voice, and image emergency reports in English, Tamil, and Sinhala.', color: 'from-purple-500/20 to-purple-600/5 border-purple-500/20' },
  { icon: <Map className="w-6 h-6" />, title: 'Live GIS Command', desc: 'Real-time interactive GIS integration plotting all incidents, teams, and risk zones.', color: 'from-nova-cyan/20 to-nova-cyan/5 border-nova-cyan/20' },
  { icon: <TrendingUp className="w-6 h-6" />, title: 'Predictive Intelligence', desc: 'ML models predict flood and landslide risk up to 6 hours in advance.', color: 'from-blue-500/20 to-blue-600/5 border-blue-500/20' },
  { icon: <Zap className="w-6 h-6" />, title: 'Smart Prioritization', desc: 'AI ranks all incidents by severity, vulnerability, and resource availability.', color: 'from-orange-500/20 to-orange-600/5 border-orange-500/20' },
  { icon: <Users className="w-6 h-6" />, title: 'Multi-Role Coordination', desc: 'Connects citizens, officers, rescue teams, and hospitals in one platform.', color: 'from-green-500/20 to-green-600/5 border-green-500/20' },
  { icon: <Activity className="w-6 h-6" />, title: 'Digital Twin', desc: 'Simulate future risk scenarios to proactively deploy resources.', color: 'from-pink-500/20 to-pink-600/5 border-pink-500/20' },
];

// ─── How NOVA Works ──────────────────────────────────────────

const HOW_STEPS = [
  { icon: '📱', step: '01', title: 'Citizen Reports', desc: 'Citizens send SOS via text, voice, or image in any language' },
  { icon: '🤖', step: '02', title: 'AI Understands', desc: 'Multimodal AI analyzes report, detects severity & type in <5 seconds' },
  { icon: '🎯', step: '03', title: 'Priority Assigned', desc: 'NOVA ranks incident against all active emergencies automatically' },
  { icon: '🗺️', step: '04', title: 'Team Dispatched', desc: 'Nearest capable team is recommended and deployed via live map' },
  { icon: '🏥', step: '05', title: 'Hospital Coordinated', desc: 'Medical facilities receive patient alerts and ETA before arrival' },
  { icon: '📊', step: '06', title: 'AI Analyses', desc: 'After-action reports generated to continuously improve response' },
];

// ─── Preparedness Types ──────────────────────────────────────

const EMERGENCY_TYPES = [
  { icon: '🌊', label: 'Flood', risk: 82 },
  { icon: '⛰️', label: 'Landslide', risk: 55 },
  { icon: '🔥', label: 'Fire', risk: 28 },
  { icon: '🌍', label: 'Earthquake', risk: 15 },
  { icon: '🌪️', label: 'Severe Weather', risk: 68 },
  { icon: '🏥', label: 'Medical', risk: 40 },
];

// ─── Main Landing Page ───────────────────────────────────────

export default function LandingPage() {
  const router = useRouter();
  const { simulation } = useNovaStore();

  return (
    <div className="min-h-screen bg-nova-bg">
      {/* ─ Navigation ─ */}
      <nav className="sticky top-0 z-50 bg-nova-bg/90 backdrop-blur-xl border-b border-nova-border/50">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <NovaLogo size="md" />
          <div className="hidden md:flex items-center gap-6 text-sm text-nova-text-dim">
            <Link href="/about" className="hover:text-nova-text transition-colors">About</Link>
            <Link href="/preparedness" className="hover:text-nova-text transition-colors">Preparedness</Link>
            <Link href="/command" className="hover:text-nova-text transition-colors">Command Center</Link>
          </div>
          <div className="flex items-center gap-3">
            <Link href="/login" className="text-sm text-nova-text-dim hover:text-nova-text border border-nova-border px-3 py-1.5 rounded-lg hover:border-nova-border2 transition-all">
              Sign In
            </Link>
            <Link href="/register" className="text-sm font-semibold bg-nova-cyan text-nova-bg px-4 py-1.5 rounded-lg hover:bg-nova-cyan-dim transition-all">
              Get Started
            </Link>
          </div>
        </div>
      </nav>

      {/* ─ Hero Section ─ */}
      <section className="relative min-h-[90vh] flex items-center overflow-hidden">
        {/* Background effects */}
        <div className="absolute inset-0 hero-grid-bg opacity-30 pointer-events-none" />
        <motion.div
          className="absolute top-1/3 left-1/4 w-96 h-96 rounded-full blur-[120px] pointer-events-none"
          style={{ background: 'rgba(0,212,255,0.06)' }}
          animate={{ scale: [1, 1.2, 1], opacity: [0.6, 1, 0.6] }}
          transition={{ duration: 8, repeat: Infinity }}
        />
        <motion.div
          className="absolute bottom-1/4 right-1/4 w-64 h-64 rounded-full blur-[80px] pointer-events-none"
          style={{ background: 'rgba(255,59,59,0.05)' }}
          animate={{ scale: [1, 1.3, 1], opacity: [0.4, 0.8, 0.4] }}
          transition={{ duration: 6, repeat: Infinity, delay: 2 }}
        />

        <div className="max-w-7xl mx-auto px-6 py-20 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center w-full relative z-10">
          {/* Left: Hero Text */}
          <motion.div
            className="space-y-8"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
          >
            {/* Status badge */}
            <motion.div
              className="inline-flex items-center gap-2 bg-red-500/10 border border-red-500/30 text-red-300 text-xs font-semibold px-3 py-1.5 rounded-full"
              animate={{ opacity: [1, 0.6, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-red-400 animate-ping-slow" />
              ACTIVE — 1,247 Monitored Incidents Nationwide
            </motion.div>

            <div>
              <h1 className="text-5xl md:text-6xl font-bold font-display leading-tight">
                <span className="text-nova-text">Predict. </span>
                <span className="text-nova-cyan text-glow-cyan">Respond. </span>
                <span className="text-nova-text">Protect.</span>
              </h1>
              <p className="text-lg text-nova-text-dim mt-4 max-w-lg leading-relaxed">
                An intelligent emergency response network that transforms real-time citizen reports into coordinated rescue decisions.
              </p>
              <p className="text-sm text-nova-cyan/80 mt-2 font-mono tracking-wide">
                "From Emergency Reports to Intelligent Action — in Seconds."
              </p>
            </div>

            <div className="flex flex-wrap gap-3 relative z-20">
              <Link
                href="/command"
                onClick={(e) => {
                  e.preventDefault();
                  router.push('/command');
                }}
                className="group flex items-center gap-2 bg-nova-cyan text-nova-bg font-bold px-6 py-3 rounded-xl hover:bg-nova-cyan-dim transition-all hover:shadow-nova-cyan cursor-pointer select-none"
              >
                <Map className="w-4 h-4" />
                Launch Command Center
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </Link>
              <Link
                href="/citizen/sos"
                onClick={(e) => {
                  e.preventDefault();
                  router.push('/citizen/sos');
                }}
                className="flex items-center gap-2 bg-red-500/10 border border-red-500/30 text-red-300 font-bold px-6 py-3 rounded-xl hover:bg-red-500/20 transition-all cursor-pointer select-none"
              >
                <Radio className="w-4 h-4" />
                Report Emergency
              </Link>
            </div>

            {/* Trust badges */}
            <div className="flex flex-wrap items-center gap-4 text-xs text-nova-text-muted">
              {['AI-Powered Analysis', 'Real-Time GIS', 'Multilingual', 'End-to-End Encrypted'].map((badge) => (
                <span key={badge} className="flex items-center gap-1.5">
                  <CheckCircle className="w-3.5 h-3.5 text-nova-low" />
                  {badge}
                </span>
              ))}
            </div>
          </motion.div>

          {/* Right: Hero Map Visual */}
          <motion.div
            className="relative h-[500px] rounded-2xl overflow-hidden border border-nova-border shadow-nova"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            <HeroMapVisual />
            {/* Overlay gradient */}
            <div className="absolute inset-0 bg-gradient-to-t from-nova-bg/40 via-transparent to-transparent pointer-events-none" />
          </motion.div>
        </div>
      </section>

      {/* ─ Live Stats ─ */}
      <section className="py-16 border-y border-nova-border/30 bg-gradient-to-b from-nova-bg to-nova-surface/20">
        <div className="max-w-7xl mx-auto px-6">
          <motion.div
            className="text-center mb-10"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <p className="text-xs font-bold text-nova-cyan uppercase tracking-widest mb-2">Live System Metrics</p>
            <h2 className="text-3xl font-bold text-nova-text">Real Impact. Real Time.</h2>
          </motion.div>
          <LiveStats />
        </div>
      </section>

      {/* ─ The Problem ─ */}
      <section className="py-20">
        <div className="max-w-7xl mx-auto px-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
            <motion.div
              initial={{ opacity: 0, x: -30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <p className="text-xs font-bold text-red-400 uppercase tracking-widest mb-3">The Problem</p>
              <h2 className="text-4xl font-bold font-display text-nova-text mb-6">
                Emergency Response is <span className="text-red-400">Broken</span>
              </h2>
              <div className="space-y-4">
                {[
                  { stat: '22 min', desc: 'Average emergency response delay in disaster scenarios' },
                  { stat: '67%', desc: 'Of critical incidents misclassified due to language barriers' },
                  { stat: '3x', desc: 'More casualties when resources are deployed without AI prioritization' },
                  { stat: '0', desc: 'Centralized real-time platforms connecting all response actors in Sri Lanka' },
                ].map((item) => (
                  <div key={item.stat} className="flex items-start gap-4 p-4 rounded-xl bg-red-500/5 border border-red-500/15">
                    <span className="text-2xl font-bold font-mono text-red-400 flex-shrink-0">{item.stat}</span>
                    <p className="text-sm text-nova-text-dim">{item.desc}</p>
                  </div>
                ))}
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <p className="text-xs font-bold text-nova-cyan uppercase tracking-widest mb-3">The Solution</p>
              <h2 className="text-4xl font-bold font-display text-nova-text mb-6">
                PROJECT <span className="text-nova-cyan text-glow-cyan">NOVA</span> Changes Everything
              </h2>
              <div className="space-y-4">
                {[
                  { stat: '<5s', desc: 'AI analysis of any emergency report, any language, any format' },
                  { stat: '87%', desc: 'Prediction accuracy for flood and landslide events up to 6 hours ahead' },
                  { stat: '23%', desc: 'Faster average response time compared to manual coordination' },
                  { stat: '5', desc: 'Role portals unified: Citizens, Officers, Rescue, Hospitals, Admin' },
                ].map((item) => (
                  <div key={item.stat} className="flex items-start gap-4 p-4 rounded-xl bg-nova-cyan/5 border border-nova-cyan/15">
                    <span className="text-2xl font-bold font-mono text-nova-cyan flex-shrink-0">{item.stat}</span>
                    <p className="text-sm text-nova-text-dim">{item.desc}</p>
                  </div>
                ))}
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* ─ How NOVA Works ─ */}
      <section className="py-20 bg-nova-surface/30 border-y border-nova-border/30">
        <div className="max-w-7xl mx-auto px-6">
          <motion.div
            className="text-center mb-14"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <p className="text-xs font-bold text-nova-cyan uppercase tracking-widest mb-2">Intelligence Pipeline</p>
            <h2 className="text-4xl font-bold font-display text-nova-text">
              How <span className="text-nova-cyan">NOVA</span> Works
            </h2>
            <p className="text-nova-text-dim mt-3 max-w-xl mx-auto">
              A seamless pipeline from citizen input to coordinated rescue action — powered by AI at every step.
            </p>
          </motion.div>

          <div className="relative">
            {/* Connection line */}
            <div className="absolute top-10 left-0 right-0 h-0.5 bg-gradient-to-r from-transparent via-nova-cyan/30 to-transparent hidden lg:block" />

            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
              {HOW_STEPS.map((step, i) => (
                <motion.div
                  key={step.step}
                  className="flex flex-col items-center text-center"
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                >
                  <div className="w-16 h-16 rounded-2xl nova-glass border border-nova-border/80 flex items-center justify-center text-2xl mb-3 relative z-10">
                    {step.icon}
                    <div className="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-nova-cyan text-nova-bg text-[9px] font-bold flex items-center justify-center">
                      {step.step}
                    </div>
                  </div>
                  <h3 className="text-xs font-bold text-nova-text mb-1">{step.title}</h3>
                  <p className="text-[10px] text-nova-text-muted leading-relaxed">{step.desc}</p>
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ─ Features Grid ─ */}
      <section className="py-20">
        <div className="max-w-7xl mx-auto px-6">
          <motion.div
            className="text-center mb-14"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <p className="text-xs font-bold text-nova-cyan uppercase tracking-widest mb-2">Platform Capabilities</p>
            <h2 className="text-4xl font-bold font-display text-nova-text">
              Built for <span className="text-nova-cyan">National Scale</span>
            </h2>
          </motion.div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {FEATURES.map((feat, i) => (
              <motion.div
                key={feat.title}
                className={cn('relative p-6 rounded-2xl border bg-gradient-to-br', feat.color)}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.08 }}
                whileHover={{ y: -3 }}
              >
                <div className="text-nova-cyan mb-4">{feat.icon}</div>
                <h3 className="text-base font-bold text-nova-text mb-2">{feat.title}</h3>
                <p className="text-sm text-nova-text-dim">{feat.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ─ Predictive Dashboard Preview ─ */}
      <section className="py-20 bg-nova-surface/20 border-y border-nova-border/30">
        <div className="max-w-7xl mx-auto px-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <p className="text-xs font-bold text-purple-400 uppercase tracking-widest mb-3">AI Prediction Engine</p>
              <h2 className="text-4xl font-bold font-display text-nova-text mb-4">
                Know What's Coming <span className="text-purple-400">Before It Happens</span>
              </h2>
              <p className="text-nova-text-dim mb-6">
                NOVA's machine learning models analyze real-time rainfall, river levels, terrain risk, and historical patterns to predict disaster scenarios up to 6 hours in advance.
              </p>
              <div className="space-y-3 mb-6">
                {[
                  { zone: 'Zone 04 — Colombo', risk: 82, type: 'Flood', time: 'Next 3 Hours' },
                  { zone: 'Zone 03 — Gampaha', risk: 74, type: 'Flood', time: 'Next 3 Hours' },
                  { zone: 'Zone 07 — Kandy', risk: 68, type: 'Landslide', time: 'Next 6 Hours' },
                ].map((pred) => (
                  <div key={pred.zone} className="p-3 rounded-xl nova-glass border border-nova-border/50 flex items-center gap-4">
                    <div className={cn('text-sm font-bold px-2 py-1 rounded', pred.risk >= 80 ? 'text-red-400 bg-red-400/10' : 'text-orange-400 bg-orange-400/10')}>
                      {pred.risk}%
                    </div>
                    <div className="flex-1">
                      <p className="text-xs font-semibold text-nova-text">{pred.zone}</p>
                      <p className="text-[10px] text-nova-text-muted">{pred.type} Risk · {pred.time}</p>
                    </div>
                    <div className="w-24 h-1.5 rounded-full bg-nova-border overflow-hidden">
                      <div className={cn('h-full rounded-full', pred.risk >= 80 ? 'bg-red-400' : 'bg-orange-400')} style={{ width: `${pred.risk}%` }} />
                    </div>
                  </div>
                ))}
              </div>
              <Link href="/command/prediction" className="inline-flex items-center gap-2 text-sm font-semibold text-nova-cyan hover:text-nova-cyan-dim transition-colors">
                View Full Prediction Dashboard <ChevronRight className="w-4 h-4" />
              </Link>
            </motion.div>

            {/* Emergency types */}
            <motion.div
              className="grid grid-cols-3 gap-3"
              initial={{ opacity: 0, x: 20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              {EMERGENCY_TYPES.map((type, i) => (
                <motion.div
                  key={type.label}
                  className="nova-glass border border-nova-border/50 rounded-xl p-4 text-center"
                  whileHover={{ scale: 1.04 }}
                  initial={{ opacity: 0, scale: 0.9 }}
                  whileInView={{ opacity: 1, scale: 1 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.05 }}
                >
                  <div className="text-3xl mb-2">{type.icon}</div>
                  <p className="text-xs font-semibold text-nova-text mb-2">{type.label}</p>
                  <div className="text-sm font-bold text-nova-cyan font-mono">{type.risk}%</div>
                  <div className="text-[10px] text-nova-text-muted">Risk Index</div>
                  <div className="mt-2 h-1 rounded-full bg-nova-border overflow-hidden">
                    <motion.div
                      className="h-full rounded-full bg-gradient-to-r from-nova-cyan to-blue-500"
                      initial={{ width: 0 }}
                      whileInView={{ width: `${type.risk}%` }}
                      viewport={{ once: true }}
                      transition={{ duration: 1, delay: 0.3 + i * 0.05 }}
                    />
                  </div>
                </motion.div>
              ))}
            </motion.div>
          </div>
        </div>
      </section>

      {/* ─ Multilingual ─ */}
      <section className="py-20">
        <div className="max-w-7xl mx-auto px-6">
          <motion.div
            className="text-center mb-10"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <p className="text-xs font-bold text-nova-cyan uppercase tracking-widest mb-2">Multilingual AI</p>
            <h2 className="text-4xl font-bold font-display text-nova-text">
              No Language <span className="text-nova-cyan">Barrier</span>
            </h2>
          </motion.div>
          <div className="nova-glass border border-nova-border/50 rounded-2xl p-8 max-w-3xl mx-auto">
            <div className="space-y-4">
              {[
                { lang: 'Tamil', flag: '🇱🇰', input: '"எங்க வீட்டுக்குள்ள தண்ணி வந்திருக்கு. அப்பாவுக்கு நடக்க முடியாது."', output: { type: 'Flood', severity: 'Critical', vulnerable: 'Elderly', action: 'Immediate Rescue' } },
                { lang: 'Sinhala', flag: '🇱🇰', input: '"ගෙදර ජලය ඇතුල් වෙලා. ළමයින් සිටිනවා."', output: { type: 'Flood', severity: 'High', vulnerable: 'Children', action: 'Priority Response' } },
                { lang: 'English', flag: '🌐', input: '"House surrounded by flood water, elderly person cannot walk."', output: { type: 'Flood', severity: 'Critical', vulnerable: 'Elderly', action: 'Immediate Rescue' } },
              ].map((item, i) => (
                <motion.div
                  key={item.lang}
                  className="grid grid-cols-1 md:grid-cols-2 gap-4 p-4 rounded-xl bg-nova-surface/50 border border-nova-border/50"
                  initial={{ opacity: 0, y: 10 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                >
                  <div>
                    <p className="text-[10px] text-nova-text-muted mb-1">{item.flag} Citizen ({item.lang})</p>
                    <p className="text-sm text-nova-text-dim italic">"{item.input}"</p>
                  </div>
                  <div className="border-l border-nova-border pl-4">
                    <p className="text-[10px] text-nova-cyan mb-1.5">🤖 AI Normalized Output</p>
                    <div className="grid grid-cols-2 gap-1 text-xs">
                      <span className="text-nova-text-muted">Type:</span> <span className="text-nova-text font-medium">{item.output.type}</span>
                      <span className="text-nova-text-muted">Severity:</span> <span className="text-red-400 font-bold">{item.output.severity}</span>
                      <span className="text-nova-text-muted">Vulnerable:</span> <span className="text-nova-text font-medium">{item.output.vulnerable}</span>
                      <span className="text-nova-text-muted">Action:</span> <span className="text-orange-400 font-medium">{item.output.action}</span>
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ─ Final CTA ─ */}
      <section className="py-24 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-nova-bg via-nova-surface/20 to-nova-bg" />
        <motion.div
          className="absolute inset-0 opacity-30"
          style={{ background: 'radial-gradient(ellipse at center, rgba(0,212,255,0.1) 0%, transparent 70%)' }}
          animate={{ scale: [1, 1.1, 1] }}
          transition={{ duration: 8, repeat: Infinity }}
        />

        <div className="relative max-w-4xl mx-auto px-6 text-center">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <div className="inline-block">
              <div className="text-xs font-bold text-nova-cyan uppercase tracking-widest mb-4">Ready to Deploy</div>
              <h2 className="text-5xl md:text-6xl font-bold font-display mb-6">
                <span className="text-nova-text">Every second </span>
                <span className="text-nova-cyan text-glow-cyan">saves lives.</span>
              </h2>
              <p className="text-xl text-nova-text-dim mb-10 max-w-2xl mx-auto">
                Join the national emergency intelligence network. From prediction to resolution — PROJECT NOVA is ready.
              </p>
            </div>

            <div className="flex flex-wrap justify-center gap-4 relative z-20">
              <Link
                href="/command"
                onClick={(e) => {
                  e.preventDefault();
                  router.push('/command');
                }}
                className="group flex items-center gap-3 bg-nova-cyan text-nova-bg font-bold text-lg px-8 py-4 rounded-2xl hover:bg-nova-cyan-dim transition-all shadow-nova-cyan cursor-pointer select-none"
              >
                <Map className="w-5 h-5" />
                Launch Command Center
                <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
              </Link>
              <Link
                href="/citizen/sos"
                onClick={(e) => {
                  e.preventDefault();
                  router.push('/citizen/sos');
                }}
                className="flex items-center gap-3 bg-red-500/10 border border-red-500/30 text-red-300 font-bold text-lg px-8 py-4 rounded-2xl hover:bg-red-500/20 transition-all cursor-pointer select-none"
              >
                <Radio className="w-5 h-5" />
                Report Emergency
              </Link>
              <Link
                href="/preparedness"
                className="flex items-center gap-3 border border-nova-border text-nova-text-dim font-bold text-lg px-8 py-4 rounded-2xl hover:border-nova-border2 hover:text-nova-text transition-all"
              >
                <Shield className="w-5 h-5" />
                Preparedness Guide
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      {/* ─ Footer ─ */}
      <footer className="border-t border-nova-border/30 py-8">
        <div className="max-w-7xl mx-auto px-6 flex flex-col md:flex-row items-center justify-between gap-4">
          <NovaLogo size="sm" />
          <div className="text-xs text-nova-text-muted text-center">
            PROJECT NOVA — AI Emergency Response Network
          </div>
          <div className="flex items-center gap-4 text-xs text-nova-text-muted">
            <Link href="/login" className="hover:text-nova-text">Login</Link>
            <Link href="/preparedness" className="hover:text-nova-text">Preparedness</Link>
            <Link href="/about" className="hover:text-nova-text">About</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
