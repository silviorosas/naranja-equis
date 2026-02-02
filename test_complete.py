import requests
import json

# Primero registramos un nuevo usuario para tener credenciales frescas
print("=== Registrando nuevo usuario ===")
register_response = requests.post(
    "http://localhost:8080/api/auth/register",
    json={
        "email": "testuser@naranjax.com",
        "password": "password123",
        "firstName": "Test",
        "lastName": "User",
        "documentNumber": "99999999",
        "documentType": "DNI"
    }
)

print(f"Status: {register_response.status_code}")
if register_response.status_code == 200:
    data = register_response.json()
    token = data['data']['accessToken']
    user_id = data['data']['user']['id']
    
    print(f"\n✅ Usuario registrado exitosamente")
    print(f"User ID: {user_id}")
    
    # Hacer un depósito primero para tener saldo
    print("\n=== Depositando fondos ===")
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "X-User-Id": str(user_id)
    }
    
    deposit_response = requests.post(
        "http://localhost:8080/api/transactions/deposit",
        headers=headers,
        json={
            "amount": 5000
        }
    )
    
    print(f"Deposit Status: {deposit_response.status_code}")
    if deposit_response.status_code == 200:
        print(f"✅ Depósito exitoso")
        
        # Esperar un momento para que Kafka procese
        import time
        time.sleep(2)
        
        # Verificar saldo
        print("\n=== Verificando saldo ===")
        wallet_response = requests.get(f"http://localhost:8082/wallets/{user_id}")
        if wallet_response.status_code == 200:
            wallet_data = wallet_response.json()
            print(f"Saldo disponible: {wallet_data.get('balance', 'N/A')}")
            
            # Ahora intentar transferencia válida
            print("\n=== Test 1: Transferencia válida (100) ===")
            transfer_response = requests.post(
                "http://localhost:8080/api/transactions/transfer",
                headers=headers,
                json={
                    "receiverId": 1,  # Asumiendo que existe user ID 1
                    "amount": 100,
                    "description": "Transferencia de prueba válida"
                }
            )
            print(f"Status: {transfer_response.status_code}")
            print(f"Response: {transfer_response.text[:200]}")
            
            # Intentar transferencia inválida (más de lo que tiene)
            print("\n=== Test 2: Transferencia inválida (1000000) ===")
            transfer_response2 = requests.post(
                "http://localhost:8080/api/transactions/transfer",
                headers=headers,
                json={
                    "receiverId": 1,
                    "amount": 1000000,
                    "description": "Transferencia que debe fallar"
                }
            )
            print(f"Status: {transfer_response2.status_code}")
            print(f"Response: {transfer_response2.text[:200]}")
    else:
        print(f"❌ Error en depósito: {deposit_response.text}")
else:
    print(f"❌ Error en registro: {register_response.text}")
