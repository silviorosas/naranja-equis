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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Wallets", description = "Gestión de billeteras de usuarios")
public class WalletController {

    private final WalletService walletService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Obtener billetera por ID de usuario", description = "Recupera la información de la billetera asociada a un usuario específico")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billetera encontrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billetera no encontrada")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Wallet>> getWalletByUserId(@PathVariable Long userId) {
        return walletService.getWalletByUserId(userId)
                .map(wallet -> ResponseEntity.ok(ApiResponse.success(wallet, "Billetera obtenida")))
                .orElse(ResponseEntity.notFound().build());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Buscar billetera por alias o CVU", description = "Busca una billetera utilizando su alias o clave virtual uniforme (CVU)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Billetera encontrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Billetera no encontrada para el identificador dado")
    })
    @GetMapping("/lookup/{identifier}")
    public ResponseEntity<ApiResponse<Wallet>> lookupWallet(@PathVariable String identifier) {
        return walletService.lookupWallet(identifier)
                .map(wallet -> ResponseEntity.ok(ApiResponse.success(wallet, "Billetera encontrada")))
                .orElse(ResponseEntity.notFound().build());
    }
}
