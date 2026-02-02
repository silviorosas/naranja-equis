import requests
import json

print("=== Generando nuevo token ===")
login_response = requests.post(
    "http://localhost:8080/api/auth/login",
    json={
        "email": "userEmisor2@naranjax.com",
        "password": "password123"
    }
)

print(f"Status: {login_response.status_code}")
if login_response.status_code == 200:
    data = login_response.json()
    token = data['data']['accessToken']
    user_id = data['data']['user']['id']
    
    print(f"\n✅ Token generado exitosamente")
    print(f"User ID: {user_id}")
    print(f"Token: {token[:50]}...")
    
    # Ahora probamos la transferencia
    print("\n=== Test: Transferencia con token fresco ===")
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "X-User-Id": str(user_id)
    }
    
    transfer_response = requests.post(
        "http://localhost:8080/api/transactions/transfer",
        headers=headers,
        json={
            "receiverId": 12,
            "amount": 100,
            "description": "Test con validación de saldo"
        }
    )
    
    print(f"Status: {transfer_response.status_code}")
    print(f"Response: {json.dumps(transfer_response.json(), indent=2)}")
    
    # Verificar saldo después
    print("\n=== Verificando saldo después de transferencia ===")
    wallet_response = requests.get(
        f"http://localhost:8082/wallets/{user_id}"
    )
    print(f"Status: {wallet_response.status_code}")
    if wallet_response.status_code == 200:
        wallet_data = wallet_response.json()
        print(f"Saldo actual: {wallet_data.get('balance', 'N/A')}")
else:
    print(f"❌ Error en login: {login_response.text}")
