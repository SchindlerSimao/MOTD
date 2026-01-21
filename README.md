# MOTD

simple "message of the day" system where authenticated users can submit messages that will be displayed to everyone the following day.

developed by Colin Stefani and Simão Romano Schindler, as part of the teaching unit "Développement d'applications internet (DAI)" at HEIG-VD.

## features

### user management
- `POST /auth/register` - create a new account freely without authentication
  - **request:** `{"username": "string", "password": "string"}`
  - **response:** `{"id": number, "username": "string", "createdAt": "timestamp"}`
  - **status codes:** 201 (created), 400 (missing credentials), 409 (username exists)

- `POST /auth/login` - secure login with jwt token generation
  - **request:** `{"username": "string", "password": "string"}`
  - **response:** `{"token": "jwt_string"}`
  - **status codes:** 200 (ok), 400 (missing credentials), 401 (invalid credentials)

- `POST /auth/logout` - safe logout and token invalidation
  - **requires:** bearer token in authorization header
  - **response:** `{"message": "logged.out"}`
  - **status codes:** 200 (ok), 401 (missing/invalid token)

- `DELETE /auth/delete` - remove user account and associated data
  - **requires:** bearer token in authorization header
  - **response:** no content
  - **status codes:** 204 (no content), 401 (unauthorized)

### message of the day (motd) management
- `GET /posts` - browse all motds without authentication required
  - **response:** array of posts with id, authorId, content, createdAt, displayAt
  - **status codes:** 200 (ok)

- `POST /posts` - authenticated users can submit new messages
  - **requires:** bearer token in authorization header
  - **request:** `{"content": "string"}`
  - **response:** `{"id": number, "content": "string", "authorId": number}`
  - **status codes:** 201 (created), 400 (empty content), 401 (unauthorized)

- `PUT /posts/{id}` - authors can edit their own messages
  - **requires:** bearer token in authorization header
  - **request:** `{"content": "string"}`
  - **response:** `{"id": number, "content": "string"}`
  - **status codes:** 200 (ok), 400 (empty content), 401 (unauthorized), 403 (forbidden), 404 (not found)

- `DELETE /posts/{id}` - authors can remove their own messages
  - **requires:** bearer token in authorization header
  - **response:** no content
  - **status codes:** 204 (no content), 401 (unauthorized), 403 (forbidden), 404 (not found)

### security
- jwt-based authentication for protected endpoints
- authorization checks ensuring users can only modify their own content
- token revocation on logout to prevent reuse of expired sessions

---

## installation

### prerequisites
- java 21+
- docker and docker-compose (for containerized deployment)

### clone the repository
```bash
git clone git@github.com:SchindlerSimao/MOTD.git
cd MOTD
```

### build

build the project with maven:
```bash
mvn clean package
```

---

## usage

### running with docker compose

start all services (database, migrations, and application):
```bash
docker-compose up --build
```

the api will be available at `http://localhost:7000`

### environment variables

the following environment variables can be configured:

- `DB_HOST` - database host (default: `db`)
- `DB_PORT` - database port (default: `5432`)
- `DB_NAME` - database name (default: `motd`)
- `DB_USER` - database user (default: `motd`)
- `DB_PASSWORD` - database password (default: `motd`)
- `JWT_SECRET` - secret key for jwt token signing (default: `change-me-in-prod`)

### api examples

here's a complete workflow demonstrating how to use the motd api:

#### 1. register a new user

```bash
curl -X POST http://localhost:7000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```

**response (201 created):**
```json
{
  "id": 1,
  "username": "alice",
  "createdAt": "2026-01-21T14:30:00Z"
}
```

#### 2. login to get jwt token

```bash
curl -X POST http://localhost:7000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```

**response (200 ok):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

save the token for subsequent authenticated requests:
```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 3. create a new post (authenticated)

```bash
curl -X POST http://localhost:7000/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"content": "hello world! this is my first message of the day."}'
```

**response (201 created):**
```json
{
  "id": 1,
  "content": "hello world! this is my first message of the day.",
  "authorId": 1
}
```

#### 4. get all posts (no authentication required)

```bash
curl http://localhost:7000/posts
```

**response (200 ok):**
```json
[
  {
    "id": 1,
    "authorId": 1,
    "content": "hello world! this is my first message of the day.",
    "createdAt": "2026-01-21T14:35:00Z",
    "displayAt": "2026-01-22T00:00:00Z"
  }
]
```

#### 5. update your own post (authenticated)

```bash
curl -X PUT http://localhost:7000/posts/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"content": "updated message content!"}'
```

**response (200 ok):**
```json
{
  "id": 1,
  "content": "updated message content!"
}
```

#### 6. delete your own post (authenticated)

```bash
curl -X DELETE http://localhost:7000/posts/1 \
  -H "Authorization: Bearer $TOKEN"
```

**response (204 no content)**

#### 7. logout (authenticated)

```bash
curl -X POST http://localhost:7000/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

**response (200 ok):**
```json
{
  "message": "logged.out"
}
```

#### 8. delete user account (authenticated)

```bash
curl -X DELETE http://localhost:7000/auth/delete \
  -H "Authorization: Bearer $TOKEN"
```

**response (204 no content)**

---

parts of this project were developed with ai assistance.
