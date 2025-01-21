package com.custom_user_provider.repositories;

import com.custom_user_provider.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.*;

import java.util.List;
import java.util.Optional;

public class UserRepository {
@PersistenceContext
    private final EntityManager em;

    public UserRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<User> findByUsername(String username) {
        System.out.println("Searching for username: " + username);
        TypedQuery<User> query = em.createQuery(
                "SELECT p FROM User p WHERE p.username = :username", User.class);
        query.setParameter("username", username);

        List<User> results = query.getResultList();
        System.out.println("Query results: " + results);

        return results.stream().findFirst();
    }

    public Optional<User> findByProfileId(String userId) {
        System.out.println("Searching for profile with ID: " + userId);

        TypedQuery<User> query = em.createQuery(
                "SELECT p FROM User p WHERE p.userId = :userId", User.class);
        query.setParameter("userId", userId);

        List<User> results = query.getResultList();
        System.out.println("Query results: " + results);

        return results.stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        TypedQuery<User> query = em.createQuery(
                "SELECT p FROM User p WHERE p.email = :email", User.class);
        query.setParameter("email", email);
        return query.getResultStream().findFirst();
    }


    public List<User> findBySearchTerm(String searchTerm, Integer firstResult, Integer maxResults) {
        TypedQuery<User> query = em.createQuery(
                "SELECT p FROM User p WHERE LOWER(p.username) LIKE :search OR LOWER(p.email) LIKE :search", User.class);
        query.setParameter("search", "%" + searchTerm.toLowerCase() + "%");

        if (firstResult != null) query.setFirstResult(firstResult);
        if (maxResults != null) query.setMaxResults(maxResults);

        List<User> ps = query.getResultList();

        return ps;
    }


    public User createUser(User user) {
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            return user;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Error while creating user: " + e.getMessage(), e);
        }
    }

//    public User createUser(User user) {
//        em.persist(user);
//        return user;
//    }

    public User getUserByEmail(String email) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
    public User getUserByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean deleteById(String id) {
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            User user = em.find(User.class, id);
            if (user != null) {
                em.remove(user);
                transaction.commit();
                return true;
            }
            transaction.rollback();
            System.out.println("No user found with ID: " + id);
            return false;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            System.err.println("Error occurred while deleting user with ID: " + id);
            e.printStackTrace();
            return false;
        }
    }

    public int count() {
        Object count = em.createQuery("SELECT COUNT(p) FROM User p").getSingleResult();
        return ((Number) count).intValue();
    }

    public void close() {
        if (em.isOpen()) {
            em.close();
        }
    }


}
