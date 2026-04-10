#!/bin/bash
# ============================================================================
# validate-security.sh — Valida correcoes de seguranca do SecureShop
#
# Uso: ./validate-security.sh [URL_BASE]
#      Default: http://localhost:4000
# ============================================================================

API="${1:-http://localhost:4000}"
PASS=0
FAIL=0
TOTAL=8

GREEN='\033[0;32m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║  SecureShop — Validacao de Seguranca                    ║${NC}"
echo -e "${BOLD}╠══════════════════════════════════════════════════════════╣${NC}"

TOKEN=$(curl -s -X POST "$API/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos@secureshop.com","password":"Senha123"}' \
  | grep -oE '"token":"[^"]+"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}║  ERRO: Nao foi possivel obter token de login             ║${NC}"
  echo -e "${BOLD}╚══════════════════════════════════════════════════════════╝${NC}"
  exit 1
fi

check() {
  local num="$1"
  local name="$2"
  local result="$3"
  local padded
  padded=$(printf "%-43s" "$name")
  if [ "$result" = "PASS" ]; then
    echo -e "${BOLD}║${NC}  V${num}  ${padded} ${GREEN}PASS${NC}          ${BOLD}║${NC}"
    PASS=$((PASS + 1))
  else
    echo -e "${BOLD}║${NC}  V${num}  ${padded} ${RED}FAIL${NC}          ${BOLD}║${NC}"
    FAIL=$((FAIL + 1))
  fi
}

# ── V1: SQL Injection Login ─────────────────────────────────────────────────
RESULT=$(curl -s -X POST "$API/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@secureshop.com\",\"password\":\"' OR '1'='1' --\"}")
HAS_TOKEN=$(echo "$RESULT" | grep -c '"token"')
if [ "$HAS_TOKEN" = "0" ]; then
  check 1 "SQL Injection Login" "PASS"
else
  check 1 "SQL Injection Login" "FAIL"
fi

# ── V2: Price Manipulation ──────────────────────────────────────────────────
RESULT=$(curl -s -X POST "$API/api/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"items":[{"product_id":1,"quantity":1,"price":0.01}],"total":0.01}')
ORDER_TOTAL=$(echo "$RESULT" | grep -oE '"total":[0-9.]+' | head -1 | cut -d: -f2)
if echo "$ORDER_TOTAL" | grep -qE '^[0-9]{3,}'; then
  check 2 "Price Manipulation" "PASS"
elif [ -z "$ORDER_TOTAL" ]; then
  check 2 "Price Manipulation" "PASS"
else
  check 2 "Price Manipulation" "FAIL"
fi

# ── V3: IDOR Users ──────────────────────────────────────────────────────────
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$API/api/users/1" \
  -H "Authorization: Bearer $TOKEN")
if [ "$STATUS" = "403" ] || [ "$STATUS" = "401" ]; then
  check 3 "IDOR Users" "PASS"
else
  check 3 "IDOR Users" "FAIL"
fi

# ── V4: Mass Assignment ─────────────────────────────────────────────────────
RAND=$RANDOM
RESULT=$(curl -s -X POST "$API/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"nome\":\"Test${RAND}\",\"email\":\"test${RAND}@test.com\",\"password\":\"test123\",\"role\":\"admin\"}")
ROLE=$(echo "$RESULT" | grep -oE '"role":"[^"]+"' | cut -d'"' -f4)
if [ "$ROLE" = "user" ] || [ -z "$ROLE" ]; then
  check 4 "Mass Assignment" "PASS"
else
  check 4 "Mass Assignment" "FAIL"
fi

# ── V5: Stored XSS ──────────────────────────────────────────────────────────
RAND=$RANDOM
RESULT=$(curl -s -X POST "$API/api/products/1/reviews" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"text\":\"<script>alert(${RAND})</script>\",\"rating\":5}")
STORED_TEXT=$(echo "$RESULT" | grep -oE '"text":"[^"]+"' | cut -d'"' -f4)
if echo "$STORED_TEXT" | grep -q "<script>"; then
  check 5 "Stored XSS" "FAIL"
else
  check 5 "Stored XSS" "PASS"
fi

# ── V6: Error Disclosure + Path Traversal ────────────────────────────────────
FORGOT_RESULT=$(curl -s -X POST "$API/api/auth/forgot-password" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@secureshop.com"}')
HAS_STACK=$(echo "$FORGOT_RESULT" | grep -c "stack")

TRAVERSAL_BODY=$(curl -s "$API/api/export/..%2Fpackage.json" \
  -H "Authorization: Bearer $TOKEN")
HAS_CONTENT=$(echo "$TRAVERSAL_BODY" | grep -c "secureshop-api")

if [ "$HAS_STACK" = "0" ] && [ "$HAS_CONTENT" = "0" ]; then
  check 6 "Error Disclosure + Path Traversal" "PASS"
else
  check 6 "Error Disclosure + Path Traversal" "FAIL"
fi

# ── V7: CORS Wildcard ───────────────────────────────────────────────────────
CORS_HEADER=$(curl -s -I "$API/api/products" | grep -i "access-control-allow-origin" | tr -d '\r')
if echo "$CORS_HEADER" | grep -q "\*"; then
  check 7 "CORS Wildcard" "FAIL"
else
  check 7 "CORS Wildcard" "PASS"
fi

# ── V8: SQL Injection Search ────────────────────────────────────────────────
RESULT=$(curl -s "$API/api/products/search?q=a'%20UNION%20SELECT%20id,email,password,role,nome,created_at%20FROM%20users--")
HAS_ADMIN=$(echo "$RESULT" | grep -c "admin@secureshop.com")
if [ "$HAS_ADMIN" = "0" ]; then
  check 8 "SQL Injection Search" "PASS"
else
  check 8 "SQL Injection Search" "FAIL"
fi

# ── Resultado ────────────────────────────────────────────────────────────────
echo -e "${BOLD}╠══════════════════════════════════════════════════════════╣${NC}"
if [ "$PASS" -eq "$TOTAL" ]; then
  echo -e "${BOLD}║${NC}  Resultado: ${GREEN}${PASS}/${TOTAL} PASS${NC}                                    ${BOLD}║${NC}"
else
  echo -e "${BOLD}║${NC}  Resultado: ${GREEN}${PASS} PASS${NC} / ${RED}${FAIL} FAIL${NC}                              ${BOLD}║${NC}"
fi
echo -e "${BOLD}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""
