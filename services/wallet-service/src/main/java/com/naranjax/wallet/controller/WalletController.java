package com.naranjax.wallet.controller;

import com.naranjax.common.dto.ApiResponse;
import com.naranjax.wallet.entity.Wallet;
import com.naranjax.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Wallet>> getWalletByUserId(@PathVariable Long userId) {
        return walletService.getWalletByUserId(userId)
                .map(wallet -> ResponseEntity.ok(ApiResponse.success(wallet, "Billetera obtenida")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/lookup/{identifier}")
    public ResponseEntity<ApiResponse<Wallet>> lookupWallet(@PathVariable String identifier) {
        return walletService.lookupWallet(identifier)
                .map(wallet -> ResponseEntity.ok(ApiResponse.success(wallet, "Billetera encontrada")))
                .orElse(ResponseEntity.notFound().build());
    }
}
