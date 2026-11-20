import { createContext, useCallback, useEffect, useState } from 'react';
import { authApi, registerUnauthorizedHandler, userApi } from '../services/api';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    try {
      const res = await userApi.getMe();
      setUser(res.data.data);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    registerUnauthorizedHandler(() => {
      setUser(null);
    });
    refreshUser();
  }, [refreshUser]);

  const login = async (credentials) => {
    const res = await authApi.login(credentials);
    const sessionUser = res.data.data.user;
    setUser(sessionUser);
    return sessionUser;
  };

  const register = async (payload) => {
    const res = await authApi.register(payload);
    const sessionUser = res.data.data.user;
    setUser(sessionUser);
    return sessionUser;
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  };

  const isAdmin = Boolean(user?.roles?.includes('ROLE_ADMIN'));

  return (
    <AuthContext.Provider value={{ user, loading, isAdmin, login, register, logout, setUser, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}
