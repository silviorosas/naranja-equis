import requests
import json
import time

# Registrar usuario
print("=== Registrando nuevo usuario ===")
email = f"testuser{int(time.time())}@naranjax.com"  # Email único
register_response = requests.post(
    "http://localhost:8080/api/auth/register",
    json={
        "email": email,
        "password": "password123",
        "firstName": "Test",
        "lastName": "User",
        "documentNumber": str(int(time.time())),
        "documentType": "DNI"
    }
)

print(f"Status: {register_response.status_code}")
if register_response.status_code != 200:
    print(f"❌ Error en registro: {register_response.text}")
    exit(1)

# Hacer login explícito
print("\n=== Haciendo login ===")
login_response = requests.post(
    "http://localhost:8080/api/auth/login",
    json={
        "email": email,
        "password": "password123"
    }
)

if login_response.status_code != 200:
    print(f"❌ Error en login: {login_response.text}")
    exit(1)

data = login_response.json()
token = data['data']['accessToken']
user_id = data['data']['user']['id']

print(f"✅ Login exitoso - User ID: {user_id}")

# Depositar fondos
print("\n=== Depositando fondos (5000) ===")
headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json",
    "X-User-Id": str(user_id)
}

deposit_response = requests.post(
    "http://localhost:8080/api/transactions/deposit",
    headers=headers,
    json={"amount": 5000}
)

print(f"Status: {deposit_response.status_code}")
if deposit_response.status_code == 200:
    print(f"✅ Depósito exitoso")
    print(f"Response: {json.dumps(deposit_response.json(), indent=2)}")
    
    # Esperar a que Kafka procese
    print("\n⏳ Esperando procesamiento de Kafka (3 segundos)...")
    time.sleep(3)
    
    # Verificar saldo
    print("\n=== Verificando saldo ===")
    wallet_response = requests.get(f"http://localhost:8082/wallets/{user_id}")
    if wallet_response.status_code == 200:
        wallet_data = wallet_response.json()
        print(f"✅ Saldo disponible: {wallet_data.get('balance', 'N/A')}")
        
        # Test 1: Transferencia válida
        print("\n=== Test 1: Transferencia VÁLIDA (100) ===")
        transfer_response = requests.post(
            "http://localhost:8080/api/transactions/transfer",
            headers=headers,
            json={
                "receiverId": 1,
                "amount": 100,
                "description": "Transferencia válida de prueba"
            }
        )
        print(f"Status: {transfer_response.status_code}")
        if transfer_response.status_code == 200:
            print(f"✅ Transferencia exitosa")
            print(f"Response: {json.dumps(transfer_response.json(), indent=2)}")
        else:
            print(f"❌ Error: {transfer_response.text}")
        
        # Test 2: Transferencia inválida (más de lo que tiene)
        print("\n=== Test 2: Transferencia INVÁLIDA (1000000) ===")
        transfer_response2 = requests.post(
            "http://localhost:8080/api/transactions/transfer",
            headers=headers,
            json={
                "receiverId": 1,
                "amount": 1000000,
                "description": "Transferencia que debe fallar por saldo insuficiente"
            }
        )
        print(f"Status: {transfer_response2.status_code}")
        print(f"Response: {transfer_response2.text[:300]}")
        
        if transfer_response2.status_code != 200:
            print(f"✅ Validación funcionando correctamente - transferencia rechazada")
    else:
        print(f"❌ Error verificando saldo: {wallet_response.text}")
else:
    print(f"❌ Error en depósito: {deposit_response.text}")
