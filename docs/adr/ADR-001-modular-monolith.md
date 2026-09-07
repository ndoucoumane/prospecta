# ADR-001: Architecture Monolithe Modulaire DDD

## Statut
Accepté

## Contexte
Prospecta démarre en tant que plateforme SaaS B2B au Sénégal. Créer immédiatement un écosystème de 15 microservices introduirait une complexité opérationnelle disproportionnée (latence réseau, transactions distribuées complexes, overhead de déploiement).

## Décision
Adopter une architecture **Monolithe Modulaire** orientée **Domain-Driven Design (DDD)** :
- Un seul livrable déployable (JAR Spring Boot).
- Modules délimités par des frontières strictes (`organization`, `identity`, `prospect`, `campaign`, `messaging`, `ai`, `conversation`, `pipeline`, `analytics`, `shared`).
- Aucune dépendance cyclique entre modules.
- Passage futur vers des microservices facilité car chaque module possède son propre modèle de domaine et respecte les frontières d'agrégat.

## Conséquences
- Déploiement et tests grandement simplifiés.
- Refactorisation rapide au fil des itérations produit.
- Nécessite une discipline stricte sur l'étanchéité des modules (ArchUnit tests).
