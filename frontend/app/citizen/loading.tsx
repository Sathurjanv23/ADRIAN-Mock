export default function CitizenLoading() {
  return (
    <div className="min-h-screen bg-nova-bg p-4 max-w-xl mx-auto space-y-6 animate-pulse">
      <div className="h-12 bg-nova-surface rounded-2xl border border-nova-border/50" />
      <div className="h-32 bg-nova-surface rounded-2xl border border-nova-border/50" />
      <div className="grid grid-cols-3 gap-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-28 bg-nova-surface rounded-2xl border border-nova-border/50" />
        ))}
      </div>
      <div className="h-44 bg-nova-surface rounded-2xl border border-nova-border/50" />
    </div>
  );
}
