package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.daos.impl.RoomDAO;
import app.dtos.PlayerDTO;
import app.dtos.RoomDTO;
import app.entities.Room;
import jakarta.persistence.EntityManagerFactory;

public class Main {

    public static void main(String[] args) {
        ApplicationConfig.startServer(7171);
    }
}