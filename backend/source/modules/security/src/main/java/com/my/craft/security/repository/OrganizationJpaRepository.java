package com.my.craft.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.my.craft.security.domain.Organization;

public interface OrganizationJpaRepository extends JpaRepository<Organization, Long> {

    /**
     * Inserts a row with an explicit {@code id}, bypassing the {@code IDENTITY} generator --
     * needed only for {@link Organization#MASTER_ID}, since {@code @GeneratedValue(IDENTITY)}
     * always asks the DB to assign the id itself and ignores one set on the entity beforehand. A
     * plain {@code IDENTITY} column (Hibernate's default DDL for it on Postgres) still accepts an
     * explicit value in a normal {@code INSERT} -- no {@code OVERRIDING SYSTEM VALUE} needed.
     * {@code ON CONFLICT DO NOTHING} makes this idempotent across repeated boots.
     */
    @Modifying
    @Query(value = "INSERT INTO organizations (id, name) VALUES (:id, :name) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void insertWithExplicitId(@Param("id") long id, @Param("name") String name);
}
