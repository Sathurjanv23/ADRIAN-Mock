export default function Loading() {
  return (
    <div className="min-h-screen bg-nova-bg flex flex-col items-center justify-center p-6 space-y-4">
      <div className="relative w-12 h-12">
        <div className="w-12 h-12 rounded-2xl bg-nova-cyan/20 border border-nova-cyan/40 animate-spin" />
        <div className="absolute inset-0 flex items-center justify-center font-black text-xs text-nova-cyan">
          N
        </div>
      </div>
      <div className="space-y-1 text-center">
        <p className="text-xs font-semibold text-nova-text uppercase tracking-widest animate-pulse">
          PROJECT NOVA
        </p>
        <p className="text-[11px] text-nova-text-muted">Loading secure operational interface...</p>
      </div>
    </div>
  );
}
