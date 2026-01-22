---
title: MOTD - Message of the Day API
sub_title: Dans le cadre de l'unité « Développement d'applications internet »
author: Colin Stefani & Simão Romano Schindler
---

# Table des matières

- API
- VM
- DNS
- Démo
- Questions

<!-- end_slide -->

# API

![image:width:100%](assets/swagger_ui.png)

<!-- end_slide -->

# VM
![VM provider](assets/azure_logo.png)

## Configuration tirée du support de cours.
- VM Linux Ubuntu 24.04,
- Standard B2ts v2: 2 vCPU, 1 Gio RAM,
- Ports 22, 80, 443 ouverts.

<!-- end_slide -->

# DNS

![DNS provider](assets/cloudflare_logo.png)

## Configuration
- **Domaine:** `motd.cstef.dev`
- **Type:** A record → `20.251.197.5` (IP publique Azure)
- **TTL:** 300 secondes (5 minutes)

```bash +exec
dig motd.cstef.dev +noall +answer
```

<!-- end_slide -->

# Démo
===

## Enregistrement d'un utilisateur

```bash +exec
curl -s -X POST https://motd.cstef.dev/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}' | jq -C
```

<!-- end_slide -->

## Connexion

```bash +exec
curl -s -X POST https://motd.cstef.dev/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}' | jq -r '.token' > /tmp/token
echo "Token: $(cat /tmp/token)"
```

<!-- end_slide -->

## Création d'un post

```bash +exec
curl -s -X POST https://motd.cstef.dev/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $(cat /tmp/token)" \
  -d '{"content": "hello world!"}' | tee >(jq -r '.id' > /tmp/post_id) | jq -C
```
<!-- end_slide -->

## Modification d'un post

```bash +exec
curl -s -X PUT https://motd.cstef.dev/posts/$(cat /tmp/post_id) \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $(cat /tmp/token)" \
  -d '{"content": "hello modified world!"}' | jq -C
```
<!-- end_slide -->

## Récupération des posts

```bash +exec
curl -s https://motd.cstef.dev/posts | jq -C '.[:3]'
```
<!-- end_slide -->

## Suppression d'un post

```bash +exec
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X DELETE https://motd.cstef.dev/posts/$(cat /tmp/post_id) \
  -H "Authorization: Bearer $(cat /tmp/token)"
```
<!-- end_slide -->

# Améliorations

- Révocation des tokens JWT (Sécurité)
- DELETE to `/auth/delete`
- Frontend?
- Redondances dans le code

<!-- end_slide -->

# Difficultés

- Javalin
- Documentation
- Configuration HTTPS Traefik + duckdns.org
- Mockito

<!-- end_slide -->

# Achievements

- Stack complet: Flyway, Mockito, OpenApi
- Couverture des UT
- Architecture globale du projet minimale et modulaire
- CI/CD Github Actions w/ caching
- Coordination d'équipe avec GitHub, rythme

<!-- end_slide -->

<!-- jump_to_middle -->
Questions? \o
===
