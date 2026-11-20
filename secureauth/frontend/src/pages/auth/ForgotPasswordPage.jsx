import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Shield } from 'lucide-react';
import { authApi } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { PasswordStrengthBar } from '../../components/PasswordStrengthBar';
import { evaluatePasswordStrength } from '../../utils/passwordStrength';

export function ForgotPasswordPage() {
  const { addToast } = useToast();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const handleRequest = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      const res = await authApi.forgotPassword({ email });
      setNotice(res.data.message);
      addToast('Reset request submitted.');
    } catch (err) {
      addToast(err.response?.data?.message || 'Unable to process request.', 'error');
    } finally {
      setBusy(false);
    }
  };

  const handleReset = async (e) => {
    e.preventDefault();
    if (!evaluatePasswordStrength(newPassword).isAcceptable) {
      addToast('Password must meet security complexity rules.', 'error');
      return;
    }
    setBusy(true);
    try {
      await authApi.resetPassword({ token, newPassword });
      addToast('Password reset successful. Please sign in.');
      navigate('/login');
    } catch (err) {
      addToast(err.response?.data?.message || 'Invalid or expired reset token.', 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-slate-50 dark:bg-slate-950">
      <div className="w-full max-w-md space-y-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-8 shadow-xl">
        <Link to="/login" className="inline-flex items-center gap-2 font-bold text-lg text-brand-600">
          <Shield className="w-5 h-5" /> SecureAuth
        </Link>
        <h1 className="text-2xl font-bold">Password Recovery</h1>

        <form onSubmit={handleRequest} className="space-y-3">
          <label className="block text-sm font-medium">Registered Email</label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3.5 py-2.5 text-sm"
          />
          <button disabled={busy} className="w-full rounded-xl bg-brand-600 py-2.5 text-sm font-semibold text-white">
            Send Reset Token
          </button>
          {notice && <p className="text-xs text-emerald-600 dark:text-emerald-400">{notice}</p>}
        </form>

        <hr className="border-slate-200 dark:border-slate-800" />

        <form onSubmit={handleReset} className="space-y-3">
          <label className="block text-sm font-medium">Reset Token</label>
          <input
            required
            value={token}
            onChange={(e) => setToken(e.target.value)}
            className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3.5 py-2.5 text-sm"
          />
          <label className="block text-sm font-medium">New Password</label>
          <input
            type="password"
            required
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3.5 py-2.5 text-sm"
          />
          <PasswordStrengthBar password={newPassword} />
          <button disabled={busy} className="w-full rounded-xl bg-slate-900 dark:bg-sky-500 dark:text-slate-950 py-2.5 text-sm font-semibold text-white">
            Reset Password
          </button>
        </form>
      </div>
    </div>
  );
}
