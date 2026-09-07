# ADR-007: Intégration Exclusive WhatsApp Business Cloud API (Meta)

## Statut
Accepté

## Contexte
Le canal WhatsApp est prioritaire pour la prospection commerciale au Sénégal. De nombreuses solutions non officielles utilisent l'émulation WhatsApp Web (Selenium, Puppeteer), qui viole les CGU de Meta et entraîne des blocages arbitraires de numéros.

## Décision
Utiliser exclusivement l'**API officielle Meta WhatsApp Business Cloud API** :
- Utilisation des templates de messages approuvés par Meta pour les prises de contact proactives.
- Webhooks entrants sécurisés avec validation HMAC-SHA256 (`X-Hub-Signature-256`).
- Idempotence garantie par enregistrement préalable des `wamid` dans la table `external_events`.
- Traitement asynchrone découplé via Kafka : la réponse HTTP 200 OK est immédiate (< 200ms) pour ne pas bloquer les serveurs Meta.

## Conséquences
- Pérennité et légitimité de la solution.
- Conformité aux politiques anti-spam et RGPD.
