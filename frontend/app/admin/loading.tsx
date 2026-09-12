export default function AdminLoading() {
  return (
    <div className="min-h-screen bg-nova-bg p-6 space-y-6 animate-pulse">
      <div className="h-14 bg-nova-surface rounded-2xl border border-nova-border/50" />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-28 bg-nova-surface rounded-2xl border border-nova-border/50" />
        ))}
      </div>
      <div className="h-96 bg-nova-surface rounded-2xl border border-nova-border/50" />
    </div>
  );
}
