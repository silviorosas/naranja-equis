import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WalletService } from '../../core/services/wallet.service';
import { TransactionService } from '../../core/services/transaction.service';
import { AuthService } from '../../core/services/auth.service';
import { Wallet } from '../../core/models/wallet.model';
import { Transaction } from '../../core/models/transaction.model';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './dashboard.component.html',
    styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
    private walletService = inject(WalletService);
    private transactionService = inject(TransactionService);
    private authService = inject(AuthService);
    private toastr = inject(ToastrService);

    wallet: Wallet | null = null;
    transactions: Transaction[] = [];
    user = this.authService.getUserInfo();

    showDepositModal = false;
    showTransferModal = false;
    amount = 0;
    targetWalletId = ''; // This will be used as receiverId

    // UI State
    isSidebarOpen = false;
    today = new Date();

    toggleSidebar() {
        this.isSidebarOpen = !this.isSidebarOpen;
    }

    ngOnInit() {
        if (this.user) {
            this.loadData();
        }
    }

    loadData() {
        if (!this.user) return;

        this.walletService.getWalletByUserId(this.user.id.toString()).subscribe(wallet => {
            this.wallet = wallet;
            this.loadTransactions(this.user!.id.toString());
        });
    }

    loadTransactions(userId: string) {
        this.transactionService.getTransactionsByUserId(userId).subscribe(txs => {
            this.transactions = txs.sort((a, b) => {
                return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
            });
        });
    }

    onDeposit() {
        if (this.amount <= 0) return;

        this.transactionService.deposit(this.amount).subscribe({
            next: () => {
                this.loadData();
                this.showDepositModal = false;
                this.amount = 0;
                this.toastr.success('Depósito realizado con éxito', 'Éxito');
            },
            error: (err) => {
                this.toastr.error(err.message || 'Error al procesar el depósito', 'Error');
            }
        });
    }

    onTransfer() {
        if (!this.targetWalletId || this.amount <= 0) return;

        // Primero buscamos la billetera por CVU/Alias para obtener el userId real
        this.walletService.lookupWallet(this.targetWalletId).subscribe({
            next: (targetWallet) => {
                if (!targetWallet) {
                    this.toastr.warning('No se encontró la billetera destino', 'Atención');
                    return;
                }

                this.transactionService.transfer({
                    receiverId: targetWallet.userId,
                    amount: this.amount
                }).subscribe({
                    next: () => {
                        this.loadData();
                        this.showTransferModal = false;
                        this.amount = 0;
                        this.targetWalletId = '';
                        this.toastr.success('Transferencia enviada correctamente', 'Éxito');
                    },
                    error: (err) => {
                        console.error('Error en transferencia', err);
                        this.toastr.error(err.message || 'Error al procesar la transferencia', 'Error');
                    }
                });
            },
            error: (err) => {
                this.toastr.error('No se pudo encontrar el destinatario. Verifica el CVU o Alias.', 'Error');
            }
        });
    }

    logout() {
        this.authService.logout();
        this.toastr.info('Has cerrado sesión correctamente', 'Adiós');
        setTimeout(() => window.location.reload(), 1000);
    }
}
