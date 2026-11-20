import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Shield } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../context/ToastContext';

export function LoginPage() {
  const { login } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState('');

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    defaultValues: { rememberMe: false }
  });

  const onSubmit = async (data) => {
    setServerError('');
    try {
      const user = await login(data);
      addToast(`Welcome back, ${user.fullName}!`);
      if (user.roles?.includes('ROLE_ADMIN')) {
        navigate('/admin');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Invalid credentials or account unavailable.';
      setServerError(msg);
      addToast(msg, 'error');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-slate-50 dark:bg-slate-950">
      <div className="w-full max-w-md rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-8 shadow-xl">
        <Link to="/" className="inline-flex items-center gap-2 font-bold text-lg text-brand-600 mb-4">
          <Shield className="w-5 h-5" /> SecureAuth
        </Link>
        <h1 className="text-2xl font-bold">Sign In</h1>
        <p className="text-sm text-slate-500 mt-1">Access your protected SecureAuth workspace.</p>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
          <div>
            <label htmlFor="identifier" className="block text-sm font-medium mb-1">Email or Username</label>
            <input
              id="identifier"
              {...register('identifier', { required: 'Email or username is required' })}
              className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3.5 py-2.5 text-sm"
            />
            {errors.identifier && <p className="text-xs text-red-500 mt-1">{errors.identifier.message}</p>}
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium mb-1">Password</label>
            <div className="relative">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                {...register('password', { required: 'Password is required' })}
                className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3.5 py-2.5 pr-10 text-sm"
              />
              <button type="button" onClick={() => setShowPassword((s) => !s)} className="absolute right-3 top-2.5 text-slate-400" aria-label="Toggle password">
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {errors.password && <p className="text-xs text-red-500 mt-1">{errors.password.message}</p>}
          </div>

          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2">
              <input type="checkbox" {...register('rememberMe')} />
              <span>Remember me</span>
            </label>
            <Link to="/forgot-password" className="text-brand-600 font-medium">Forgot password?</Link>
          </div>

          {serverError && <p className="text-sm text-red-500">{serverError}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-xl bg-brand-600 py-2.5 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
          >
            {isSubmitting ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        <p className="mt-5 text-center text-sm text-slate-500">
          Need an account? <Link to="/register" className="font-semibold text-brand-600">Register</Link>
        </p>
      </div>
    </div>
  );
}
