# ADR-008: Outbox Pattern pour la Publication Événementielle Résiliente

## Statut
Accepté

## Contexte
Lors du déclenchement d'actions critiques (création de prospect, lancement de campagne, réception de webhook), l'écriture en base de données et la publication dans Kafka ne peuvent pas partager une transaction XA distribuée sans impact sévère sur la latence et la disponibilité (problème du double commit : commit DB réussi mais échec d'envoi Kafka).

## Décision
Implémenter le pattern **Transactional Outbox** :
- Les données métier et l'événement `OutboxEvent` sont enregistrés dans la même transaction PostgreSQL locale.
- Le service `OutboxService` publie les événements vers Kafka avec retries et mise à jour du statut (`PENDING` -> `PUBLISHED` / `FAILED`).
- Les consumers Kafka vérifient l'idempotence via la table `processed_events`.

## Conséquences
- Garantie de livraison *at-least-once* sans perte d'événements.
- Tolérance aux pannes réseau temporaires du cluster Kafka.
