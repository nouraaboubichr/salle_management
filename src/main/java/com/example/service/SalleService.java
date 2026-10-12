package com.example.service;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.util.HibernateUtil;
import com.example.util.PaginationResult;
import org.hibernate.Session;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalleService extends AbstractFacade<Salle> {

    public SalleService() {
        super(Salle.class);
    }

    /** Étape 5 : salles sans réservation qui chevauche le créneau [debut, fin]. */
    public List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN ("
                + "SELECT r.salle.id FROM Reservation r "
                + "WHERE (:debut < r.fin AND :fin > r.debut)) "
                + "ORDER BY s.nom";

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(jpql, Salle.class)
                    .setParameter("debut", debut)
                    .setParameter("fin", fin)
                    .getResultList();
        }
    }

    /**
     * Étape 6 : construit la liste des critères selon les paramètres renseignés.
     * Utilisée par searchRooms ET countRooms pour garantir les mêmes filtres.
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Salle> salle,
                                            String nom, Integer capaciteMin,
                                            LocalDateTime debut, LocalDateTime fin) {
        List<Predicate> predicates = new ArrayList<>();

        // Critère nom (contient, insensible à la casse)
        if (nom != null && !nom.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(salle.<String>get("nom")),
                    "%" + nom.trim().toLowerCase() + "%"));
        }

        // Critère capacité minimale
        if (capaciteMin != null) {
            predicates.add(cb.ge(salle.<Integer>get("capacite"), capaciteMin));
        }

        // Critère disponibilité : exclure les salles réservées sur le créneau
        if (debut != null && fin != null) {
            Subquery<Long> sub = cq.subquery(Long.class);
            Root<Reservation> reservation = sub.from(Reservation.class);
            sub.select(reservation.get("salle").<Long>get("id"));
            sub.where(cb.and(
                    cb.lessThan(reservation.<LocalDateTime>get("debut"), fin),
                    cb.greaterThan(reservation.<LocalDateTime>get("fin"), debut)));

            predicates.add(cb.not(salle.<Long>get("id").in(sub)));
        }

        return predicates;
    }

    /** Étapes 6 et 7 : recherche multi-critères avec pagination (page numérotée à partir de 1). */
    public List<Salle> searchRooms(String nom, Integer capaciteMin,
                                   LocalDateTime debut, LocalDateTime fin,
                                   int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("page et size doivent être supérieurs ou égaux à 1");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
            Root<Salle> salle = cq.from(Salle.class);

            List<Predicate> predicates = buildPredicates(cb, cq, salle, nom, capaciteMin, debut, fin);

            cq.select(salle)
                    .where(predicates.toArray(new Predicate[0]))
                    .orderBy(cb.asc(salle.get("nom")));

            TypedQuery<Salle> query = session.createQuery(cq);
            query.setFirstResult((page - 1) * size);
            query.setMaxResults(size);
            return query.getResultList();
        }
    }

    /** Étape 7 : nombre total de salles, avec exactement les mêmes critères. */
    public long countRooms(String nom, Integer capaciteMin,
                           LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<Salle> salle = countQuery.from(Salle.class);

            List<Predicate> predicates = buildPredicates(cb, countQuery, salle, nom, capaciteMin, debut, fin);

            countQuery.select(cb.count(salle))
                    .where(predicates.toArray(new Predicate[0]));

            return session.createQuery(countQuery).getSingleResult();
        }
    }

    /** Étape 10 : résultat paginé avec métadonnées. */
    public PaginationResult<Salle> searchRoomsPaginated(String nom, Integer capaciteMin,
                                                        LocalDateTime debut, LocalDateTime fin,
                                                        int page, int size) {
        List<Salle> items = searchRooms(nom, capaciteMin, debut, fin, page, size);
        long total = countRooms(nom, capaciteMin, debut, fin);
        return new PaginationResult<>(items, page, size, total);
    }
}