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

### caching
- `GET /posts` responses are cached in-memory using caffeine
- cache expires after 60 seconds or is invalidated on create/update/delete

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
curl -X POST https://motd.cstef.dev/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```

**login:**
```bash
curl -X POST https://motd.cstef.dev/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```

**create a post (requires authentication):**
```bash
curl -X POST https://motd.cstef.dev/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"content": "hello world!"}'
```

**get all posts:**
```bash
curl https://motd.cstef.dev/posts
```

**swagger ui:**

the swagger ui is available at `https://motd.cstef.dev`


## vm creation

we used an azure vm to host our web application. we followed the steps described in the
[course material](https://github.com/heig-vd-dai-course/heig-vd-dai-course/blob/main/11.03-ssh-and-scp/01-course-material/README.md),
except we used different resource and vm names.

the teaching staff public key
`ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIF5deyMbQaRaiO4ojymkCoWBtwPyG8Y+4BbLQsb413KC heig-vd-dai-course`
was added to the vm for ssh access.

## dns configuration

the domain `motd.cstef.dev` is managed via cloudflare dns.

```
$ dig motd.cstef.dev +noall +answer
motd.cstef.dev.		300	IN	A	20.251.197.5
```

- **type:** A record pointing to the azure vm public ip
- **ttl:** 300 seconds (5 minutes)
- **nameservers:** cloudflare (`ignat.ns.cloudflare.com`, `tegan.ns.cloudflare.com`)


---

parts of this project were developed with ai assistance.
