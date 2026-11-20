import { Link } from 'react-router-dom';
import { ArrowRight, KeyRound, Lock, Moon, Shield, ShieldCheck, Sun, Users } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import { useAuth } from '../hooks/useAuth';

export function LandingPage() {
  const { dark, toggleTheme } = useTheme();
  const { user, isAdmin } = useAuth();

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950">
      <header className="border-b border-slate-200 dark:border-slate-800 bg-white/80 dark:bg-slate-900/80 backdrop-blur sticky top-0 z-30">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2 font-bold text-xl">
            <Shield className="w-6 h-6 text-brand-600" />
            <span>SecureAuth</span>
          </div>
          <div className="flex items-center gap-3">
            <button onClick={toggleTheme} aria-label="Toggle theme" className="p-2 rounded-lg border border-slate-200 dark:border-slate-700">
              {dark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
            </button>
            {user ? (
              <Link to={isAdmin ? '/admin' : '/dashboard'} className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700">
                Open Dashboard
              </Link>
            ) : (
              <>
                <Link to="/login" className="rounded-lg px-4 py-2 text-sm font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800">
                  Login
                </Link>
                <Link to="/register" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700">
                  Register
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-6xl mx-auto px-4 sm:px-6 py-16 space-y-20 w-full">
        <section className="rounded-3xl bg-gradient-to-br from-slate-900 via-brand-950 to-slate-900 text-white p-8 sm:p-14 shadow-2xl border border-slate-800">
          <span className="inline-flex items-center rounded-full bg-sky-500/20 px-3 py-1 text-xs font-semibold text-sky-300 tracking-wide">
            ENTERPRISE IDENTITY & ACCESS MANAGEMENT
          </span>
          <h1 className="mt-5 text-3xl sm:text-5xl font-bold tracking-tight max-w-3xl">
            Zero-Trust Authentication &amp; Role-Based User Management
          </h1>
          <p className="mt-4 text-slate-300 max-w-2xl text-base sm:text-lg">
            Built with React, Spring Boot 3, Spring Security, BCrypt hashing, HttpOnly JWT cookies, Flyway migrations, and MySQL 8.
          </p>
          <div className="mt-8 flex flex-wrap gap-4">
            <Link to="/register" className="inline-flex items-center gap-2 rounded-xl bg-sky-500 px-5 py-3 text-sm font-semibold text-slate-950 hover:bg-sky-400">
              Get Started <ArrowRight className="w-4 h-4" />
            </Link>
            <Link to="/login" className="inline-flex items-center gap-2 rounded-xl border border-slate-600 px-5 py-3 text-sm font-semibold text-white hover:bg-slate-800">
              Sign In
            </Link>
          </div>
        </section>

        <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {[
            [Lock, 'BCrypt Hashing', 'Adaptive strength-12 BCrypt hashing with zero plaintext storage.'],
            [KeyRound, 'HttpOnly JWT Cookies', 'Signed HS256 JWTs delivered via HttpOnly SameSite cookies with revocation denylist.'],
            [ShieldCheck, 'Strict RBAC', 'Method-level @PreAuthorize enforcement separating USER and ADMIN privileges.'],
            [Users, 'Admin Audit Trail', 'Search, filter, paginate, and audit every role, status, and account mutation.']
          ].map(([Icon, title, desc]) => (
            <div key={title} className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
              <Icon className="w-7 h-7 text-brand-600 mb-3" />
              <h2 className="font-bold text-lg">{title}</h2>
              <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{desc}</p>
            </div>
          ))}
        </section>

        <section className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-8">
          <h2 className="text-xl font-bold mb-4">Technology Stack</h2>
          <div className="flex flex-wrap gap-2">
            {['React 18', 'Vite', 'Tailwind CSS', 'React Router', 'React Hook Form', 'Axios', 'Java 17', 'Spring Boot 3', 'Spring Security 6', 'Spring Data JPA', 'MySQL 8', 'Flyway', 'Swagger / OpenAPI', 'Docker'].map((item) => (
              <span key={item} className="rounded-lg bg-slate-100 dark:bg-slate-800 px-3 py-1.5 text-xs font-semibold">
                {item}
              </span>
            ))}
          </div>
        </section>
      </main>

      <footer className="border-t border-slate-200 dark:border-slate-800 py-6 text-center text-sm text-slate-500">
        SecureAuth – Secure User Management &amp; Authentication System • MIT License
      </footer>
    </div>
  );
}
