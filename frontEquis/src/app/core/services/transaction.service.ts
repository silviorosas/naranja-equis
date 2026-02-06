import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Transaction, TransferRequest } from '../models/transaction.model';
import { ApiResponse } from '../models/auth.model';

@Injectable({
    providedIn: 'root'
})
export class TransactionService {
    private http = inject(HttpClient);
    private apiUrl = '/api/transactions';

    deposit(amount: number): Observable<ApiResponse<Transaction>> {
        const params = new HttpParams().set('amount', amount.toString());
        return this.http.post<ApiResponse<Transaction>>(`${this.apiUrl}/deposit`, null, { params });
    }

    transfer(data: TransferRequest): Observable<ApiResponse<Transaction>> {
        return this.http.post<ApiResponse<Transaction>>(`${this.apiUrl}/transfer`, data);
    }

    getTransactionsByUserId(userId: string): Observable<Transaction[]> {
        return this.http.get<ApiResponse<Transaction[]>>(`${this.apiUrl}/user/${userId}`).pipe(
            map(res => res.data)
        );
    }
}
