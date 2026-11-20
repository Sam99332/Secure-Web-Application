import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { ThemeProvider } from '../context/ThemeContext';
import { AppRoutes } from '../App';
import { RegisterPage } from '../pages/auth/RegisterPage';
import { LoginPage } from '../pages/auth/LoginPage';

function renderWithAuth(ui, authValue, initialEntries = ['/']) {
  return render(
    <ThemeProvider>
      <ToastProvider>
        <AuthContext.Provider value={authValue}>
          <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
        </AuthContext.Provider>
      </ToastProvider>
    </ThemeProvider>
  );
}

describe('SecureAuth Frontend Validation & RBAC Routes', () => {
  it('validates registration form fields before submit', async () => {
    const registerMock = vi.fn();
    renderWithAuth(
      <RegisterPage />,
      { user: null, loading: false, isAdmin: false, register: registerMock }
    );
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));
    await waitFor(() => {
      expect(screen.getByText(/full name is required/i)).toBeInTheDocument();
    });
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('validates login form inputs before submit', async () => {
    const loginMock = vi.fn();
    renderWithAuth(
      <LoginPage />,
      { user: null, loading: false, isAdmin: false, login: loginMock }
    );
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/email or username is required/i)).toBeInTheDocument();
    });
    expect(loginMock).not.toHaveBeenCalled();
  });

  it('redirects unauthenticated visitors from protected dashboard to login', () => {
    renderWithAuth(
      <AppRoutes />,
      { user: null, loading: false, isAdmin: false },
      ['/dashboard']
    );
    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument();
  });

  it('restricts standard users from admin route', () => {
    renderWithAuth(
      <AppRoutes />,
      {
        user: { id: 2, fullName: 'Alex', username: 'alex', email: 'alex@example.com', roles: ['ROLE_USER'], accountStatus: 'ACTIVE' },
        loading: false,
        isAdmin: false,
        logout: vi.fn()
      },
      ['/admin']
    );
    expect(screen.getByText(/welcome, alex/i)).toBeInTheDocument();
  });
});
