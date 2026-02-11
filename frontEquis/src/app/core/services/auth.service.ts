import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map } from 'rxjs';
import { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, User } from '../models/auth.model';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private http = inject(HttpClient);
    private apiUrl = '/api/auth';

    login(credentials: LoginRequest): Observable<AuthResponse> {
        return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, credentials).pipe(
            map(res => {
                const data = res.data;
                this.setToken(data.accessToken);
                this.setUserInfo(data.user);
                return data;
            })
        );
    }

    register(data: RegisterRequest): Observable<ApiResponse<any>> {
        return this.http.post<ApiResponse<any>>(`${this.apiUrl}/register`, data);
    }

    setToken(token: string): void {
        localStorage.setItem('token', token);
    }

    getToken(): string | null {
        return localStorage.getItem('token');
    }

    setUserInfo(user: User): void {
        localStorage.setItem('user', JSON.stringify(user));
    }

    getUserInfo(): User | null {
        const user = localStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    }

    logout(): void {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }

    isLoggedIn(): boolean {
        return !!this.getToken();
    }
}
