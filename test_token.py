
import jwt
import datetime
import requests

secret = "naranjax-secret-key-change-in-production-minimum-256-bits-required-very-long-string"
payload = {
    "sub": "test@test.com",
    "roles": ["ROLE_USER"],
    "iat": datetime.datetime.utcnow(),
    "exp": datetime.datetime.utcnow() + datetime.timedelta(hours=1)
}
token = jwt.encode(payload, secret, algorithm="HS256")
print(f"Generated Token: {token}")

headers = {
    "Authorization": f"Bearer {token}",
    "X-User-Id": "1"
}
url = "http://localhost:8080/api/transactions/deposit?amount=100"
try:
    response = requests.post(url, headers=headers)
    print(f"Status: {response.status_code}")
    print(f"Body: {response.text}")
except Exception as e:
    print(f"Error: {e}")
