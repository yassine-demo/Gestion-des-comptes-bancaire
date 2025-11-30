package com.fstg.mediatech.repositories;


import com.fstg.mediatech.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fstg.mediatech.entities.Compte;

import java.util.List;

public interface CompteRepository extends  JpaRepository<Compte,Long> {
    List<Compte> findByUser(User user);
}
