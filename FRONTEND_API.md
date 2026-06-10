# API-контракт для фронтенда (art-backend-ai)

Базовый URL: `http://<host>:8080` (локально `http://localhost:8080`).
Все ответы — JSON. **Авторизация обязательна** для всех `/api/v1/art/**` (Bearer JWT).

> CORS: сервер разрешает источники из `CORS_ALLOWED_ORIGINS`. Укажите там URL фронтенда,
> иначе браузер заблокирует запросы.

---

## 1. Авторизация (JWT)

Эндпоинты `/api/v1/auth/**` — **публичные**.

### Регистрация
`POST /api/v1/auth/register`
```json
{ "username": "ivan", "password": "secret123" }
```
→ `200 OK`
```json
{ "token": "eyJhbGci...", "username": "ivan" }
```
Ошибки: `409` — username занят; `400` — невалидные данные (username 3–50, password 6–100 символов).

### Вход
`POST /api/v1/auth/login` — тело то же.
→ `200` `{ "token": "...", "username": "..." }`. Ошибка `401` — неверный логин/пароль.

### Использование токена
Сохраните `token` (например в `localStorage`) и прикладывайте ко **всем** запросам к `/api/v1/art/**`:
```
Authorization: Bearer <token>
```
Токен живёт 24 часа. На `401` — выкиньте токен и попросите войти заново.

---

## 2. Анализ изображения

### Запустить анализ
`POST /api/v1/art/analyze` — `multipart/form-data`, поле **`file`** (изображение, < 10 МБ).
Заголовок `Authorization: Bearer ...`. **Content-Type не ставьте вручную** — браузер сам.
→ `202 Accepted`
```json
{ "id": "f47ac10b-...", "status": "PROCESSING", ... }
```
Берём `id`.

### Опрашивать статус
`GET /api/v1/art/tasks/{id}` (с токеном). Опрашивайте раз в ~1000 мс, пока `status` не станет
`COMPLETED` или `FAILED`.
```json
{
  "id": "f47ac10b-...",
  "status": "COMPLETED",
  "imageS3Url": "http://.../uploads/...",     // presigned, можно сразу в <img src>
  "palette": ["#0a0a0a", "#f5f5f5", ...],      // 5 HEX
  "tags": ["Лаконичность", ...],
  "styleBreakdown": [
    { "style": "Минимализм", "prob": "85.4%", "val": 85.4 },
    ...                                          // топ-5, отсортировано по убыванию
  ],
  "matches": [
    {
      "artwork": { "id": 123, "artist": "...", "title": "...", "style": "...",
                   "imageS3Url": "http://.../dataset/..." },  // presigned
      "similarityScore": 0.91,
      "similarityPercentage": "91.0%"
    }
    // первые 6
  ],
  "createdAt": "2026-06-10T12:00:00"
}
```
- `status: "FAILED"` → анализ не удался, прекратите опрос и покажите ошибку.
- `styleBreakdown[0]` — самый вероятный стиль. Метки стилей приходят **на русском**.
- `imageS3Url` и `matches[].artwork.imageS3Url` — готовые presigned-ссылки, ставьте прямо в `<img>`.

### Догрузить ещё похожие (пагинация)
`GET /api/v1/art/tasks/{id}/more?limit=6&offset=6` (с токеном)
→ массив объектов как `matches[]` выше. Пустой массив / меньше `limit` — больше нет.

### История пользователя
`GET /api/v1/art/history` (с токеном) → список задач текущего пользователя (новые сверху).

---

## 3. Минимальный поток (псевдокод)
```
token = POST /auth/login | /auth/register
{id} = POST /art/analyze  (file, Bearer token)  -> 202
loop каждые 1000мс:
    t = GET /art/tasks/{id}  (Bearer token)
    if t.status == COMPLETED: render(t); break
    if t.status == FAILED:    showError();  break
```

> Текущий демо-фронт в репозитории `art-frontend/` уже делает это (см. `src/services/api.ts`),
> но логинит фиксированного демо-пользователя. В новом фронте сделайте нормальные экраны
> регистрации/входа и храните токен пользователя.
