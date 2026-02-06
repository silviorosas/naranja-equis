export interface Transaction {
    id: number;
    senderId?: number;
    receiverId?: number;
    amount: number;
    type: 'DEPOSIT' | 'TRANSFER' | 'WITHDRAW';
    createdAt: string;
}

export interface DepositRequest {
    walletId: number;
    amount: number;
}

export interface TransferRequest {
    receiverId: number;
    amount: number;
    description?: string;
}
