import { useCallback, useEffect, useState } from 'react';
import { DashboardLayout } from '../../layouts/DashboardLayout';
import { adminApi } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../context/ToastContext';
import { StatusBadge } from '../../components/StatusBadge';
import { ConfirmDialog } from '../../components/ConfirmDialog';

export function AdminDashboardPage() {
  const { user: currentAdmin } = useAuth();
  const { addToast } = useToast();
  const [overview, setOverview] = useState(null);
  const [usersPage, setUsersPage] = useState({ content: [], page: 0, totalPages: 1 });
  const [auditLogs, setAuditLogs] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [confirmState, setConfirmState] = useState(null);
  const [editingUser, setEditingUser] = useState(null);

  const loadAll = useCallback(async (page = 0) => {
    try {
      const [dashRes, usersRes, auditRes] = await Promise.all([
        adminApi.getDashboard(),
        adminApi.listUsers({
          search: search || undefined,
          status: statusFilter || undefined,
          role: roleFilter || undefined,
          page,
          size: 10,
          sortBy,
          sortDir: sortBy === 'createdAt' ? 'desc' : 'asc'
        }),
        adminApi.getAuditLogs({ page: 0, size: 15 })
      ]);
      setOverview(dashRes.data.data);
      setUsersPage(usersRes.data.data);
      setAuditLogs(auditRes.data.data.content || []);
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to load admin console.', 'error');
    }
  }, [search, statusFilter, roleFilter, sortBy, addToast]);

  useEffect(() => {
    loadAll(0);
  }, [loadAll]);

  const handleToggleStatus = async (target) => {
    const next = target.accountStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await adminApi.updateStatus(target.id, next);
      addToast(`User @${target.username} set to ${next}.`);
      loadAll(usersPage.page);
    } catch (err) {
      addToast(err.response?.data?.message || 'Status update failed.', 'error');
    }
  };

  const handleToggleAdminRole = async (target) => {
    const isCurrentlyAdmin = target.roles.includes('ROLE_ADMIN');
    const nextRoles = isCurrentlyAdmin ? ['ROLE_USER'] : ['ROLE_USER', 'ROLE_ADMIN'];
    try {
      await adminApi.updateRoles(target.id, nextRoles);
      addToast(`Updated roles for @${target.username}.`);
      loadAll(usersPage.page);
    } catch (err) {
      addToast(err.response?.data?.message || 'Role update failed.', 'error');
    }
  };

  const handleDeleteUser = async (target) => {
    try {
      await adminApi.deleteUser(target.id);
      addToast(`Deleted @${target.username}.`);
      loadAll(usersPage.page);
    } catch (err) {
      addToast(err.response?.data?.message || 'Delete failed.', 'error');
    }
  };

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    try {
      await adminApi.updateUser(editingUser.id, {
        fullName: editingUser.fullName,
        email: editingUser.email,
        username: editingUser.username
      });
      addToast(`Updated @${editingUser.username}.`);
      setEditingUser(null);
      loadAll(usersPage.page);
    } catch (err) {
      addToast(err.response?.data?.message || 'Update failed.', 'error');
    }
  };

  return (
    <DashboardLayout title="Admin Control Center">
      <div className="space-y-6">
        {overview && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              ['Total Users', overview.totalUsers],
              ['Active Users', overview.activeUsers],
              ['Inactive Users', overview.inactiveUsers],
              ['Administrators', overview.adminCount]
            ].map(([label, val]) => (
              <div key={label} className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5">
                <p className="text-xs font-medium text-slate-500">{label}</p>
                <p className="mt-1 text-3xl font-bold text-brand-600">{val}</p>
              </div>
            ))}
          </div>
        )}

        <div className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 space-y-4">
          <div className="flex flex-wrap gap-3 items-center justify-between">
            <h2 className="font-bold text-lg">User Management</h2>
            <div className="flex flex-wrap gap-2">
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search name, email, username..."
                className="rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-1.5 text-sm"
              />
              <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-1.5 text-sm">
                <option value="">All Status</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
              <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)} className="rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-1.5 text-sm">
                <option value="">All Roles</option>
                <option value="ROLE_USER">ROLE_USER</option>
                <option value="ROLE_ADMIN">ROLE_ADMIN</option>
              </select>
              <select value={sortBy} onChange={(e) => setSortBy(e.target.value)} className="rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-1.5 text-sm">
                <option value="createdAt">Newest</option>
                <option value="fullName">Name</option>
                <option value="username">Username</option>
              </select>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="border-b border-slate-200 dark:border-slate-800 text-xs uppercase text-slate-500">
                <tr>
                  <th className="py-3 pr-4">User</th>
                  <th className="py-3 pr-4">Status</th>
                  <th className="py-3 pr-4">Roles</th>
                  <th className="py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {usersPage.content.map((u) => (
                  <tr key={u.id}>
                    <td className="py-3 pr-4">
                      <p className="font-semibold">{u.fullName}</p>
                      <p className="text-xs text-slate-500">@{u.username} • {u.email}</p>
                    </td>
                    <td className="py-3 pr-4"><StatusBadge status={u.accountStatus} /></td>
                    <td className="py-3 pr-4">{u.roles.join(', ')}</td>
                    <td className="py-3 text-right space-x-2">
                      <button onClick={() => setEditingUser({ ...u })} className="text-xs font-semibold text-brand-600 hover:underline">Edit</button>
                      {u.id !== currentAdmin?.id && (
                        <>
                          <button onClick={() => handleToggleAdminRole(u)} className="text-xs font-semibold text-amber-600 hover:underline">
                            {u.roles.includes('ROLE_ADMIN') ? 'Demote' : 'Make Admin'}
                          </button>
                          <button onClick={() => setConfirmState({ type: 'status', target: u })} className="text-xs font-semibold text-slate-600 dark:text-slate-300 hover:underline">
                            {u.accountStatus === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                          </button>
                          <button onClick={() => setConfirmState({ type: 'delete', target: u })} className="text-xs font-semibold text-red-600 hover:underline">
                            Delete
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
          <h2 className="font-bold text-lg mb-4">Audit Logs</h2>
          <div className="divide-y divide-slate-200 dark:divide-slate-800 text-sm">
            {auditLogs.map((log) => (
              <div key={log.id} className="py-3 flex flex-wrap justify-between gap-2">
                <div>
                  <span className="font-semibold text-brand-600">{log.action}</span>
                  <span className="mx-2 text-slate-400">•</span>
                  <span>{log.details}</span>
                  <p className="text-xs text-slate-500">Admin: @{log.adminUsername} • Target User ID: {log.targetUserId ?? 'N/A'}</p>
                </div>
                <span className="text-xs text-slate-400">{new Date(log.timestamp).toLocaleString()}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {editingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4">
          <form onSubmit={handleSaveEdit} className="w-full max-w-md rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 space-y-4">
            <h3 className="font-bold text-lg">Edit User</h3>
            <input value={editingUser.fullName} onChange={(e) => setEditingUser({ ...editingUser, fullName: e.target.value })} className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-2 text-sm" required />
            <input type="email" value={editingUser.email} onChange={(e) => setEditingUser({ ...editingUser, email: e.target.value })} className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-2 text-sm" required />
            <input value={editingUser.username} onChange={(e) => setEditingUser({ ...editingUser, username: e.target.value })} className="w-full rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent px-3 py-2 text-sm" required />
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setEditingUser(null)} className="rounded-lg border px-4 py-2 text-sm">Cancel</button>
              <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white">Save</button>
            </div>
          </form>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(confirmState)}
        title={confirmState?.type === 'delete' ? 'Delete User Account' : 'Change Account Status'}
        message={
          confirmState?.type === 'delete'
            ? `Permanently delete @${confirmState?.target?.username}? This action will be recorded in audit logs.`
            : `Toggle account status for @${confirmState?.target?.username}?`
        }
        confirmLabel={confirmState?.type === 'delete' ? 'Delete' : 'Confirm'}
        destructive={confirmState?.type === 'delete'}
        onCancel={() => setConfirmState(null)}
        onConfirm={() => {
          const current = confirmState;
          setConfirmState(null);
          if (current.type === 'delete') handleDeleteUser(current.target);
          else handleToggleStatus(current.target);
        }}
      />
    </DashboardLayout>
  );
}
