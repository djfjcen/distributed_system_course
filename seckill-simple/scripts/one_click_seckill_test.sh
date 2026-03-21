#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost}"
SUFFIX="$(date +%s)"

# Parse an integer field from a flat JSON object, e.g. {"userId":12,...}
extract_int_field() {
  local json="$1"
  local field="$2"
  echo "$json" | sed -n "s/.*\"${field}\"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p"
}

print_step() {
  echo
  echo "=================================================="
  echo "$1"
  echo "=================================================="
}

print_step "0) Health check"
curl -sS "${BASE_URL}/api/products" >/dev/null
echo "OK: ${BASE_URL} is reachable"

USER_A="userA_${SUFFIX}"
USER_B="userB_${SUFFIX}"

print_step "1) Register two users"
RESP_A=$(curl -sS -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USER_A}\",\"password\":\"pass123\",\"email\":\"${USER_A}@test.com\"}")

echo "User A register response:"
echo "$RESP_A"

RESP_B=$(curl -sS -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USER_B}\",\"password\":\"pass123\",\"email\":\"${USER_B}@test.com\"}")

echo "User B register response:"
echo "$RESP_B"

USER_A_ID=$(extract_int_field "$RESP_A" "userId")
USER_B_ID=$(extract_int_field "$RESP_B" "userId")

if [[ -z "$USER_A_ID" || -z "$USER_B_ID" ]]; then
  echo "ERROR: Failed to parse userId from register response."
  exit 1
fi

echo "Parsed user IDs: A=${USER_A_ID}, B=${USER_B_ID}"

print_step "2) List products"
PRODUCTS_RESP=$(curl -sS "${BASE_URL}/api/products")
echo "$PRODUCTS_RESP"

# Keep test stable by using productId=1 (assuming exists in your seed data)
PRODUCT_ID=1

print_step "3) User A seckill product ${PRODUCT_ID}, quantity=10"
ORDER1_RESP=$(curl -sS -X POST "${BASE_URL}/api/orders/seckill?userId=${USER_A_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":${PRODUCT_ID},\"quantity\":10}")
echo "$ORDER1_RESP"
ORDER1_ID=$(extract_int_field "$ORDER1_RESP" "id")

print_step "4) User B seckill product ${PRODUCT_ID}, quantity=15"
ORDER2_RESP=$(curl -sS -X POST "${BASE_URL}/api/orders/seckill?userId=${USER_B_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":${PRODUCT_ID},\"quantity\":15}")
echo "$ORDER2_RESP"
ORDER2_ID=$(extract_int_field "$ORDER2_RESP" "id")

print_step "5) Idempotency test: User A repeats seckill on same product"
ORDER1_RETRY_RESP=$(curl -sS -X POST "${BASE_URL}/api/orders/seckill?userId=${USER_A_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":${PRODUCT_ID},\"quantity\":5}")
echo "$ORDER1_RETRY_RESP"
ORDER1_RETRY_ID=$(extract_int_field "$ORDER1_RETRY_RESP" "id")

if [[ -n "$ORDER1_ID" && -n "$ORDER1_RETRY_ID" && "$ORDER1_ID" == "$ORDER1_RETRY_ID" ]]; then
  echo "PASS: Idempotency works. Same order ID returned: ${ORDER1_ID}"
else
  echo "WARN: Idempotency check did not return the same order ID."
fi

print_step "6) Query orders by user"
USER_A_ORDERS=$(curl -sS "${BASE_URL}/api/orders/user/${USER_A_ID}")
USER_B_ORDERS=$(curl -sS "${BASE_URL}/api/orders/user/${USER_B_ID}")
echo "User A orders:"
echo "$USER_A_ORDERS"
echo "User B orders:"
echo "$USER_B_ORDERS"

print_step "7) Query orders by product"
PRODUCT_ORDERS=$(curl -sS "${BASE_URL}/api/orders/product/${PRODUCT_ID}")
echo "$PRODUCT_ORDERS"

print_step "8) Query order by orderId"
if [[ -n "$ORDER1_ID" ]]; then
  ORDER1_DETAIL=$(curl -sS "${BASE_URL}/api/orders/${ORDER1_ID}")
  echo "Order ${ORDER1_ID}:"
  echo "$ORDER1_DETAIL"
else
  echo "WARN: ORDER1_ID parse failed; skip orderId query."
fi

print_step "9) Insufficient stock test (expect failure)"
INSUFFICIENT_RESP=$(curl -sS -X POST "${BASE_URL}/api/orders/seckill?userId=${USER_B_ID}" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":${PRODUCT_ID},\"quantity\":99999}")
echo "$INSUFFICIENT_RESP"

print_step "Done"
echo "One-click test finished."
echo "Users: ${USER_A}(${USER_A_ID}), ${USER_B}(${USER_B_ID})"
echo "Orders: ${ORDER1_ID:-N/A}, ${ORDER2_ID:-N/A}"
