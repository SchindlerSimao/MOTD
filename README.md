# MOTD

simple "message of the day" system where authenticated users can submit messages that will be displayed to everyone the following day.

developed by Colin Stefani and Simão Romano Schindler, as part of the teaching unit "Développement d'applications internet (DAI)" at HEIG-VD.

## features

### user management
- `POST /auth/register` - create a new account freely without authentication
- `POST /auth/login` - secure login with jwt token generation
- `POST /auth/logout` - safe logout and token invalidation
- `DELETE /auth/delete` - remove user account and associated data

### message of the day (motd) management
- `GET /posts` - browse all motds without authentication required
- `POST /posts` - authenticated users can submit new messages
- `PUT /posts/{id}` - authors can edit their own messages
- `DELETE /posts/{id}` - authors can remove their own messages

### security
- jwt-based authentication for protected endpoints
- authorization checks ensuring users can only modify their own content

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

**register a new user:**
```bash
curl -X POST http://localhost:7000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```

**login:**
```bash
curl -X POST http://localhost:7000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```

**create a post (requires authentication):**
```bash
curl -X POST http://localhost:7000/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"content": "hello world!"}'
```

**get all posts:**
```bash
curl http://localhost:7000/posts
```

---

parts of this project were developed with ai assistance.
