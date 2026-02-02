import requests
import json

# Token del login
token = "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1VTRVIiXSwic3ViIjoidXNlckVtaXNvcjJAbmFyYW5qYXguY29tIiwiaWF0IjoxNzcwMDY2NzY4LCJleHAiOjE3NzAwNjc2Njh9.NdfMEZkqpgDsViOJ4CqjXtL4vNnIKoHv66AEnz0K-tq0khO3YEEpse6K7jJj5KrRb-4__5pguC_7-frLZpAOOQ"

# Headers
headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json",
    "X-User-Id": "13"
}

# Test 1: Direct to transaction-service (bypass gateway)
print("=== Test 1: Direct to transaction-service ===")
try:
    response = requests.post(
        "http://localhost:8083/transactions/transfer",
        headers=headers,
        json={
            "receiverId": 12,
            "amount": 100,
            "description": "Test transfer direct"
        }
    )
    print(f"Status: {response.status_code}")
    print(f"Response: {response.text}")
except Exception as e:
    print(f"Error: {e}")

print("\n=== Test 2: Through API Gateway ===")
try:
    response = requests.post(
        "http://localhost:8080/api/transactions/transfer",
        headers=headers,
        json={
            "receiverId": 12,
            "amount": 100,
            "description": "Test transfer via gateway"
        }
    )
    print(f"Status: {response.status_code}")
    print(f"Response: {response.text}")
except Exception as e:
    print(f"Error: {e}")

print("\n=== Test 3: Check wallet balance ===")
try:
    response = requests.get(
        "http://localhost:8082/wallets/13",
        headers=headers
    )
    print(f"Status: {response.status_code}")
    print(f"Response: {response.text}")
except Exception as e:
    print(f"Error: {e}")
