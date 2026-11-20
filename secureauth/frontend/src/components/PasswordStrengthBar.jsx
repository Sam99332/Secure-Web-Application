import { CheckCircle2, Circle } from 'lucide-react';
import { evaluatePasswordStrength } from '../utils/passwordStrength';

export function PasswordStrengthBar({ password }) {
  const { score, label, rules } = evaluatePasswordStrength(password);
  const colors = ['bg-slate-300', 'bg-red-500', 'bg-amber-500', 'bg-sky-500', 'bg-emerald-500'];
  const widthPct = `${(score / 4) * 100}%`;

  return (
    <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/60 p-3 space-y-2">
      <div className="flex items-center justify-between text-xs font-semibold">
        <span className="text-slate-600 dark:text-slate-400">Password Strength</span>
        <span className="text-slate-900 dark:text-white">{label}</span>
      </div>
      <div className="h-1.5 w-full rounded-full bg-slate-200 dark:bg-slate-800 overflow-hidden">
        <div className={`h-full transition-all duration-300 ${colors[score]}`} style={{ width: widthPct }} />
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-1 text-xs text-slate-600 dark:text-slate-400">
        {[
          ['8+ characters', rules.minLength],
          ['Uppercase letter', rules.uppercase],
          ['Lowercase letter', rules.lowercase],
          ['Digit (0-9)', rules.digit],
          ['Special symbol', rules.special]
        ].map(([text, ok]) => (
          <div key={text} className="flex items-center gap-1.5">
            {ok ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" /> : <Circle className="w-3.5 h-3.5 opacity-40" />}
            <span className={ok ? 'text-slate-900 dark:text-slate-100' : ''}>{text}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
