# MOTD

Simple "message of the day" system where authenticated users can submit messages that will be displayed to everyone the following day.

Developed by Colin Stefani and Simão Romano Schindler, as part of the teaching unit
"Développement d'applications internet (DAI)" at HEIG-VD.

## Features

### User Management
- **User Registration** (`POST /auth/register`) - Create a new account freely without authentication,
- **User Authentication** (`POST /auth/login`) - Secure login with JWT token generation,
- **Session Management** (`POST /auth/logout`) - Safe logout and token invalidation,
- **Account Deletion** (`POST /auth/delete`) - Remove user account and associated data.

### Message of the Day (MOTD) Management
- **Public Viewing** (`GET /posts`) - Browse all MOTDs without authentication required,
- **Create MOTD** (`POST /posts`) - Authenticated users can submit new messages,
- **Update MOTD** (`PUT /posts`) - Authors can edit their own messages,
- **Delete MOTD** (`DELETE /posts`) - Authors can remove their own messages.

### Security
- JWT-based authentication for protected endpoints,
- Authorization checks ensuring users can only modify their own content.

---

## Installation

### Prerequisites
- Java 21+

### Clone the repository
```bash
git clone git@github.com:SchindlerSimao/MOTD.git
```
### Build

---

## Usage
