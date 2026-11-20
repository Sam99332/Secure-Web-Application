export function StatusBadge({ status }) {
  const positive = status === 'ACTIVE' || status === 'SUCCESS';
  const warning = status === 'INACTIVE_BLOCKED' || status === 'LOCKED';
  const style = positive
    ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300'
    : warning
    ? 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300'
    : 'bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300';
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${style}`}>
      {status}
    </span>
  );
}
