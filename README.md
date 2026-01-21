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

#### validation caching model
the api implements http conditional requests for optimized bandwidth and concurrency control:

**conditional GET (`If-Modified-Since`)**:
- `GET /posts` returns a `Last-Modified` header with the timestamp of the last modification
- clients can send `If-Modified-Since` header with subsequent requests
- if content hasn't changed, server returns `304 Not Modified` with no body
- reduces bandwidth and improves performance

**optimistic concurrency control (`If-Unmodified-Since`)**:
- `PUT /posts/{id}` and `DELETE /posts/{id}` accept `If-Unmodified-Since` header
- if the resource was modified after the provided timestamp, server returns `412 Precondition Failed`
- prevents lost updates when multiple clients edit the same post
- implemented using `ConcurrentHashMap<String, Instant>` to track modification times

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
cd app
docker compose up
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

Output: 
```json
{
  "username": "alice",
  "id": 9,
  "createdAt": "2026-01-21T18:54:51.171834299Z"
}
```

**login:**
```bash
curl -X POST https://motd.cstef.dev/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```
Output:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5IiwidXNlcm5hbWUiOiJhbGljZSIsImlhdCI6MTc2OTAyMTcxOCwiZXhwIjoxNzY5MTA4MTE4LCJqdGkiOiJiZmQzM2IyNC03NWFlLTRlN2EtYjgwMi04OTZiYjhjNjQ0NTYifQ.uhhepJ6SNjT7vRjXWHwxAg7QLgY2thGHMyoy1RyV_4E"
}
```
**create a post (requires authentication):**
```bash
curl -X POST https://motd.cstef.dev/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"content": "hello world!"}'
```
Output:
```json
{
  "id": 45,
  "authorId": 9,
  "content": "ça marche..."
}
```

**get all posts:**
```bash
curl https://motd.cstef.dev/posts
```
Output:
```json
[
  {
    "createdAt": "2026-01-21T18:56:21.847995Z",
    "displayAt": "2026-01-22",
    "id": 45,
    "authorId": 9,
    "content": "ça marche..."
  },
  {
    "createdAt": "2026-01-21T18:56:11.427983Z",
    "displayAt": "2026-01-22",
    "id": 44,
    "authorId": 9,
    "content": "ça marche... plus ou moins"
  }
]
```

**swagger ui:**

the swagger ui is available at `https://motd.cstef.dev`
![Swagger UI](assets/swagger_ui.png)

**traefik dashboard:**

the traefik dashboard is enabled for monitoring and debugging. it's accessible at `https://motd.cstef.dev` on port 8080 (configure domain/authentication as needed for production).


## deployment

### vm creation

we used an azure vm to host our web application. we followed the steps described in the
[course material](https://github.com/heig-vd-dai-course/heig-vd-dai-course/blob/main/11.03-ssh-and-scp/01-course-material/README.md),
except we used different resource and vm names.

the teaching staff public key
`ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIF5deyMbQaRaiO4ojymkCoWBtwPyG8Y+4BbLQsb413KC heig-vd-dai-course`
was added to the vm for ssh access.

### dns configuration

the domain `motd.cstef.dev` is managed via cloudflare dns.

```
$ dig motd.cstef.dev +noall +answer
motd.cstef.dev.		300	IN	A	20.251.197.5
```

- **type:** A record pointing to the azure vm public ip
- **ttl:** 300 seconds (5 minutes)
- **nameservers:** cloudflare (`ignat.ns.cloudflare.com`, `tegan.ns.cloudflare.com`)

### deployment steps

1. **install docker on the vm:**
   ```bash
   # follow official docker installation for ubuntu
   # add user to docker group to run without sudo
   sudo usermod -aG docker $USER
   ```

2. **set up traefik reverse proxy:**
   ```bash
   # create traefik directory and network
   docker network create traefik-public
   
   # start traefik with docker-compose.traefik.yml
   docker compose -f docker-compose.traefik.yml up -d
   ```

3. **deploy the application:**
   ```bash
   # copy docker-compose.yml to the vm
   # configure environment variables (JWT_SECRET, etc.)
   
   # start the application stack (database, migrations, app)
   docker compose up -d
   ```

4. **verify deployment:**
   ```bash
   # check running containers
   docker ps
   
   # check application logs
   docker compose logs -f app
   
   # test the api
   curl https://motd.cstef.dev/posts
   ```

the application uses github container registry (ghcr.io) for docker images. images are automatically built and published via github actions on push to master.


---

parts of this project were developed with ai assistance.
