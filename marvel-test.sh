#!/usr/bin/env bash
set -e
HOST=http://localhost:8081

echo "=== 1) Registro 3 usuarios Marvel ==="
for u in tony natasha bruce; do
  echo -n "$u: "
  curl -s -X POST $HOST/api/auth/register \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$u\",\"password\":\"secret123\"}" | head -c 200; echo
done

echo
echo "=== 2) Login ==="
TOKEN_TONY=$(curl -s -X POST $HOST/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"tony","password":"secret123"}' | jq -r .token)
TOKEN_NATASHA=$(curl -s -X POST $HOST/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"natasha","password":"secret123"}' | jq -r .token)
TOKEN_BRUCE=$(curl -s -X POST $HOST/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"bruce","password":"secret123"}' | jq -r .token)
echo "tony token: ${TOKEN_TONY:0:8}..."
echo "natasha token: ${TOKEN_NATASHA:0:8}..."
echo "bruce token: ${TOKEN_BRUCE:0:8}..."

echo
echo "=== 3) Discusiones Marvel ==="
DISC_TONY=$(curl -s -X POST $HOST/api/discussions -H "Authorization: Bearer $TOKEN_TONY" -H "Content-Type: application/json" \
  -d '{"title":"Iron Man: Armor Wars","content":"¿Cuál es el mejor traje de Tony? Mark XLII vs Hulkbuster vs Bleeding Edge"}' | jq -r .id)
DISC_NATASHA=$(curl -s -X POST $HOST/api/discussions -H "Authorization: Bearer $TOKEN_NATASHA" -H "Content-Type: application/json" \
  -d '{"title":"Black Widow: Red Room","content":"Teorías sobre Yelena y el pasado de Natasha"}' | jq -r .id)
DISC_BRUCE=$(curl -s -X POST $HOST/api/discussions -H "Authorization: Bearer $TOKEN_BRUCE" -H "Content-Type: application/json" \
  -d '{"title":"Hulk: Gamma Lab","content":"Banner vs Hulk, ¿quién controla al monstruo?"}' | jq -r .id)
echo "DISC_TONY=$DISC_TONY"
echo "DISC_NATASHA=$DISC_NATASHA"
echo "DISC_BRUCE=$DISC_BRUCE"

echo
echo "=== 4) Interacción cruzada en discusión de Tony (maxDepth=3) ==="
C1=$(curl -s -X POST $HOST/api/discussions/$DISC_TONY/comments -H "Authorization: Bearer $TOKEN_NATASHA" -H "Content-Type: application/json" \
  -d '{"content":"Natasha: El Mark XLII está infravalorado, muy ágil."}' | jq -r .id)
echo "C1 natasha -> tony: $C1 (nivel 1)"

C2=$(curl -s -X POST $HOST/api/discussions/$DISC_TONY/comments -H "Authorization: Bearer $TOKEN_BRUCE" -H "Content-Type: application/json" \
  -d "{\"content\":\"Bruce: Yo prefiero el Hulkbuster, diseñado para contenerme.\",\"parentId\":\"$C1\"}" | jq -r .id)
echo "C2 bruce -> C1: $C2 (nivel 2)"

C3=$(curl -s -X POST $HOST/api/discussions/$DISC_TONY/comments -H "Authorization: Bearer $TOKEN_TONY" -H "Content-Type: application/json" \
  -d "{\"content\":\"Tony: Hulkbuster solo para emergencias, Bruce. Bleeding Edge es el futuro.\",\"parentId\":\"$C2\"}" | jq -r .id)
echo "C3 tony -> C2: $C3 (nivel 3 OK)"

echo "Intento nivel 4 (debe fallar 422)..."
HTTP_CODE=$(curl -s -o /tmp/resp_level4 -w "%{http_code}" -X POST $HOST/api/discussions/$DISC_TONY/comments \
  -H "Authorization: Bearer $TOKEN_TONY" -H "Content-Type: application/json" \
  -d "{\"content\":\"Nivel 4 bloqueado\",\"parentId\":\"$C3\"}")
echo "HTTP $HTTP_CODE"
cat /tmp/resp_level4 | jq . 2>/dev/null || cat /tmp/resp_level4

echo
echo "=== 5) Tony pasa a ilimitado ==="
curl -s -X PATCH $HOST/api/users/me/settings -H "Authorization: Bearer $TOKEN_TONY" -H "Content-Type: application/json" \
  -d '{"maxReplyDepth":null}' | jq .
C4=$(curl -s -X POST $HOST/api/discussions/$DISC_TONY/comments -H "Authorization: Bearer $TOKEN_TONY" -H "Content-Type: application/json" \
  -d "{\"content\":\"Nivel 4 ahora sí (ilimitado). La nanotech no tiene límites.\",\"parentId\":\"$C3\"}" | jq -r .id)
echo "C4 (nivel 4 unlimited): $C4"
C5=$(curl -s -X POST $HOST/api/discussions/$DISC_TONY/comments -H "Authorization: Bearer $TOKEN_BRUCE" -H "Content-Type: application/json" \
  -d "{\"content\":\"Nivel 5 ilimitado: ¡Hasta el infinito!\",\"parentId\":\"$C4\"}" | jq -r .id)
echo "C5 (nivel 5 unlimited): $C5"

echo
echo "=== 6) Interacciones en otras discusiones ==="
# Comentario de Tony en la discusión de Natasha
CN1=$(curl -s -X POST $HOST/api/discussions/$DISC_NATASHA/comments -H "Authorization: Bearer $TOKEN_TONY" -H "Content-Type: application/json" \
  -d '{"content":"Tony: ¿Yelena tomará el manto?"}' | jq -r .id)
echo "Tony comenta en Black Widow: $CN1"
# Reply de Bruce
CN2=$(curl -s -X POST $HOST/api/discussions/$DISC_NATASHA/comments -H "Authorization: Bearer $TOKEN_BRUCE" -H "Content-Type: application/json" \
  -d "{\"content\":\"Bruce: Cuidado con el suero.\",\"parentId\":\"$CN1\"}" | jq -r .id)
echo "Bruce reply: $CN2"

# Comentario de Natasha en la discusión de Bruce
CB1=$(curl -s -X POST $HOST/api/discussions/$DISC_BRUCE/comments -H "Authorization: Bearer $TOKEN_NATASHA" -H "Content-Type: application/json" \
  -d '{"content":"Natasha: Banner, ¿cómo controlas la transformación?"}' | jq -r .id)
echo "Natasha en Hulk: $CB1"

echo
echo "=== 7) Verificación árbol y listados ==="
echo "--- GET /api/discussions/$DISC_TONY (árbol) ---"
curl -s $HOST/api/discussions/$DISC_TONY -H "Authorization: Bearer $TOKEN_TONY" | jq '.comments'

echo "--- GET /api/discussions (commentCount) ---"
curl -s $HOST/api/discussions -H "Authorization: Bearer $TOKEN_TONY" | jq '.[].title, .[].commentCount'

echo "--- GET /api/users/me/discussions (Tony) ---"
curl -s $HOST/api/users/me/discussions -H "Authorization: Bearer $TOKEN_TONY" | jq length
echo "--- GET /api/users/me/settings (Tony, debe ser null) ---"
curl -s $HOST/api/users/me/settings -H "Authorization: Bearer $TOKEN_TONY" | jq .

echo
echo "=== Datos persistidos ==="
echo "users.json:"; cat api/data/users.json | jq '.[] | {username, maxReplyDepth}' 2>/dev/null || cat api/data/users.json
echo "discussions.json count:"; cat api/data/discussions.json | jq length 2>/dev/null || echo "no jq"
echo "comments.json count:"; cat api/data/comments.json | jq length 2>/dev/null || echo "no jq"

echo
echo "=== FIN Marvel test ==="
