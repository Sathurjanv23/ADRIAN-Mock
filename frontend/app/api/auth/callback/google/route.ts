import { NextRequest, NextResponse } from 'next/server';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const code = searchParams.get('code');
  const error = searchParams.get('error');

  if (error || !code) {
    return NextResponse.redirect(new URL('/login?error=google_auth_failed', request.url));
  }

  // Forward code to Spring Boot backend for token exchange and user creation
  const backendUrl = `${API_BASE}/auth/oauth2/callback/google?code=${encodeURIComponent(code)}&redirect_uri=${encodeURIComponent('http://localhost:3000/api/auth/callback/google')}`;
  
  return NextResponse.redirect(backendUrl);
}
