'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { useEffect, useState } from 'react';
import { cn } from '@/lib/utils';

interface StatCardProps {
  label: string;
  value: number | string;
  unit?: string;
  icon?: React.ReactNode;
  trend?: { value: number; label: string };
  variant?: 'default' | 'critical' | 'cyan' | 'warning' | 'success';
  animate?: boolean;
  className?: string;
  suffix?: string;
}

function useCountUp(target: number, duration = 1500) {
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    if (typeof target !== 'number') return;
    const startTime = Date.now();
    const timer = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3); // ease-out-cubic
      setCurrent(Math.round(eased * target));
      if (progress >= 1) clearInterval(timer);
    }, 16);
    return () => clearInterval(timer);
  }, [target, duration]);

  return current;
}

export function StatCard({
  label,
  value,
  unit,
  icon,
  trend,
  variant = 'default',
  animate = true,
  className,
  suffix,
}: StatCardProps) {
  const numericValue = typeof value === 'number' ? value : parseInt(value as string) || 0;
  const count = useCountUp(numericValue);
  const animatedValue = animate ? count : numericValue;

  const variantStyles: Record<string, string> = {
    default: 'border-nova-border',
    critical: 'border-red-500/30 bg-red-500/5',
    cyan: 'border-nova-cyan/20 bg-nova-cyan/5',
    warning: 'border-orange-500/20 bg-orange-500/5',
    success: 'border-green-500/20 bg-green-500/5',
  };

  const iconStyles: Record<string, string> = {
    default: 'text-nova-cyan bg-nova-cyan/10',
    critical: 'text-red-400 bg-red-400/10',
    cyan: 'text-nova-cyan bg-nova-cyan/10',
    warning: 'text-orange-400 bg-orange-400/10',
    success: 'text-green-400 bg-green-400/10',
  };

  const valueStyles: Record<string, string> = {
    default: 'text-nova-text',
    critical: 'text-red-400',
    cyan: 'text-nova-cyan',
    warning: 'text-orange-400',
    success: 'text-green-400',
  };

  return (
    <motion.div
      className={cn(
        'nova-card rounded-xl p-4 border flex flex-col gap-3',
        variantStyles[variant],
        className
      )}
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      whileHover={{ scale: 1.01 }}
    >
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium text-nova-text-dim uppercase tracking-wider">{label}</p>
        {icon && (
          <div className={cn('w-8 h-8 rounded-lg flex items-center justify-center', iconStyles[variant])}>
            {icon}
          </div>
        )}
      </div>

      <div className="flex items-end gap-1.5">
        <AnimatePresence mode="wait">
          <motion.span
            key={animatedValue}
            className={cn('text-3xl font-bold font-display tabular-nums', valueStyles[variant])}
          >
            {typeof value === 'string' && !parseInt(value)
              ? value
              : animate
              ? animatedValue.toLocaleString()
              : numericValue.toLocaleString()}
          </motion.span>
        </AnimatePresence>
        {unit && <span className="text-sm text-nova-text-dim mb-1">{unit}</span>}
        {suffix && <span className="text-sm text-nova-text-dim mb-1">{suffix}</span>}
      </div>

      {trend && (
        <div className="flex items-center gap-1.5">
          <span
            className={cn(
              'text-xs font-medium',
              trend.value > 0 ? 'text-red-400' : 'text-green-400'
            )}
          >
            {trend.value > 0 ? '↑' : '↓'} {Math.abs(trend.value)}%
          </span>
          <span className="text-xs text-nova-text-muted">{trend.label}</span>
        </div>
      )}
    </motion.div>
  );
}
