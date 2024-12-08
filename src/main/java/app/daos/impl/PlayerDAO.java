package app.daos.impl;

import app.dtos.PlayerDTO;
import app.entities.Player;
import jakarta.persistence.EntityManagerFactory;

public class PlayerDAO {


    private static PlayerDAO instance;
    private static EntityManagerFactory emf;

    public static PlayerDAO getInstance(EntityManagerFactory emf_) {
        if (instance == null) {
            emf = emf_;
            instance = new PlayerDAO();
        }
        return instance;
    }


    public PlayerDTO create(PlayerDTO player) {

        try (var em = emf.createEntityManager()) {

            em.getTransaction().begin();

            var newPlayer = new Player(player);

            em.persist(newPlayer);

            em.getTransaction().commit();

            return new PlayerDTO(newPlayer);

        }
    }

    public PlayerDTO getById(int id) {

        try (var em = emf.createEntityManager()) {

            var player = em.find(Player.class, id);

            return new PlayerDTO(player);

        }
    }
}
