package app.security.daos;

import app.config.HibernateConfig;
import app.security.entities.Role;
import app.security.entities.User;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityManagerFactory;

public class SecurityPopulatorDAO {

    private static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

    public static UserDTO[] populateUsers(EntityManagerFactory emf) {
        User user, admin;
        Role userRole, adminRole;

        user = new User("usertest", "user123");
        admin = new User("admintest", "admin123");
        userRole = new Role("USER");
        adminRole = new Role("ADMIN");
        user.addRole(userRole);
        admin.addRole(adminRole);

        try (var em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.persist(userRole);
            em.persist(adminRole);
            em.persist(user);
            em.persist(admin);
            em.getTransaction().commit();
        }
        UserDTO userDTO = new UserDTO(user.getUsername(), "user123");
        UserDTO adminDTO = new UserDTO(admin.getUsername(), "admin123");
        return new UserDTO[]{userDTO, adminDTO};
    }
}
