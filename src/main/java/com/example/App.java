package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.service.ReservationService;
import com.example.service.SalleService;
import com.example.util.HibernateUtil;
import com.example.util.PaginationResult;
import org.h2.tools.Server;

import java.time.LocalDateTime;
import java.util.List;

public class App {

    private static void afficher(List<Salle> salles) {
        if (salles.isEmpty()) {
            System.out.println("  (aucune salle)");
            return;
        }
        for (Salle s : salles) {
            System.out.println("  - " + s);
        }
    }

    public static void main(String[] args) {

        try {
            Server.createWebServer("-web", "-webPort", "8082").start();
            System.out.println("Console H2 disponible sur : http://localhost:8082");
        } catch (Exception e) {
            System.out.println("Erreur lors du démarrage de la console H2");
            e.printStackTrace();
        }

        SalleService ss = new SalleService();
        ReservationService rs = new ReservationService();


        Salle a = new Salle("Salle A", 20);
        Salle b = new Salle("Salle B", 40);
        Salle c = new Salle("Salle C", 60);
        Salle amphi = new Salle("Amphi 1", 200);
        Salle labo = new Salle("Labo Info", 30);
        ss.create(a);
        ss.create(b);
        ss.create(c);
        ss.create(amphi);
        ss.create(labo);


        rs.create(new Reservation(a, LocalDateTime.of(2024, 6, 1, 9, 0), LocalDateTime.of(2024, 6, 1, 11, 0)));
        rs.create(new Reservation(b, LocalDateTime.of(2024, 6, 1, 10, 0), LocalDateTime.of(2024, 6, 1, 12, 0)));
        rs.create(new Reservation(c, LocalDateTime.of(2024, 6, 1, 14, 0), LocalDateTime.of(2024, 6, 1, 16, 0)));

        LocalDateTime debut = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2024, 6, 1, 12, 0);

        System.out.println("\n===== 1. Salles disponibles entre " + debut + " et " + fin + " =====");
        afficher(ss.findAvailableRooms(debut, fin));


        System.out.println("\n===== 2. Recherche : nom contient 'salle', capacité >= 30 (2 par page) =====");
        PaginationResult<Salle> p1 = ss.searchRoomsPaginated("salle", 30, null, null, 1, 2);
        System.out.println("Total : " + p1.getTotalItems() + " salle(s), " + p1.getTotalPages() + " page(s)");
        System.out.println("Page " + p1.getCurrentPage() + " :");
        afficher(p1.getItems());


        System.out.println("\n===== 3. Toutes les salles, 2 par page =====");
        long total = ss.countRooms(null, null, null, null);
        int pages = (int) Math.ceil(total / 2.0);
        for (int page = 1; page <= pages; page++) {
            System.out.println("Page " + page + "/" + pages + " :");
            afficher(ss.searchRooms(null, null, null, null, page, 2));
        }


        System.out.println("\n===== 4. Nom 'salle', capacité >= 30, disponible entre " + debut + " et " + fin + " =====");
        System.out.println("Total : " + ss.countRooms("salle", 30, debut, fin));
        afficher(ss.searchRooms("salle", 30, debut, fin, 1, 10));

        HibernateUtil.shutdown();
    }
}