import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Wallet } from '../models/wallet.model';
import { ApiResponse } from '../models/auth.model';

@Injectable({
    providedIn: 'root'
})
export class WalletService {
    private http = inject(HttpClient);
    private apiUrl = '/api/wallets';

    getWalletByUserId(userId: string): Observable<Wallet> {
        return this.http.get<ApiResponse<Wallet>>(`${this.apiUrl}/${userId}`).pipe(
            map(res => res.data)
        );
    }

    lookupWallet(identifier: string): Observable<Wallet> {
        return this.http.get<ApiResponse<Wallet>>(`${this.apiUrl}/lookup/${identifier}`).pipe(
            map(res => res.data)
        );
    }
}
