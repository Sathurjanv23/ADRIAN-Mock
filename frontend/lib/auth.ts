// ─── Auth utilities for PROJECT NOVA ─────────────────────────
// Real JWT-based auth via Spring Boot backend
// Replaces localStorage mock auth

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
const TOKEN_KEY = 'nova_token';
const USER_KEY  = 'nova_user';

// ─── AuthUser ────────────────────────────────────────────────

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: 'citizen' | 'officer' | 'rescue_team' | 'hospital' | 'admin';
  status?: 'ACTIVE' | 'PENDING_VERIFICATION' | 'DEACTIVATED';
  approvalStatus?: 'APPROVED' | 'PENDING_APPROVAL' | 'REJECTED';
  avatarUrl?: string;
  phone?: string;
  district?: string;
  organization?: string;
  rescueTeamId?: string;
  createdAt: string;
  lastActive: string;
  isActive: boolean;
  isVerified: boolean;
  language: 'en' | 'ta' | 'si';
}

export const SL_DISTRICTS = [
  'Colombo', 'Gampaha', 'Kalutara', 'Kandy', 'Matale', 'Nuwara Eliya',
  'Galle', 'Matara', 'Hambantota', 'Jaffna', 'Kilinochchi', 'Mannar',
  'Mullaitivu', 'Vavuniya', 'Trincomalee', 'Batticaloa', 'Ampara',
  'Kurunegala', 'Puttalam', 'Anuradhapura', 'Polonnaruwa', 'Badulla',
  'Moneragala', 'Ratnapura', 'Kegalle',
] as const;

// ─── Validation ──────────────────────────────────────────────

export function validateEmail(email: string): string | null {
  if (!email || !email.trim()) return 'Email is required';
  const trimmed = email.trim();
  const re = /^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/;
  if (!re.test(trimmed)) return 'Enter a valid email address (e.g. you@example.com)';
  const blockedDomains = ['mailinator.com', 'guerrillamail.com', 'tempmail.com', 'throwam.com', 'trashmail.com'];
  const domain = trimmed.split('@')[1]?.toLowerCase();
  if (blockedDomains.includes(domain)) return 'Disposable email addresses are not allowed';
  return null;
}

export function validatePassword(password: string): string | null {
  if (!password) return 'Password is required';
  if (password.length < 8) return 'Password must be at least 8 characters';
  if (!/[A-Z]/.test(password)) return 'Password must contain at least one uppercase letter';
  if (!/[0-9]/.test(password)) return 'Password must contain at least one number';
  return null;
}

export function validateName(name: string): string | null {
  if (!name || !name.trim()) return 'Full name is required';
  if (name.trim().length < 2) return 'Name must be at least 2 characters';
  if (name.trim().length > 60) return 'Name must be at most 60 characters';
  if (!/^[a-zA-Z\s\u0080-\uFFFF]+$/.test(name.trim())) return 'Name can only contain letters and spaces';
  return null;
}

export function validatePhone(phone?: string): string | null {
  if (!phone || !phone.trim()) return null;
  const trimmed = phone.trim();
  const cleaned = trimmed.replace(/[\s\-()]/g, '');

  // Sri Lankan local format: 07XXXXXXXX or 0XXXXXXXXX (10 digits)
  if (/^0[1-9][0-9]{8}$/.test(cleaned)) {
    return null; // Valid Sri Lankan local number
  }

  // Sri Lankan international format: +947XXXXXXXX or +94XXXXXXXXX or 947XXXXXXXX
  if (/^(\+?94)[1-9][0-9]{8}$/.test(cleaned)) {
    return null; // Valid Sri Lankan international number
  }

  // General international format: +[country code][number] (total 7 to 15 digits)
  if (/^\+?[0-9]{7,15}$/.test(cleaned)) {
    return null; // Valid international number
  }

  return 'Enter a valid phone number (e.g. +94 77 123 4567 or 077 123 4567)';
}

export function isSriLankanPhone(phone?: string): boolean {
  if (!phone) return false;
  const cleaned = phone.replace(/[\s\-()]/g, '');
  return /^0[1-9][0-9]{8}$/.test(cleaned) || /^(\+?94)[1-9][0-9]{8}$/.test(cleaned);
}

export function validateConfirmPassword(password: string, confirm: string): string | null {
  if (!confirm) return 'Please confirm your password';
  if (password !== confirm) return 'Passwords do not match';
  return null;
}

export function getPasswordStrength(password: string): {
  score: number;
  label: string;
  color: string;
} {
  if (!password) return { score: 0, label: '', color: '' };
  let score = 0;
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++;
  if (/[0-9]/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;

  const clamped = Math.min(score, 4);
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
  const colors = ['', '#ef4444', '#f97316', '#eab308', '#22c55e'];

  return { score: clamped, label: labels[clamped], color: colors[clamped] };
}

// ─── Token helpers ────────────────────────────────────────────

export function getToken(): string {
  if (typeof window === 'undefined') return '';
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function saveToken(token: string): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem(TOKEN_KEY, token);
  }
}

export function saveUser(user: AuthUser): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }
}

export function clearSession(): void {
  if (typeof window !== 'undefined') {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
}

// ─── API helper ──────────────────────────────────────────────

async function authFetch(endpoint: string, body: Record<string, unknown>): Promise<Response> {
  return fetch(`${API_BASE}${endpoint}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

// ─── Auth Result ─────────────────────────────────────────────

export interface SignUpPayload {
  name: string;
  email: string;
  password: string;
  role: AuthUser['role'];
  phone?: string;
  district?: string;
  organization?: string;
  language?: string;
}

export type AuthResult =
  | { success: true; user: AuthUser; requiresVerification?: boolean; demoOtp?: string }
  | { success: false; error: string; field?: string; statusCode?: number };

// ─── Sign Up (Email / Password) ──────────────────────────────

export async function signUp(payload: SignUpPayload): Promise<AuthResult> {
  const nameErr = validateName(payload.name);
  if (nameErr) return { success: false, error: nameErr, field: 'name' };

  const emailErr = validateEmail(payload.email);
  if (emailErr) return { success: false, error: emailErr, field: 'email' };

  const passErr = validatePassword(payload.password);
  if (passErr) return { success: false, error: passErr, field: 'password' };

  const phoneErr = validatePhone(payload.phone);
  if (phoneErr) return { success: false, error: phoneErr, field: 'phone' };

  if (payload.role === 'admin') {
    return { success: false, error: 'Administrator accounts cannot be self-registered.' };
  }

  try {
    const body: Record<string, unknown> = { ...payload };
    const res = await authFetch('/auth/register', body);
    const json = await res.json().catch(() => ({}));

    if (res.status === 409) {
      return {
        success: false,
        error: json.message || 'An account with this email already exists.',
        statusCode: 409,
      };
    }

    if (!res.ok || !json.success) {
      return {
        success: false,
        error: json.message || 'Registration failed. Please check your details.',
        statusCode: res.status,
      };
    }

    const { accessToken, user } = json.data || {};

    if (!user?.isVerified || !accessToken) {
      return { success: true, user, requiresVerification: true };
    }

    saveToken(accessToken);
    saveUser(user);
    return { success: true, user, requiresVerification: false };
  } catch {
    return { success: false, error: 'Cannot connect to server. Please ensure the backend is running.' };
  }
}

// ─── Sign In (Email / Password) ──────────────────────────────

export async function signIn(email: string, password: string): Promise<AuthResult> {
  const emailErr = validateEmail(email);
  if (emailErr) return { success: false, error: emailErr, field: 'email' };

  if (!password) return { success: false, error: 'Password is required', field: 'password' };

  try {
    const res = await authFetch('/auth/login', { email, password });
    const json = await res.json().catch(() => ({}));

    if (res.status === 401) {
      return { success: false, error: json.message || 'Invalid email or password.', statusCode: 401 };
    }

    if (res.status === 403) {
      return {
        success: false,
        error: json.message || 'Your account is deactivated. Please contact emergency administration.',
        statusCode: 403,
      };
    }

    if (!res.ok || !json.success) {
      const msg = json.message || 'Login failed. Please try again.';
      return { success: false, error: msg, statusCode: res.status };
    }

    const { accessToken, user } = json.data || {};

    if (!accessToken) {
      return { success: false, error: 'Login failed. Security token not issued.' };
    }

    saveToken(accessToken);
    saveUser(user);
    return { success: true, user };
  } catch {
    return { success: false, error: 'Cannot connect to server. Please ensure the backend is running.' };
  }
}

// ─── OAuth Exchange (Existing Google User) ───────────────────

export async function exchangeOAuthCode(code: string): Promise<AuthResult> {
  try {
    const res = await authFetch('/auth/oauth2/exchange', { code });
    const json = await res.json().catch(() => ({}));

    if (res.status === 403) {
      return {
        success: false,
        error: json.message || 'Your account is deactivated. Please contact administration.',
        statusCode: 403,
      };
    }

    if (!res.ok || !json.success) {
      return {
        success: false,
        error: json.message || `Google authorization failed (HTTP ${res.status}). Please try again.`,
        statusCode: res.status,
      };
    }

    const { accessToken, user } = json.data || {};
    if (accessToken) saveToken(accessToken);
    if (user) saveUser(user);
    return { success: true, user };
  } catch (error) {
    return { success: false, error: error instanceof Error ? error.message : 'Cannot connect to backend server.' };
  }
}

// ─── Google Complete Registration (New Google User) ─────────

export interface GoogleRegisterPayload {
  registrationIntent: string;
  role: 'citizen' | 'officer' | 'rescue_team' | 'hospital';
  phone?: string;
  district?: string;
  organization?: string;
  language?: string;
}

export async function completeGoogleRegistration(payload: GoogleRegisterPayload): Promise<AuthResult> {
  if (payload.role === ('admin' as any)) {
    return { success: false, error: 'Administrator accounts cannot be self-registered.' };
  }

  const phoneErr = validatePhone(payload.phone);
  if (phoneErr) return { success: false, error: phoneErr, field: 'phone' };

  try {
    const res = await authFetch('/auth/google/register', payload as unknown as Record<string, unknown>);
    const json = await res.json().catch(() => ({}));

    if (!res.ok || !json.success) {
      return {
        success: false,
        error: json.message || 'Google registration failed. Please try again.',
        statusCode: res.status,
      };
    }

    const { accessToken, user } = json.data || {};
    if (accessToken) saveToken(accessToken);
    if (user) saveUser(user);
    return { success: true, user };
  } catch {
    return { success: false, error: 'Cannot connect to backend server.' };
  }
}

// ─── Request Email OTP ───────────────────────────────────────

export async function requestLoginOtp(email: string): Promise<{ success: boolean; message: string }> {
  const emailErr = validateEmail(email);
  if (emailErr) return { success: false, message: emailErr };

  try {
    const res = await authFetch('/auth/request-otp', { email });
    const json = await res.json().catch(() => ({}));

    if (!res.ok || !json.success) {
      return { success: false, message: json.message || 'Failed to send verification code.' };
    }

    return { success: true, message: json.message || 'Verification code sent to your email.' };
  } catch {
    return { success: false, message: 'Cannot connect to backend server.' };
  }
}

// ─── Sign Out ────────────────────────────────────────────────

export function signOut(): void {
  clearSession();
}

// ─── Get Session ─────────────────────────────────────────────

export function getSession(): AuthUser | null {
  if (typeof window === 'undefined') return null;
  try {
    const token = getToken();
    if (!token) return null;

    const userStr = localStorage.getItem(USER_KEY);
    if (!userStr) return null;

    const user: AuthUser = JSON.parse(userStr);
    return user;
  } catch {
    return null;
  }
}

export async function fetchCurrentUser(): Promise<AuthUser> {
  const res = await fetch(`${API_BASE}/auth/me`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok || !json.success || !json.data) {
    throw new Error(json.message || `Unable to load current user (${res.status})`);
  }
  const user = json.data as AuthUser;
  saveUser(user);
  return user;
}

// ─── Forgot Password ─────────────────────────────────────────

export async function forgotPassword(
  email: string
): Promise<{ success: boolean; message: string }> {
  const emailErr = validateEmail(email);
  if (emailErr) return { success: false, message: emailErr };

  try {
    const res = await authFetch('/auth/forgot-password', { email });
    const json = await res.json().catch(() => ({}));

    if (!res.ok || !json.success) {
      return { success: false, message: json.message || 'Failed to send verification code.' };
    }

    return { success: true, message: json.message || 'If registered, a verification code has been sent to your email.' };
  } catch {
    return { success: false, message: 'Cannot connect to backend server.' };
  }
}

// ─── Reset Password ──────────────────────────────────────────

export async function resetPassword(email: string, otp: string, newPassword: string): Promise<AuthResult> {
  const emailErr = validateEmail(email);
  if (emailErr) return { success: false, error: emailErr, field: 'email' };

  if (!otp || !otp.trim()) return { success: false, error: 'OTP code is required', field: 'otp' };

  const passErr = validatePassword(newPassword);
  if (passErr) return { success: false, error: passErr, field: 'password' };

  try {
    const res = await authFetch('/auth/reset-password', { email, otp, newPassword });
    const json = await res.json().catch(() => ({}));

    if (!res.ok || !json.success) {
      return { success: false, error: json.message || 'Password reset failed. Please try again.' };
    }

    const { accessToken, user } = json.data || {};
    if (accessToken) saveToken(accessToken);
    if (user) saveUser(user);
    return { success: true, user };
  } catch {
    return { success: false, error: 'Cannot connect to backend server.' };
  }
}

// ─── Google OAuth Initiate ───────────────────────────────────

export async function signInWithGoogle(from?: string): Promise<AuthResult> {
  if (typeof window !== 'undefined') {
    const query = from ? `?from=${encodeURIComponent(from)}` : '';
    window.location.href = `${API_BASE}/auth/oauth2/google${query}`;
    return new Promise(() => {});
  }
  return { success: false, error: 'Browser window required for Google login.' };
}

// ─── Email Verification ───────────────────────────────────────

export async function verifyEmail(
  email: string,
  otp: string
): Promise<AuthResult> {
  try {
    const res = await authFetch('/auth/verify-otp', { email, otp });
    const json = await res.json().catch(() => ({}));

    if (!res.ok || !json.success) {
      const msg = json.message || 'Invalid or expired verification code.';
      return { success: false, error: msg };
    }

    const { accessToken, user } = json.data || {};
    if (accessToken) saveToken(accessToken);
    if (user) saveUser(user);
    return { success: true, user };
  } catch {
    return { success: false, error: 'Cannot connect to server. Please ensure the backend is running.' };
  }
}

export async function resendVerificationCode(
  email: string
): Promise<{ success: boolean; message: string }> {
  try {
    const res = await fetch(`${API_BASE}/auth/resend-otp?email=${encodeURIComponent(email)}`, {
      method: 'POST',
    });
    const json = await res.json().catch(() => ({}));
    return { success: res.ok, message: json.message || 'Verification code resent.' };
  } catch {
    return { success: false, message: 'Cannot connect to server.' };
  }
}
