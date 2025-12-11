import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

let tokenCleanupDone = false;

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = localStorage.getItem('token');
  
  // Skip auth for login and public endpoints
  if (req.url.includes('/auth/') || req.url.includes('/settings')) {
    return next(req);
  }
  
  if (token) {
    if (isTokenValid(token)) {
      tokenCleanupDone = false;
      // Always add Authorization header for API requests
      console.log('Adding Authorization header for request:', req.url);
      req = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });
    } else if (!tokenCleanupDone) {
      // Token expired, clear it
      tokenCleanupDone = true;
      authService.logout();
      router.navigate(['/auth/login']);
    }
  } else {
    // No token and request requires authentication
    if (req.url.includes('/api/') && !req.url.includes('/auth/')) {
      console.warn('No token available for authenticated request:', req.url);
    }
  }
  
  return next(req);
};

function isTokenValid(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const currentTime = Math.floor(Date.now() / 1000);
    const isValid = payload.exp > currentTime;
    console.log('Token validation result:', { isValid, exp: payload.exp, now: currentTime });
    return isValid;
  } catch (error) {
    return false;
  }
}