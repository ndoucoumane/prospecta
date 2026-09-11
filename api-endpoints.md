# 🚀 Prospecta Backend — Guide d'Intégration API & Endpoints

Ce document fournit la référence complète des endpoints de l'API REST de **Prospecta** à destination de l'équipe **Frontend**.
Chaque endpoint est documenté avec :
- Sa méthode HTTP et son chemin absolu
- Les paramètres de requête (*query params*) et variables d'URL (*path variables*)
- Les en-têtes requis (*headers*)
- Les payloads de requête (*Request Body*) avec contraintes de validation
- Les réponses réelles de succès (*HTTP 200/201/204*)
- Les structures de gestion d'erreurs (*HTTP 400/401/403/422/500*)

---

## 📌 Sommaire

1. [Conventions Globales, Authentification & Enveloppes](#-1-conventions-globales-authentification--enveloppes)
2. [Énumérations & Valeurs Métier](#-2-énumérations--valeurs-métier)
3. [Organisations & Espaces de Travail (`/api/v1/organizations`)](#-3-organisations--espaces-de-travail-apiv1organizations)
4. [Profils Utilisateurs & Équipe (`/api/v1/users`)](#-4-profils-utilisateurs--équipe-apiv1users)
5. [Prospects & Leads (`/api/v1/prospects`)](#-5-prospects--leads-apiv1prospects)
6. [Entreprises Cibles (`/api/v1/companies`)](#-6-entreprises-cibles-apiv1companies)
7. [Profil Client Idéal / ICP (`/api/v1/icp`)](#-7-profil-client-idéal--icp-apiv1icp)
8. [Campagnes & Séquences de Prospection (`/api/v1/campaigns`)](#-8-campagnes--séquences-de-prospection-apiv1campaigns)
9. [Boîte de Réception Unifiée & Conversations (`/api/v1/conversations`)](#-9-boîte-de-réception-unifiée--conversations-apiv1conversations)
10. [Pipeline Commercial & Opportunités (`/api/v1/pipeline`)](#-10-pipeline-commercial--opportunités-apiv1pipeline)
11. [Intelligence Artificielle & Copilot B2B (`/api/v1/ai`)](#-11-intelligence-artificielle--copilot-b2b-apiv1ai)
12. [Analytics & Métriques de Conversion (`/api/v1/analytics`)](#-12-analytics--métriques-de-conversion-apiv1analytics)
13. [Webhooks Meta WhatsApp (`/api/v1/webhooks/whatsapp`)](#-13-webhooks-meta-whatsapp-apiv1webhookswhatsapp)
14. [Facturation, Abonnements & Stripe (`/api/v1/billing`)](#-14-facturation-abonnements--stripe-apiv1billing)
15. [Découverte de Prospects & Entreprises — Apollo (`/api/v1/discovery`)](#-15-découverte-de-prospects--entreprises--apollo-apiv1discovery)
16. [Listes de Prospects — Lead Lists (`/api/v1/lead-lists`)](#-16-listes-de-prospects--lead-lists-apiv1lead-lists)
17. [Enrichissement de Coordonnées (`/api/v1/prospects/{id}/enrichment`)](#-17-enrichissement-de-coordonnées-apiv1prospectsidenrichment)
18. [Exemple de Client HTTP TypeScript Recommandé](#-18-exemple-de-client-http-typescript-recommandé)

---

## 🌐 1. Conventions Globales, Authentification & Enveloppes

### URL de Base
- **Local Dev** : `http://localhost:8080`
- **Documentation OpenAPI / Swagger UI** : `http://localhost:8080/swagger-ui.html`
- **Spécification OpenAPI JSON** : `http://localhost:8080/v3/api-docs`

### Authentification & Multi-Tenancy
Toutes les requêtes (sauf les webhooks et les sondes Actuator) doivent inclure le JWT obtenu via **Keycloak** :
```http
Authorization: Bearer <access_token>
Content-Type: application/json
Accept: application/json
X-Trace-Id: <uuid-facultatif>
```
> [!NOTE]
> Le backend résout l'organisation de l'utilisateur (**Multi-tenancy**) automatiquement depuis le token JWT Keycloak (`sub` lié au profil `UserProfile`). Si `X-Trace-Id` n'est pas envoyé par le front, le serveur en génère un et le renvoie systématiquement dans l'en-tête de réponse `X-Trace-Id`.

---

### Format Standard des Réponses : `ApiResponse<T>`
Toutes les réponses de succès (hors `204 No Content`) sont enveloppées dans l'objet suivant :

```json
{
  "data": { ... },
  "meta": {
    "key": "value"
  }
}
```
*(Le champ `meta` est facultatif).*

---

### Format Standard de Pagination : `PageResponse<T>`
Pour tous les endpoints retournant une liste paginée :

**Paramètres de requête acceptés :**
- `page` : Numéro de la page (commence à `0`, par défaut `0`)
- `size` : Nombre d'éléments par page (par défaut `20`)
- `sort` : Champ et direction de tri, ex: `createdAt,desc` ou `name,asc`

**Structure de réponse :**
```json
{
  "data": {
    "items": [
      { ... }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 48,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

---

### Format Standard des Erreurs : `ApiErrorResponse`
En cas d'erreur HTTP (4xx / 5xx), le format renvoyé est rigoureusement identique :

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "traceId": "c4d0e911-30c8-47fb-a790-2e4a8b75f812",
    "details": [
      "email: Invalid email format",
      "companyName: Company name is required"
    ]
  }
}
```

**Codes d'erreur applicatifs fréquents :**
| Code HTTP | `error.code` | Description |
|---|---|---|
| `400 Bad Request` | `BAD_REQUEST` | Requête syntaxiquement incorrecte |
| `401 Unauthorized` | `UNAUTHORIZED` | Token JWT absent, invalide ou expiré |
| `403 Forbidden` | `FORBIDDEN` | Permissions insuffisantes (ex: accès hors de son organisation) |
| `404 Not Found` | `PROSPECT_NOT_FOUND` / `CAMPAIGN_NOT_FOUND` / `COMPANY_NOT_FOUND` | Ressource introuvable |
| `422 Unprocessable` | `VALIDATION_FAILED` | Erreur de validation bean (@Valid sur les champs DTO) |
| `500 Internal Error`| `INTERNAL_SERVER_ERROR`| Erreur serveur non interceptée |

---

## 🏷️ 2. Énumérations & Valeurs Métier

Le front peut typer ses interfaces avec ces valeurs strictes :

- **`UserRole`** : `SUPER_ADMIN`, `ORG_ADMIN`, `SALES_MANAGER`, `SALES_REP`, `VIEWER`
- **`UserStatus`** : `ACTIVE`, `INVITED`, `SUSPENDED`, `DEACTIVATED`
- **`ChannelType`** : `WHATSAPP`, `EMAIL`, `SMS`, `LINKEDIN`
- **`ProspectStatus`** : `NEW`, `CONTACTED`, `QUALIFIED`, `REPLIED`, `MEETING_BOOKED`, `OPPORTUNITY`, `CUSTOMER`, `UNRESPONSIVE`, `OPTED_OUT`, `INVALID`
- **`LeadScoreLevel`** : `HOT`, `WARM`, `COLD`
- **`CampaignStatus`** : `DRAFT`, `SCHEDULED`, `RUNNING`, `PAUSED`, `COMPLETED`, `ARCHIVED`
- **`ConversationStatus`** : `OPEN`, `PENDING`, `REPLIED`, `CLOSED`, `ARCHIVED`
- **`MessageDirection`** : `OUTBOUND`, `INBOUND`
- **`OpportunityStage`** : `NEW`, `QUALIFICATION`, `PROPOSAL`, `NEGOTIATION`, `WON`, `LOST`
- **`OrganizationPlan`** : `FREE`, `STARTER`, `BUSINESS`
- **`SubscriptionStatus`** : `ACTIVE`, `TRIALING`, `PAST_DUE`, `CANCELED`, `UNPAID`, `INCOMPLETE`

---

## 🏢 3. Organisations & Espaces de Travail (`/api/v1/organizations`)

### 3.1. Créer une nouvelle organisation (Espace de travail)
`POST /api/v1/organizations`

#### Request Body
```json
{
  "name": "Dakar Tech Ventures",
  "slug": "dakar-tech-ventures",
  "country": "Sénégal",
  "timezone": "Africa/Dakar",
  "currency": "XOF",
  "industry": "Fintech & SaaS",
  "website": "https://dakartech.sn",
  "phone": "+221771234567",
  "email": "contact@dakartech.sn"
}
```

#### Response `201 Created`
```json
{
  "data": {
    "id": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
    "name": "Dakar Tech Ventures",
    "slug": "dakar-tech-ventures",
    "country": "Sénégal",
    "timezone": "Africa/Dakar",
    "currency": "XOF",
    "industry": "Fintech & SaaS",
    "website": "https://dakartech.sn",
    "phone": "+221771234567",
    "email": "contact@dakartech.sn",
    "status": "ACTIVE",
    "plan": "FREE",
    "createdAt": "2026-09-07T18:00:00Z",
    "updatedAt": "2026-09-07T18:00:00Z"
  }
}
```

---

### 3.2. Récupérer l'organisation active de l'utilisateur connecté
`GET /api/v1/organizations/current`

#### Response `200 OK`
```json
{
  "data": {
    "id": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
    "name": "Dakar Tech Ventures",
    "slug": "dakar-tech-ventures",
    "country": "Sénégal",
    "timezone": "Africa/Dakar",
    "currency": "XOF",
    "industry": "Fintech & SaaS",
    "website": "https://dakartech.sn",
    "phone": "+221771234567",
    "email": "contact@dakartech.sn",
    "status": "ACTIVE",
    "plan": "PROFESSIONAL",
    "createdAt": "2026-09-07T18:00:00Z",
    "updatedAt": "2026-09-07T18:00:00Z"
  }
}
```

---

### 3.3. Récupérer une organisation par son ID
`GET /api/v1/organizations/{id}`

#### Response `200 OK`
*(Même structure que 3.2, avec vérification stricte de tenant isolation).*

---

### 3.4. Mettre à jour les paramètres de l'organisation
`PATCH /api/v1/organizations/{id}`  
*(Rôle requis : `ORG_ADMIN` ou `SUPER_ADMIN`)*

#### Request Body
```json
{
  "name": "Dakar Tech Global",
  "timezone": "Africa/Dakar",
  "currency": "XOF",
  "industry": "Technologies & B2B",
  "website": "https://dakartech.global",
  "phone": "+221338000000",
  "email": "hello@dakartech.global"
}
```

#### Response `200 OK`
*(Retourne l'objet `OrganizationResponse` mis à jour).*

---

## 👤 4. Profils Utilisateurs & Équipe (`/api/v1/users`)

### 4.1. Récupérer le profil connecté
`GET /api/v1/users/me`

#### Response `200 OK`
```json
{
  "data": {
    "id": "u7d8e9a0-b1c2-3d4e-5f6a-7b8c9d0e1f2a",
    "organizationId": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
    "keycloakSubject": "f62629b3-5683-4a1b-944a-d68f237ef811",
    "firstName": "Fatou",
    "lastName": "Diop",
    "fullName": "Fatou Diop",
    "email": "f.diop@dakartech.sn",
    "phone": "+221771112233",
    "jobTitle": "Directrice Commerciale",
    "role": "ORG_ADMIN",
    "status": "ACTIVE",
    "createdAt": "2026-09-07T18:10:00Z",
    "updatedAt": "2026-09-07T18:10:00Z"
  }
}
```

---

### 4.2. Mettre à jour son profil utilisateur
`PATCH /api/v1/users/me`

#### Request Body
```json
{
  "firstName": "Fatou",
  "lastName": "Diop Ndiaye",
  "phone": "+221778889900",
  "jobTitle": "Head of Sales West Africa"
}
```

#### Response `200 OK`
*(Retourne le `UserProfileResponse` mis à jour).*

---

### 4.3. Lister les membres de l'équipe (Espace de travail)
`GET /api/v1/users?page=0&size=20&sort=createdAt,desc`

#### Response `200 OK`
```json
{
  "data": {
    "items": [
      {
        "id": "u7d8e9a0-b1c2-3d4e-5f6a-7b8c9d0e1f2a",
        "organizationId": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
        "keycloakSubject": "f62629b3-5683-4a1b-944a-d68f237ef811",
        "firstName": "Fatou",
        "lastName": "Diop",
        "fullName": "Fatou Diop",
        "email": "f.diop@dakartech.sn",
        "phone": "+221771112233",
        "jobTitle": "Directrice Commerciale",
        "role": "ORG_ADMIN",
        "status": "ACTIVE",
        "createdAt": "2026-09-07T18:10:00Z",
        "updatedAt": "2026-09-07T18:10:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

## 🎯 5. Prospects & Leads (`/api/v1/prospects`)

### 5.1. Créer un prospect
`POST /api/v1/prospects`  
*(Normalise automatiquement les numéros de téléphone pour l'Afrique de l'Ouest et calcule le Lead Score immédiat).*

#### Request Body
```json
{
  "companyId": "c5d6e7f8-a9b0-1c2d-3e4f-5a6b7c8d9e0f",
  "firstName": "Moussa",
  "lastName": "Ndiaye",
  "jobTitle": "Chief Technology Officer",
  "companyName": "Sonatel B2B",
  "companyWebsite": "https://sonatel.sn",
  "email": "moussa.ndiaye@sonatel.sn",
  "phone": "77 123 45 67",
  "whatsappNumber": "77 123 45 67",
  "country": "Sénégal",
  "city": "Dakar",
  "region": "Dakar Plateau",
  "industry": "Télécommunications",
  "companySize": "1000+",
  "linkedinUrl": "https://linkedin.com/in/moussa-ndiaye",
  "source": "LINKEDIN"
}
```

#### Response `201 Created`
```json
{
  "data": {
    "id": "p1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "organizationId": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
    "companyId": "c5d6e7f8-a9b0-1c2d-3e4f-5a6b7c8d9e0f",
    "firstName": "Moussa",
    "lastName": "Ndiaye",
    "fullName": "Moussa Ndiaye",
    "jobTitle": "Chief Technology Officer",
    "companyName": "Sonatel B2B",
    "companyWebsite": "https://sonatel.sn",
    "email": "moussa.ndiaye@sonatel.sn",
    "emailStatus": "VALID",
    "phone": "+221771234567",
    "phoneStatus": "VALID",
    "whatsappNumber": "+221771234567",
    "country": "Sénégal",
    "city": "Dakar",
    "region": "Dakar Plateau",
    "industry": "Télécommunications",
    "companySize": "1000+",
    "linkedinUrl": "https://linkedin.com/in/moussa-ndiaye",
    "source": "LINKEDIN",
    "status": "NEW",
    "leadScore": 85,
    "leadScoreLevel": "HOT",
    "leadScoreReasons": "Matching ICP cible (+30), Décideur technique C-Level (+25), Numéro WhatsApp certifié (+15), Entreprise +500 emp (+15)",
    "createdAt": "2026-09-07T18:30:00Z",
    "updatedAt": "2026-09-07T18:30:00Z"
  }
}
```

---

### 5.2. Consulter un prospect par ID
`GET /api/v1/prospects/{id}`

#### Response `200 OK`
*(Même structure que 5.1)*

---

### 5.3. Rechercher et filtrer les prospects
`GET /api/v1/prospects?status=QUALIFIED&search=Sonatel&page=0&size=20&sort=leadScore,desc`

**Paramètres Query :**
- `status` *(optionnel)* : Filtrer par `ProspectStatus` (`NEW`, `QUALIFIED`, `CONTACTED`, etc.)
- `search` *(optionnel)* : Recherche insensible à la casse sur nom, prénom, email ou entreprise
- `page`, `size`, `sort` : Pagination

#### Response `200 OK`
```json
{
  "data": {
    "items": [
      {
        "id": "p1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "organizationId": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
        "companyId": "c5d6e7f8-a9b0-1c2d-3e4f-5a6b7c8d9e0f",
        "firstName": "Moussa",
        "lastName": "Ndiaye",
        "fullName": "Moussa Ndiaye",
        "jobTitle": "Chief Technology Officer",
        "companyName": "Sonatel B2B",
        "companyWebsite": "https://sonatel.sn",
        "email": "moussa.ndiaye@sonatel.sn",
        "emailStatus": "VALID",
        "phone": "+221771234567",
        "phoneStatus": "VALID",
        "whatsappNumber": "+221771234567",
        "country": "Sénégal",
        "city": "Dakar",
        "region": "Dakar Plateau",
        "industry": "Télécommunications",
        "companySize": "1000+",
        "linkedinUrl": "https://linkedin.com/in/moussa-ndiaye",
        "source": "LINKEDIN",
        "status": "QUALIFIED",
        "leadScore": 85,
        "leadScoreLevel": "HOT",
        "leadScoreReasons": "Matching ICP cible (+30), Décideur technique C-Level (+25)",
        "createdAt": "2026-09-07T18:30:00Z",
        "updatedAt": "2026-09-07T18:45:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 5.4. Mettre à jour un prospect
`PATCH /api/v1/prospects/{id}`

#### Request Body
```json
{
  "jobTitle": "Directeur Général Adjoint",
  "phone": "+221775556677",
  "whatsappNumber": "+221775556677",
  "status": "QUALIFIED",
  "city": "Dakar",
  "industry": "Technologies Financières"
}
```

#### Response `200 OK`
*(Retourne le `ProspectResponse` mis à jour avec re-calcul automatique des indicateurs).*

---

### 5.5. Supprimer un prospect (Droit à l'oubli / RGPD)
`DELETE /api/v1/prospects/{id}`

#### Response `204 No Content`
*(Pas de body).*

---

### 5.6. Recalculer le Lead Score d'un prospect
`POST /api/v1/prospects/{id}/score`

#### Response `200 OK`
```json
{
  "data": {
    "score": 90,
    "level": "HOT",
    "reasons": [
      "Profil décideur correspondant aux critères ICP",
      "Numéro WhatsApp direct vérifié",
      "Secteur à haute valeur ajoutée identifié"
    ]
  }
}
```

---

### 5.7. Importer des prospects par fichier CSV
`POST /api/v1/prospects/import`  
`Content-Type: multipart/form-data`

**Form Data :**
- `file` : Fichier `.csv` (colonnes recommandées : `first_name`, `last_name`, `company_name`, `email`, `phone`, `job_title`, `city`, `industry`).

#### Response `200 OK`
```json
{
  "data": {
    "total": 150,
    "created": 142,
    "duplicates": 6,
    "invalid": 2,
    "errors": [
      "Ligne 45: Format d'email invalide 'test@@bad.com'",
      "Ligne 89: Numéro de téléphone impossible à normaliser 'abc123'"
    ]
  }
}
```

---

## 🏭 6. Entreprises Cibles (`/api/v1/companies`)

### 6.1. Créer une entreprise
`POST /api/v1/companies`

#### Request Body
```json
{
  "name": "Wave Digital Finance",
  "website": "https://wave.com",
  "industry": "Mobile Money & Fintech",
  "description": "Leader du transfert d'argent et paiements mobiles en Afrique de l'Ouest",
  "country": "Sénégal",
  "city": "Dakar",
  "phone": "+221338000000",
  "email": "contact@wave.com",
  "employeeCount": 850,
  "linkedinUrl": "https://linkedin.com/company/wave-mobile-money",
  "source": "PROSPECTION_DIRECTE"
}
```

#### Response `201 Created`
```json
{
  "data": {
    "id": "c5d6e7f8-a9b0-1c2d-3e4f-5a6b7c8d9e0f",
    "organizationId": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
    "name": "Wave Digital Finance",
    "website": "https://wave.com",
    "industry": "Mobile Money & Fintech",
    "description": "Leader du transfert d'argent et paiements mobiles en Afrique de l'Ouest",
    "country": "Sénégal",
    "city": "Dakar",
    "phone": "+221338000000",
    "email": "contact@wave.com",
    "employeeCount": 850,
    "linkedinUrl": "https://linkedin.com/company/wave-mobile-money",
    "source": "PROSPECTION_DIRECTE",
    "aiSummary": null,
    "aiPainPoints": null,
    "aiAnalyzedAt": null,
    "createdAt": "2026-09-07T18:20:00Z",
    "updatedAt": "2026-09-07T18:20:00Z"
  }
}
```

---

### 6.2. Consulter une entreprise par ID
`GET /api/v1/companies/{id}`

#### Response `200 OK`
*(Même structure que 6.1 avec les résultats d'enrichissement IA le cas échéant).*

---

### 6.3. Lister les entreprises
`GET /api/v1/companies?page=0&size=20&sort=name,asc`

#### Response `200 OK`
```json
{
  "data": {
    "items": [
      {
        "id": "c5d6e7f8-a9b0-1c2d-3e4f-5a6b7c8d9e0f",
        "organizationId": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
        "name": "Wave Digital Finance",
        "website": "https://wave.com",
        "industry": "Mobile Money & Fintech",
        "city": "Dakar",
        "country": "Sénégal",
        "employeeCount": 850,
        "createdAt": "2026-09-07T18:20:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 6.4. Mettre à jour une entreprise
`PATCH /api/v1/companies/{id}`

#### Request Body
```json
{
  "employeeCount": 950,
  "description": "Opérateur de monnaie électronique agréé BCEAO"
}
```

#### Response `200 OK`
*(Retourne le `CompanyResponse` mis à jour).*

---

## 🎯 7. Profil Client Idéal / ICP (`/api/v1/icp`)

### 7.1. Créer un profil ICP
`POST /api/v1/icp`

#### Request Body
```json
{
  "name": "Fintech & Banques UEMOA C-Level",
  "description": "Banques commerciales, microfinances et startups Fintech à forte croissance",
  "targetIndustries": "Fintech, Banque, Assurance, Télécoms",
  "targetCities": "Dakar, Abidjan, Bamako, Lomé",
  "targetCountries": "Sénégal, Côte d'Ivoire, Mali, Togo",
  "minEmployees": 50,
  "maxEmployees": 5000,
  "targetJobTitles": "Directeur Général, CEO, CTO, Directeur Commercial, VP Sales, Head of Digital",
  "keywords": "Paiement, API, Facturation, B2B, SaaS",
  "excludedIndustries": "BTP, Restauration rapide"
}
```

#### Response `201 Created`
```json
{
  "data": {
    "id": "i9a8b7c6-d5e4-3f2a-1b0c-9d8e7f6a5b4c",
    "organizationId": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
    "name": "Fintech & Banques UEMOA C-Level",
    "description": "Banques commerciales, microfinances et startups Fintech à forte croissance",
    "targetIndustries": "Fintech, Banque, Assurance, Télécoms",
    "targetCities": "Dakar, Abidjan, Bamako, Lomé",
    "targetCountries": "Sénégal, Côte d'Ivoire, Mali, Togo",
    "minEmployees": 50,
    "maxEmployees": 5000,
    "targetJobTitles": "Directeur Général, CEO, CTO, Directeur Commercial, VP Sales, Head of Digital",
    "keywords": "Paiement, API, Facturation, B2B, SaaS",
    "excludedIndustries": "BTP, Restauration rapide",
    "createdAt": "2026-09-07T18:25:00Z",
    "updatedAt": "2026-09-07T18:25:00Z"
  }
}
```

---

### 7.2. Récupérer l'ICP actif de l'organisation
`GET /api/v1/icp/active`

#### Response `200 OK`
*(Renvoie l'ICP actif le plus récent, utilisé par le moteur de Lead Scoring).*

---

### 7.3. Mettre à jour les critères ICP
`PATCH /api/v1/icp/{id}`

#### Request Body
```json
{
  "minEmployees": 30,
  "keywords": "Paiement, API, Facturation, B2B, IA, Automatisation"
}
```

#### Response `200 OK`
*(Retourne le `IcpResponse` mis à jour).*

---

## 📢 8. Campagnes & Séquences de Prospection (`/api/v1/campaigns`)

### 8.1. Créer une campagne de prospection
`POST /api/v1/campaigns`

#### Request Body
```json
{
  "name": "Campagne Démo SaaS Q4 Dakar",
  "description": "Séquence multicanale WhatsApp + Email pour directeurs commerciaux",
  "channelStrategy": "WHATSAPP_FIRST"
}
```

#### Response `201 Created`
```json
{
  "data": {
    "id": "f8f64d2a-5507-4dbf-a6cd-bb56fae3ae84",
    "organizationId": "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d",
    "name": "Campagne Démo SaaS Q4 Dakar",
    "description": "Séquence multicanale WhatsApp + Email pour directeurs commerciaux",
    "status": "DRAFT",
    "channelStrategy": "WHATSAPP_FIRST",
    "startedAt": null,
    "completedAt": null,
    "steps": [],
    "createdAt": "2026-09-07T18:40:00Z",
    "updatedAt": "2026-09-07T18:40:00Z"
  }
}
```

---

### 8.2. Configurer les étapes (Séquence multicanale)
`POST /api/v1/campaigns/{id}/steps`

#### Request Body
```json
[
  {
    "position": 1,
    "channel": "WHATSAPP",
    "delayMinutes": 0,
    "subjectTemplate": null,
    "contentTemplate": "Bonjour {{firstName}}, j'ai vu vos récentes réalisations chez {{companyName}}. Seriez-vous ouvert à un rapide échange de 10 min sur l'automatisation de vos ventes ?",
    "enabled": true
  },
  {
    "position": 2,
    "channel": "EMAIL",
    "delayMinutes": 2880,
    "subjectTemplate": "Accélération commerciale B2B pour {{companyName}}",
    "contentTemplate": "Bonjour {{firstName}},\n\nJe fais suite à mon message WhatsApp. Nos clients B2B à Dakar ont réduit leur cycle de vente de 40%...",
    "enabled": true
  }
]
```

#### Response `200 OK`
```json
{
  "data": {
    "id": "f8f64d2a-5507-4dbf-a6cd-bb56fae3ae84",
    "name": "Campagne Démo SaaS Q4 Dakar",
    "status": "DRAFT",
    "channelStrategy": "WHATSAPP_FIRST",
    "steps": [
      {
        "id": "s1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "position": 1,
        "channel": "WHATSAPP",
        "delayMinutes": 0,
        "subjectTemplate": null,
        "contentTemplate": "Bonjour {{firstName}}, j'ai vu vos récentes réalisations chez {{companyName}}...",
        "enabled": true
      },
      {
        "id": "s2b3c4d5-e6f7-8a9b-0c1d-2e3f4a5b6c7d",
        "position": 2,
        "channel": "EMAIL",
        "delayMinutes": 2880,
        "subjectTemplate": "Accélération commerciale B2B pour {{companyName}}",
        "contentTemplate": "Bonjour {{firstName}},\n\nJe fais suite...",
        "enabled": true
      }
    ],
    "createdAt": "2026-09-07T18:40:00Z",
    "updatedAt": "2026-09-07T18:45:00Z"
  }
}
```

---

### 8.3. Associer des prospects cibles à la campagne
`POST /api/v1/campaigns/{id}/prospects`

#### Request Body
```json
{
  "prospectIds": [
    "p1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "p2b3c4d5-e6f7-8a9b-0c1d-2e3f4a5b6c7d"
  ]
}
```

#### Response `200 OK`
```json
{
  "data": {
    "addedCount": 2,
    "campaignId": "f8f64d2a-5507-4dbf-a6cd-bb56fae3ae84"
  }
}
```

---

### 8.4. Lancer la campagne
`POST /api/v1/campaigns/{id}/launch`  
*(Passe le statut à `RUNNING` et déclenche immédiatement la première étape pour les cibles).*

#### Response `200 OK`
```json
{
  "data": {
    "id": "f8f64d2a-5507-4dbf-a6cd-bb56fae3ae84",
    "name": "Campagne Démo SaaS Q4 Dakar",
    "status": "RUNNING",
    "startedAt": "2026-09-07T18:50:00Z",
    "steps": [ ... ],
    "updatedAt": "2026-09-07T18:50:00Z"
  }
}
```

---

### 8.5. Mettre en pause la campagne
`POST /api/v1/campaigns/{id}/pause`

#### Response `200 OK`
```json
{
  "data": {
    "id": "f8f64d2a-5507-4dbf-a6cd-bb56fae3ae84",
    "status": "PAUSED"
  }
}
```

---

## 💬 9. Boîte de Réception Unifiée & Conversations (`/api/v1/conversations`)

### 9.1. Lister les conversations de la boîte unifiée
`GET /api/v1/conversations?channel=WHATSAPP&status=OPEN&page=0&size=20&sort=lastMessageAt,desc`

**Paramètres Query :**
- `channel` *(optionnel)* : `WHATSAPP`, `EMAIL`, `SMS`, `LINKEDIN`
- `status` *(optionnel)* : `OPEN`, `PENDING`, `REPLIED`, `CLOSED`, `ARCHIVED`
- `page`, `size`, `sort` : Pagination

#### Response `200 OK`
```json
{
  "data": {
    "items": [
      {
        "id": "conv-1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
        "prospectId": "p1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
        "prospectName": "Moussa Ndiaye",
        "companyName": "Sonatel B2B",
        "channel": "WHATSAPP",
        "lastMessage": "Bonjour, oui cela m'intéresse. Avez-vous une démo disponible jeudi ?",
        "lastMessageAt": "2026-09-07T18:55:00Z",
        "status": "OPEN",
        "assignedTo": "u7d8e9a0-b1c2-3d4e-5f6a-7b8c9d0e1f2a"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 9.2. Consulter le fil de discussion complet
`GET /api/v1/conversations/{id}`

#### Response `200 OK`
```json
{
  "data": {
    "id": "conv-1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "prospectId": "p1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "prospectName": "Moussa Ndiaye",
    "companyName": "Sonatel B2B",
    "channel": "WHATSAPP",
    "status": "OPEN",
    "assignedTo": "u7d8e9a0-b1c2-3d4e-5f6a-7b8c9d0e1f2a",
    "messages": [
      {
        "id": "msg-111",
        "direction": "OUTBOUND",
        "channel": "WHATSAPP",
        "sender": "Prospecta App",
        "recipient": "+221771234567",
        "content": "Bonjour Moussa, j'ai vu vos récentes réalisations chez Sonatel B2B...",
        "sentAt": "2026-09-07T18:50:00Z"
      },
      {
        "id": "msg-222",
        "direction": "INBOUND",
        "channel": "WHATSAPP",
        "sender": "+221771234567",
        "recipient": "Prospecta App",
        "content": "Bonjour, oui cela m'intéresse. Avez-vous une démo disponible jeudi ?",
        "sentAt": "2026-09-07T18:55:00Z"
      }
    ]
  }
}
```

---

### 9.3. Répondre au prospect sur le canal de la conversation
`POST /api/v1/conversations/{id}/messages`

#### Request Body
```json
{
  "content": "Parfait Moussa ! Seriez-vous disponible ce jeudi à 11h00 via Google Meet ?"
}
```

#### Response `200 OK`
```json
{
  "data": {
    "id": "msg-333",
    "direction": "OUTBOUND",
    "channel": "WHATSAPP",
    "sender": "Fatou Diop",
    "recipient": "+221771234567",
    "content": "Parfait Moussa ! Seriez-vous disponible ce jeudi à 11h00 via Google Meet ?",
    "sentAt": "2026-09-07T18:57:00Z"
  }
}
```

---

### 9.4. Assistant IA Copilot : Suggérer une réponse intelligente
`POST /api/v1/conversations/{id}/ai/reply`

#### Response `200 OK`
```json
{
  "data": {
    "suggestedReply": "Bonjour Moussa, avec grand plaisir ! Je vous propose un créneau ce jeudi à 11h ou 15h selon vos préférences. Voici le lien de réservation directe : https://cal.com/prospecta-demo",
    "intent": "DEMANDE_DE_DEMO_COMMERCIALE",
    "sentiment": "POSITIF_TRES_INTERESSE",
    "recommendedNextAction": "Envoyer créneau de démo et créer une Opportunité dans le Pipeline",
    "confidence": 0.94
  }
}
```

---

## 📈 10. Pipeline Commercial & Opportunités (`/api/v1/pipeline`)

### 10.1. Créer une opportunité d'affaires (Deal)
`POST /api/v1/pipeline/opportunities`

#### Request Body
```json
{
  "prospectId": "p1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "companyId": "c5d6e7f8-a9b0-1c2d-3e4f-5a6b7c8d9e0f",
  "assignedTo": "u7d8e9a0-b1c2-3d4e-5f6a-7b8c9d0e1f2a",
  "title": "Sonatel B2B — Déploiement SaaS 50 licences",
  "stage": "QUALIFICATION",
  "estimatedValue": 15000000.00,
  "currency": "XOF",
  "winProbability": 40,
  "expectedCloseDate": "2026-10-31",
  "notes": "Démo programmée pour jeudi 11h avec le CTO et l'équipe commerciale"
}
```

#### Response `201 Created`
```json
{
  "data": {
    "id": "opp-1111-2222-3333-4444",
    "prospectId": "p1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "prospectName": "Moussa Ndiaye",
    "companyId": "c5d6e7f8-a9b0-1c2d-3e4f-5a6b7c8d9e0f",
    "companyName": "Sonatel B2B",
    "assignedTo": "u7d8e9a0-b1c2-3d4e-5f6a-7b8c9d0e1f2a",
    "title": "Sonatel B2B — Déploiement SaaS 50 licences",
    "stage": "QUALIFICATION",
    "estimatedValue": 15000000.00,
    "currency": "XOF",
    "winProbability": 40,
    "expectedCloseDate": "2026-10-31",
    "closedAt": null,
    "lossReason": null,
    "notes": "Démo programmée pour jeudi 11h avec le CTO et l'équipe commerciale",
    "createdAt": "2026-09-07T19:00:00Z",
    "updatedAt": "2026-09-07T19:00:00Z"
  }
}
```

---

### 10.2. Lister les opportunités (Vue liste ou colonnes Kanban)
`GET /api/v1/pipeline/opportunities?stage=QUALIFICATION&page=0&size=20&sort=createdAt,desc`

**Paramètres Query :**
- `stage` *(optionnel)* : `NEW`, `QUALIFICATION`, `PROPOSAL`, `NEGOTIATION`, `WON`, `LOST`
- `assignedTo` *(optionnel)* : UUID du commercial assigné
- `page`, `size`, `sort` : Pagination

#### Response `200 OK`
*(Retourne un `PageResponse<OpportunityResponse>` standard).*

---

### 10.3. Changer l'étape de l'opportunité (Drag & Drop Kanban)
`PATCH /api/v1/pipeline/opportunities/{id}/stage`

#### Request Body
```json
{
  "stage": "WON",
  "lossReason": null
}
```
*(Si `stage` = `LOST`, fournir le motif dans `lossReason`)*

#### Response `200 OK`
*(Retourne l'objet `OpportunityResponse` mis à jour).*

---

### 10.4. Mettre à jour une opportunité
`PUT /api/v1/pipeline/opportunities/{id}`

#### Request Body
```json
{
  "title": "Sonatel B2B — Déploiement SaaS 75 licences",
  "assignedTo": "u7d8e9a0-b1c2-3d4e-5f6a-7b8c9d0e1f2a",
  "estimatedValue": 22500000.00,
  "currency": "XOF",
  "winProbability": 70,
  "expectedCloseDate": "2026-11-15",
  "notes": "Périmètre étendu à la filiale Mali"
}
```

#### Response `200 OK`
*(Retourne le `OpportunityResponse` mis à jour).*

---

### 10.5. Vue d'ensemble du Pipeline & Agrégats financiers
`GET /api/v1/pipeline/overview`

#### Response `200 OK`
```json
{
  "data": {
    "totalOpportunities": 12,
    "totalPipelineValue": 145000000.00,
    "currency": "XOF",
    "stages": [
      { "stage": "NEW", "count": 3, "totalValue": 25000000.00 },
      { "stage": "QUALIFICATION", "count": 4, "totalValue": 45000000.00 },
      { "stage": "PROPOSAL", "count": 2, "totalValue": 30000000.00 },
      { "stage": "NEGOTIATION", "count": 1, "totalValue": 15000000.00 },
      { "stage": "WON", "count": 2, "totalValue": 30000000.00 },
      { "stage": "LOST", "count": 0, "totalValue": 0.00 }
    ]
  }
}
```

---

## 🤖 11. Intelligence Artificielle & Copilot B2B (`/api/v1/ai`)

### 11.1. Analyser l'intelligence web d'une entreprise cible
`POST /api/v1/companies/{id}/ai/analyze`

#### Response `200 OK`
```json
{
  "data": {
    "summary": "Acteur majeur des télécoms au Sénégal développant activement son offre B2B Cloud et Cybersécurité.",
    "industry": "Télécommunications & Solutions Entreprises",
    "painPoints": [
      "Processus de prospection manuels et perte de traçabilité des leads WhatsApp",
      "Cycles de négociation longs sur les grands comptes régionaux"
    ],
    "opportunities": [
      "Digitalisation de la prospection commerciale via WhatsApp Business API certifiée",
      "Scoring automatique des leads entrants pour prioriser les commerciaux"
    ],
    "recommendedApproach": "Positionner Prospecta comme la solution souveraine locale augmentant la conversion WhatsApp sans changer d'outils CRM existants.",
    "confidence": 0.88
  }
}
```

---

### 11.2. Synthétiser un prospect & Angle d'accroche personnalisé
`POST /api/v1/prospects/{id}/ai/summarize`

#### Response `200 OK`
```json
{
  "data": {
    "summary": "Moussa Ndiaye est CTO chez Sonatel B2B, ingénieur chevronné très sensible aux architectures fiables et sécurisées.",
    "keyStrengths": [
      "Décisionnaire technique direct sur les intégrations logicielles",
      "Actif sur les sujets d'API bancaires et télécoms"
    ],
    "suggestedAngle": "Aborder la sécurité des données, l'hébergement conforme aux régulations ouest-africaines et la rapidité d'implémentation via API."
  }
}
```

---

### 11.3. Générer un message commercial multicanal (WhatsApp / Email / SMS)
`POST /api/v1/ai/messages/generate`

#### Request Body
```json
{
  "channel": "WHATSAPP",
  "prospectId": "p1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "offerDescription": "Plateforme Prospecta d'automatisation des ventes B2B adaptée aux entreprises sénégalaises",
  "goal": "Décrocher un call de 15 minutes cette semaine"
}
```

#### Response `200 OK`
```json
{
  "data": {
    "channel": "WHATSAPP",
    "subject": null,
    "body": "Bonjour Moussa,\n\nJ'ai suivi avec attention les innovations récentes de Sonatel B2B sur le segment entreprises. Beaucoup de directeurs tech me partagent la complexité de relier les canaux informels (WhatsApp) à un suivi commercial rigoureux.\n\nNous avons conçu Prospecta précisément pour automatiser ces flux en toute conformité.\n\nSeriez-vous ouvert à une brève démonstration de 10 min ce jeudi ?",
    "callToAction": "Seriez-vous ouvert à un échange de 10 min jeudi ?"
  }
}
```

---

### 11.4. Suivi des quotas IA & Consommation de tokens
`GET /api/v1/ai/usage`

#### Response `200 OK`
```json
{
  "data": {
    "monthlyOperations": 42,
    "monthlyTokens": 38500,
    "quotaLimit": 1000,
    "plan": "PROFESSIONAL"
  }
}
```

---

## 📊 12. Analytics & Métriques de Conversion (`/api/v1/analytics`)

### 12.1. Tableau de bord général des ventes
`GET /api/v1/analytics/overview`

#### Response `200 OK`
```json
{
  "data": {
    "totalProspects": 250,
    "qualifiedProspects": 140,
    "contactedProspects": 110,
    "repliedProspects": 38,
    "meetingBookedProspects": 19,
    "opportunitiesCount": 12,
    "opportunitiesWon": 4,
    "totalWonValue": 55000000.00,
    "totalPipelineValue": 145000000.00,
    "currency": "XOF",
    "replyRate": 34.55,
    "conversionRate": 15.2,
    "activeCampaigns": 3,
    "channelBreakdown": [
      {
        "channel": "WHATSAPP",
        "totalSent": 85,
        "delivered": 83,
        "failed": 2
      },
      {
        "channel": "EMAIL",
        "totalSent": 60,
        "delivered": 58,
        "failed": 2
      }
    ]
  }
}
```

---

### 12.2. Statistiques et entonnoir d'une campagne spécifique
`GET /api/v1/analytics/campaigns/{id}`

#### Response `200 OK`
```json
{
  "data": {
    "campaignId": "f8f64d2a-5507-4dbf-a6cd-bb56fae3ae84",
    "campaignName": "Campagne Démo SaaS Q4 Dakar",
    "status": "RUNNING",
    "targetProspectsCount": 50,
    "pendingCount": 10,
    "activeCount": 20,
    "completedCount": 8,
    "repliedCount": 10,
    "optedOutCount": 1,
    "failedCount": 1,
    "replyRate": 25.0
  }
}
```

---

## 📲 13. Webhooks Meta WhatsApp (`/api/v1/webhooks/whatsapp`)

Ces endpoints sont appelés directement par l'infrastructure Cloud de **Meta WhatsApp Business** :

### 13.1. Vérification du Webhook Meta (Challenge)
`GET /api/v1/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token={token}&hub.challenge={challenge}`

- **Réponse** : Renvoie le code de défi `{challenge}` en texte brut avec code `200 OK`.

---

### 13.2. Réception des messages entrants & accusés de lecture
`POST /api/v1/webhooks/whatsapp`  
`X-Hub-Signature-256: sha256={signature_hmac}`

- Valide la signature HMAC SHA-256 avec `app-secret`
- Traite l'idempotence des événements (`wamid.xxx`)
- Rapproche automatiquement le numéro de l'expéditeur d'un prospect existant
- Met à jour l'inbox unifiée et passe le statut du prospect à `REPLIED` si une campagne était en cours.

---

## 💳 14. Facturation, Abonnements & Stripe (`/api/v1/billing`)

Ce module gère le modèle SaaS Freemium/Payant de Prospecta : affichage de la grille tarifaire (en FCFA / XOF), initiation de paiement sécurisé via **Stripe Checkout**, gestion du moyen de paiement et des factures via le **Stripe Customer Portal**, et synchronisation en temps réel via les webhooks Stripe.

### 14.1. Consulter les formules et tarifs disponibles
`GET /api/v1/billing/plans`

Renvoie la liste des 3 offres d'abonnement (`FREE`, `STARTER`, `BUSINESS`) avec leurs quotas IA, tarifs en XOF (Francs CFA), fonctionnalités incluses, et l'indicateur `isCurrent` pour le plan actuellement actif de l'organisation connectée.

#### Response `200 OK`
```json
{
  "data": [
    {
      "id": "FREE",
      "name": "Plan Gratuit (Découverte)",
      "description": "Idéal pour tester l'automatisation et le scoring de vos prospects",
      "price": 0,
      "currency": "XOF",
      "billingPeriod": "MONTHLY",
      "aiQuota": 100,
      "features": [
        "1 utilisateur commercial",
        "Jusqu'à 100 opérations IA par mois",
        "Scoring automatique des leads UEMOA",
        "Importation CSV jusqu'à 50 prospects"
      ],
      "isCurrent": true
    },
    {
      "id": "STARTER",
      "name": "Plan Starter",
      "description": "Pour les commerciaux indépendants et TPE qui accélèrent leur prospection",
      "price": 29000,
      "currency": "XOF",
      "billingPeriod": "MONTHLY",
      "aiQuota": 1000,
      "features": [
        "Jusqu'à 3 utilisateurs commerciaux",
        "1 000 opérations et résumés IA par mois",
        "Campagnes de prospection multicanales (Email & WhatsApp)",
        "Inbox unifiée et gestion du Pipeline Kanban",
        "Support prioritaire par WhatsApp"
      ],
      "isCurrent": false
    },
    {
      "id": "BUSINESS",
      "name": "Plan Business",
      "description": "Pour les équipes commerciales ambitieuses exigeant le maximum de conversion",
      "price": 79000,
      "currency": "XOF",
      "billingPeriod": "MONTHLY",
      "aiQuota": 5000,
      "features": [
        "Jusqu'à 10 utilisateurs commerciaux",
        "5 000 opérations et copies IA par mois",
        "Génération automatique d'angles de vente IA",
        "IA Copilot Inbox avec suggestions de réponses temps réel",
        "Accès API complet et Webhooks personnalisés",
        "Accompagnement et onboarding dédié"
      ],
      "isCurrent": false
    }
  ]
}
```

---

### 14.2. Consulter l'état de l'abonnement actif de l'organisation
`GET /api/v1/billing/subscription`

#### Response `200 OK`
```json
{
  "data": {
    "organizationId": "c4d0e911-30c8-47fb-a790-2e4a8b75f812",
    "organizationName": "Sonatel B2B Solutions",
    "plan": "STARTER",
    "status": "ACTIVE",
    "stripeCustomerId": "cus_R9wXk...",
    "stripeSubscriptionId": "sub_1Q...",
    "currentPeriodEnd": "2026-10-07T21:00:00Z",
    "monthlyAiQuota": 1000
  }
}
```

---

### 14.3. Initier une session de paiement Stripe Checkout
`POST /api/v1/billing/checkout`  
*(Requiert le rôle `ORG_ADMIN` ou `SUPER_ADMIN`)*

Génère une URL hébergée par Stripe Checkout vers laquelle rediriger l'utilisateur pour effectuer le paiement de son abonnement par carte bancaire. Le paramètre `{CHECKOUT_SESSION_ID}` dans `successUrl` sera automatiquement résolu par Stripe lors de la redirection après succès.

#### Request Body
```json
{
  "plan": "STARTER",
  "successUrl": "https://app.prospecta.sn/billing/success?session_id={CHECKOUT_SESSION_ID}",
  "cancelUrl": "https://app.prospecta.sn/billing"
}
```

#### Response `201 Created`
```json
{
  "data": {
    "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_live_a1b2c3d4e5...",
    "sessionId": "cs_live_a1b2c3d4e5..."
  }
}
```

> [!NOTE]
> Côté Frontend (Next.js / React) : dès réception de la réponse, rediriger simplement l'utilisateur avec `window.location.href = data.checkoutUrl;`. Le backend met à jour le compte dès la confirmation du paiement via le Webhook Stripe.

---

### 14.4. Accéder au portail client Stripe (Gestion des cartes et factures)
`POST /api/v1/billing/portal`  
*(Requiert le rôle `ORG_ADMIN` ou `SUPER_ADMIN`)*

Permet à l'administrateur d'accéder au portail sécurisé Stripe Customer Portal pour télécharger ses factures, modifier sa carte bancaire ou gérer son abonnement.

#### Request Body
```json
{
  "returnUrl": "https://app.prospecta.sn/settings/billing"
}
```

#### Response `200 OK`
```json
{
  "data": {
    "portalUrl": "https://billing.stripe.com/p/session_live_xyz..."
  }
}
```

---

### 14.5. Réception des Webhooks Stripe (`/api/v1/webhooks/stripe`)
`POST /api/v1/webhooks/stripe`  
`Stripe-Signature: t=1614589200,v1=5257a869e7ece...`

Endpoint public non authentifié par JWT, mais **sécurisé cryptographiquement par HMAC SHA-256** via l'en-tête `Stripe-Signature`.
Événements traités :
- `checkout.session.completed` : Active le plan choisi (`STARTER` ou `BUSINESS`), enregistre l'identifiant client (`stripeCustomerId`) et abonnement (`stripeSubscriptionId`), et augmente immédiatement le quota IA mensuel de l'organisation.
- `customer.subscription.updated` : Met à jour le statut (`ACTIVE`, `TRIALING`, `PAST_DUE`, `CANCELED`) et la date d'échéance de période (`currentPeriodEnd`).
- `customer.subscription.deleted` : Rétrograde automatiquement l'organisation vers le plan `FREE` (100 quotas IA).
- `invoice.payment_failed` : Passe le statut de l'abonnement à `PAST_DUE`.

```json
{
  "status": 200,
  "body": "EVENT_RECEIVED"
}
```

---

## 🔍 15. Découverte de Prospects & Entreprises — Apollo (`/api/v1/discovery`)

Prospecta intègre une brique de découverte de prospects et d'entreprises B2B sans scraping LinkedIn ni dépendance rigide, connectée par défaut au fournisseur **Apollo.io**.

### Principes Clés :
- **Recherche exploratoire** : Résultats temporaires paginés (1 à 50 éléments par page, 25 par défaut).
- **Dédoublonnage proactif** : Chaque prospect retourné indique `alreadyImported` et `existingProspectId` s'il existe déjà dans l'espace de travail de l'organisation.
- **Importation sélective** : L'utilisateur sélectionne les prospects ou entreprises souhaités pour les intégrer en base Prospecta et les associer optionnellement à une **Lead List**.
- **Calcul de Score Automatique** : Tout prospect importé passe immédiatement par le moteur `LeadScoringEngine`.

---

### 15.1. Rechercher des prospects avec filtres B2B
`POST /api/v1/discovery/people/search`

Permet de rechercher des décideurs et profils cibles selon des critères de poste, entreprise, localisation et secteur via le fournisseur B2B externe (Apollo).

#### Request Body (`PeopleSearchRequest`)
```json
{
  "firstName": "Mamadou",
  "lastName": "Diop",
  "jobTitles": [
    "CEO",
    "Directeur commercial",
    "Sales Manager"
  ],
  "companyName": "Sonatel",
  "companyDomain": "orange.sn",
  "country": "SN",
  "city": "Dakar",
  "industry": "Technology",
  "companySizeMin": 10,
  "companySizeMax": 200,
  "page": 0,
  "size": 25
}
```
*(Tous les critères sont facultatifs. `size` doit être compris entre 1 et 50, par défaut 25. `page` commence à 0).*

#### Response `200 OK`
```json
{
  "data": {
    "items": [
      {
        "externalId": "apollo_6401a2b3c4d5e6f7a8b9c0d1",
        "firstName": "Mamadou",
        "lastName": "Diop",
        "jobTitle": "CEO",
        "companyName": "Sonatel",
        "companyDomain": "orange.sn",
        "linkedinUrl": "https://www.linkedin.com/in/mamadou-diop-demo",
        "country": "Senegal",
        "city": "Dakar",
        "email": null,
        "phoneNumber": null,
        "source": "APOLLO"
      }
    ],
    "page": 0,
    "size": 25,
    "total": 100,
    "source": "APOLLO"
  }
}
```
*(Remarque : conformément aux principes de discovery B2B, l'email direct et le téléphone peuvent être null à cette étape afin d'éviter la consommation inutile de crédits d'enrichissement).*

---

### 15.2. Rechercher des entreprises avec filtres B2B
`POST /api/v1/discovery/companies/search`

#### Request Body (`CompanySearchRequest`)
```json
{
  "name": "Sonatel",
  "domain": "orange.sn",
  "industry": "Technology",
  "country": "SN",
  "city": "Dakar",
  "companySizeMin": 10,
  "companySizeMax": 200,
  "page": 0,
  "size": 25
}
```

#### Response `200 OK`
```json
{
  "data": {
    "items": [
      {
        "externalId": "apollo_org_6401a2b3c4d5e6f7a8b9c0d2",
        "name": "Sonatel",
        "domain": "orange.sn",
        "industry": "Technology",
        "country": "Senegal",
        "city": "Dakar",
        "employeeCount": 80,
        "linkedinUrl": "https://www.linkedin.com/company/sonatel",
        "source": "APOLLO"
      }
    ],
    "page": 0,
    "size": 25,
    "total": 50,
    "source": "APOLLO"
  }
}
```

---

### 15.3. Importer une sélection de prospects vers la base locale et une Lead List
`POST /api/v1/discovery/people/import`

Importe les prospects sélectionnés par l'utilisateur depuis les résultats de recherche. Crée automatiquement les entreprises rattachées, calcule le score prédictif initial via `LeadScoringEngine`, dédoublonne avec la base locale de l'organisation et peut ajouter automatiquement les prospects créés à une `LeadList`.

#### Request Body (`ImportPeopleRequest`)
Deux modes sont supportés :
1. **Par identifiants externes (depuis le cache de recherche)** :
```json
{
  "externalIds": [
    "apollo_6401a2b3c4d5e6f7a8b9c0d1",
    "apollo_6401a2b3c4d5e6f7a8b9c0d2"
  ],
  "listId": "b1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"
}
```

2. **Par objets prospects complets (avec création optionnelle de liste par nom)** :
```json
{
  "prospects": [
    {
      "externalId": "apollo_6401a2b3c4d5e6f7a8b9c0d1",
      "firstName": "Mamadou",
      "lastName": "Diop",
      "jobTitle": "CEO",
      "companyName": "Sonatel",
      "companyDomain": "orange.sn",
      "linkedinUrl": "https://www.linkedin.com/in/mamadou-diop-demo",
      "country": "Senegal",
      "city": "Dakar",
      "email": null,
      "phoneNumber": null,
      "source": "APOLLO"
    }
  ],
  "listName": "Directeurs Généraux Sénégal 2026"
}
```

#### Response `201 Created` (`ImportPeopleReport`)
```json
{
  "data": {
    "importedCount": 1,
    "duplicateCount": 0,
    "totalProcessed": 1,
    "listId": "b1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "listName": "Directeurs Généraux Sénégal 2026",
    "prospectIds": [
      "d7b1a2c3-e5f6-7a8b-9c0d-1e2f3a4b5c6e"
    ]
  }
}
```

---

### 15.4. Importer une entreprise sélectionnée
`POST /api/v1/discovery/companies/import`

#### Request Body (`DiscoveredCompany`)
```json
{
  "externalId": "apollo_org_6401a2b3c4d5e6f7a8b9c0d2",
  "name": "Sonatel",
  "domain": "orange.sn",
  "industry": "Technology",
  "country": "Senegal",
  "city": "Dakar",
  "employeeCount": 80,
  "linkedinUrl": "https://www.linkedin.com/company/sonatel",
  "source": "APOLLO"
}
```

#### Response `201 Created` (`Company`)
```json
{
  "data": {
    "id": "c1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6f",
    "name": "Sonatel",
    "domain": "orange.sn",
    "industry": "Technology",
    "country": "Senegal",
    "city": "Dakar",
    "linkedinUrl": "https://www.linkedin.com/company/sonatel",
    "externalId": "apollo_org_6401a2b3c4d5e6f7a8b9c0d2",
    "source": "APOLLO"
  }
}
```

---

## 📋 16. Listes de Prospects — Lead Lists (`/api/v1/lead-lists`)

Les **Lead Lists** permettent de structurer les prospects découverts ou créés en segments de prospection (ex: *"Directeurs Commerciaux Dakar Q3"*, *"Fintech Nigeria 2026"*).

### 16.1. Créer une nouvelle liste
`POST /api/v1/lead-lists`

#### Request Body (`CreateLeadListRequest`)
```json
{
  "name": "Décideurs Télécoms Sénégal",
  "description": "Liste de prospection ciblée pour le lancement de l'offre Enterprise"
}
```

#### Response `201 Created`
```json
{
  "data": {
    "id": "b1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "name": "Décideurs Télécoms Sénégal",
    "description": "Liste de prospection ciblée pour le lancement de l'offre Enterprise",
    "prospectCount": 0,
    "createdAt": "2026-09-11T10:15:00Z",
    "updatedAt": "2026-09-11T10:15:00Z"
  }
}
```

---

### 16.2. Lister les listes de prospects (Paginé)
`GET /api/v1/lead-lists?page=0&size=20&sort=createdAt,desc`

#### Response `200 OK`
```json
{
  "data": {
    "items": [
      {
        "id": "b1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "name": "Décideurs Télécoms Sénégal",
        "description": "Liste de prospection ciblée pour le lancement de l'offre Enterprise",
        "prospectCount": 14,
        "createdAt": "2026-09-11T10:15:00Z",
        "updatedAt": "2026-09-11T10:18:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 16.3. Consulter le détail d'une liste
`GET /api/v1/lead-lists/{id}`

#### Response `200 OK`
```json
{
  "data": {
    "id": "b1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "name": "Décideurs Télécoms Sénégal",
    "description": "Liste de prospection ciblée pour le lancement de l'offre Enterprise",
    "prospectCount": 14,
    "createdAt": "2026-09-11T10:15:00Z",
    "updatedAt": "2026-09-11T10:18:00Z"
  }
}
```

---

### 16.4. Obtenir les prospects rattachés à une liste (Paginé)
`GET /api/v1/lead-lists/{id}/prospects?page=0&size=25`

#### Response `200 OK`
```json
{
  "data": {
    "items": [
      {
        "id": "d7b1a2c3-e5f6-7a8b-9c0d-1e2f3a4b5c6e",
        "firstName": "Amadou",
        "lastName": "Diallo",
        "email": "a***@orange.sn",
        "phone": "+221338391200",
        "jobTitle": "Directeur Général",
        "companyName": "Sonatel",
        "status": "NEW",
        "score": 65,
        "scoreLevel": "MEDIUM",
        "createdAt": "2026-09-11T10:16:00Z"
      }
    ],
    "page": 0,
    "size": 25,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 16.5. Ajouter manuellement des prospects à une liste
`POST /api/v1/lead-lists/{id}/prospects`

#### Request Body
```json
[
  "d7b1a2c3-e5f6-7a8b-9c0d-1e2f3a4b5c6e",
  "e8c2b3d4-f6a7-8b9c-0d1e-2f3a4b5c6d7e"
]
```

#### Response `204 No Content`

---

### 16.6. Retirer un prospect d'une liste
`DELETE /api/v1/lead-lists/{id}/prospects/{prospectId}`

#### Response `204 No Content`

---

## ⚡ 17. Enrichissement de Coordonnées (`/api/v1/prospects/{id}/enrichment`)

L'enrichissement B2B est une action distincte de la recherche : elle cible un prospect précis déjà enregistré dans la base Prospecta pour obtenir ses coordonnées directes vérifiées (email nominatif, numéro WhatsApp / mobile, profil complet).

### 17.1. Enrichir un prospect existant via Apollo Person Match
`POST /api/v1/prospects/{id}/enrichment`

#### Fonctionnement interne :
1. Résout le prospect et son entreprise associée.
2. Interroge l'API `people/match` du provider B2B externe (Apollo) avec le domaine, l'entreprise, le nom complet ou le lien LinkedIn.
3. Met à jour l'email et le numéro de téléphone direct.
4. Normalise automatiquement le numéro de téléphone au format WhatsApp Sénégal (`+221...`) si le prospect est situé à Dakar ou au Sénégal.
5. Fait évoluer le statut du prospect vers `QUALIFIED`.
6. Recalcule le score IA prédictif du lead via le `LeadScoringEngine`.
7. Enregistre une trace d'audit d'enrichissement.

#### Response `200 OK` (`ProspectResponse`)
```json
{
  "data": {
    "id": "d7b1a2c3-e5f6-7a8b-9c0d-1e2f3a4b5c6e",
    "firstName": "Amadou",
    "lastName": "Diallo",
    "email": "amadou.diallo@sonatel.sn",
    "phone": "+221771234567",
    "jobTitle": "Directeur Général B2B",
    "companyName": "Sonatel",
    "status": "QUALIFIED",
    "score": 88,
    "scoreLevel": "VERY_HIGH",
    "externalId": "apollo_6401a2b3c4d5e6f7a8b9c0d1",
    "metadata": {
      "enrichedBy": "APOLLO",
      "enrichedAt": "2026-09-11T10:20:00Z"
    },
    "createdAt": "2026-09-11T10:16:00Z",
    "updatedAt": "2026-09-11T10:20:00Z"
  }
}
```

---

## 💻 18. Exemple de Client HTTP TypeScript Recommandé

Pour garantir une intégration robuste et type-safe côté Frontend (Next.js / React / Vue) :

```typescript
// src/api/client.ts
import axios, { AxiosError, AxiosResponse } from 'axios';

export interface ApiResponse<T> {
  data: T;
  meta?: Record<string, unknown>;
}

export interface ApiError {
  error: {
    code: string;
    message: string;
    traceId?: string;
    details?: string[];
  };
}

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// Intercepteur : Injection automatique du Token Keycloak
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token'); // ou session Keycloak
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Intercepteur : Gestion unifiée des erreurs
apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.data?.error) {
      console.error(
        `[API Error] [${error.response.data.error.code}] ${error.response.data.error.message}`,
        error.response.data.error.details
      );
    }
    return Promise.reject(error);
  }
);
```

### Exemple d'appel d'un endpoint :
```typescript
// src/services/prospectService.ts
import { apiClient, ApiResponse } from '@/api/client';
import { ProspectResponse, CreateProspectRequest, PageResponse } from '@/types';

export const getProspects = async (status?: string, search?: string, page = 0, size = 20) => {
  const res = await apiClient.get<ApiResponse<PageResponse<ProspectResponse>>>('/api/v1/prospects', {
    params: { status, search, page, size, sort: 'createdAt,desc' }
  });
  return res.data.data; // Renvoie l'objet PageResponse { items: [...], totalElements: ... }
};

export const createProspect = async (payload: CreateProspectRequest) => {
  const res = await apiClient.post<ApiResponse<ProspectResponse>>('/api/v1/prospects', payload);
  return res.data.data;
};

// Recherche Discovery B2B
export const searchDiscoveredPeople = async (request: any) => {
  const res = await apiClient.post<ApiResponse<any>>('/api/v1/discovery/people/search', request);
  return res.data.data;
};

// Enrichissement direct
export const enrichProspect = async (prospectId: string) => {
  const res = await apiClient.post<ApiResponse<ProspectResponse>>(`/api/v1/prospects/${prospectId}/enrichment`);
  return res.data.data;
};
```

