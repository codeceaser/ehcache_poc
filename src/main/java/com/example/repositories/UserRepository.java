package com.example.repositories;

import com.example.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface UserRepository extends JpaRepository<User, Long> {

    Collection<User> findByLocation(String location);

    Collection<User> findByDepartment(String department);
    Collection<User> findByLocationAndDepartment(String location, String department);

}
