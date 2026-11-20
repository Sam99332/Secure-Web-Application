import { useEffect, useState } from 'react';
import { DashboardLayout } from '../../layouts/DashboardLayout';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../context/ToastContext';
import { userApi } from '../../services/api';
import { StatusBadge } from '../../components/StatusBadge';
import { PasswordStrengthBar } from '../../components/PasswordStrengthBar';

export function UserDashboardPage() {
  const { user, setUser } = useAuth();
  const { addToast } = useToast();
  const [activity, setActivity] = useState([]);
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [email, setEmail] = useState(user?.email || '');
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl || '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');

  useEffect(() => {
    if (user) {
      setFullName(user.fullName);
      setEmail(user.email);
      setAvatarUrl(user.avatarUrl || '');
    }
    userApi.getActivity().then((res) => setActivity(res.data.data || [])).catch(() => {});
  }, [user]);

  const handleProfileSave = async (e) => {
    e.preventDefault();
    try {
      const res = await userApi.updateMe({ fullName, email, avatarUrl });
      setUser(res.data.data);
      addToast('Profile updated.');
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to update profile.', 'error');
    }
  };

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    try {
      await userApi.changePassword({ currentPassword, newPassword });
      setCurrentPassword('');
      setNewPassword('');
      addToast('Password changed successfully.');
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to change password.', 'error');
    }
  };

  return (
    <DashboardLayout title="User Dashboard">
      <div className="space-y-6">
        <div className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold">Welcome, {user?.fullName}</h2>
            <p className="text-sm text-slate-500">@{user?.username} • {user?.email}</p>
            <p className="text-xs text-slate-400 mt-1">Member since {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : ''}</p>
          </div>
          <div className="flex items-center gap-2">
            <StatusBadge status={user?.accountStatus || 'ACTIVE'} />
            {user?.roles?.map((r) => (
              <span key={r} className="rounded-lg bg-slate-100 dark:bg-slate-800 px-2.5 py-1 text-xs font-semibold">{r}</span>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <form onSubmit={handleProfileSave} className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 space-y-4">
            <h3 className="font-bold text-lg">Update Profile</h3>
            <div>
              <label className="block text-sm font-medium mb-1">Full Name</label>
              <input value={fullName} onChange={(e) => setFullName(e.target.value)} className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-2 text-sm" required />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Email</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-2 text-sm" required />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Avatar URL</label>
              <input value={avatarUrl} onChange={(e) => setAvatarUrl(e.target.value)} placeholder="https://..." className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-2 text-sm" />
            </div>
            <button className="rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white">Save Profile</button>
          </form>

          <form onSubmit={handlePasswordChange} className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 space-y-4">
            <h3 className="font-bold text-lg">Change Password</h3>
            <div>
              <label className="block text-sm font-medium mb-1">Current Password</label>
              <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-2 text-sm" required />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">New Password</label>
              <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-2 text-sm" required />
            </div>
            <PasswordStrengthBar password={newPassword} />
            <button className="rounded-xl bg-slate-900 dark:bg-sky-500 dark:text-slate-950 px-4 py-2 text-sm font-semibold text-white">Update Password</button>
          </form>
        </div>

        <div className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
          <h3 className="font-bold text-lg mb-4">Login Activity History</h3>
          <div className="divide-y divide-slate-200 dark:divide-slate-800">
            {activity.map((item) => (
              <div key={item.id} className="py-3 flex items-center justify-between text-sm">
                <div>
                  <p className="font-medium">{new Date(item.loginTime).toLocaleString()}</p>
                  <p className="text-xs text-slate-500">IP: {item.ipAddress}</p>
                </div>
                <StatusBadge status={item.loginStatus} />
              </div>
            ))}
            {activity.length === 0 && <p className="text-sm text-slate-500">No login activity recorded.</p>}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
