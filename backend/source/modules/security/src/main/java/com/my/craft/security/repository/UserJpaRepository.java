package com.my.craft.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.security.domain.User;

public interface UserJpaRepository extends JpaRepository<User, String> {}
