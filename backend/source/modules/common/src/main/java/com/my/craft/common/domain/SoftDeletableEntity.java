package com.my.craft.common.domain;

import java.time.Instant;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;

/**
 * Adds {@code deleted_at} on top of {@link Auditable} for soft delete: a non-null value means
 * "deleted." {@code @SQLRestriction("deleted_at IS NULL")} makes every query Hibernate generates
 * for a subclass (including a plain {@code findById}) silently exclude soft-deleted rows -- this
 * applies at the {@code @MappedSuperclass} level correctly since, unlike {@code @SQLDelete} (see
 * below), the restriction clause itself never needs to know the subclass's table name.
 *
 * <p><strong>What this does NOT do:</strong> intercept {@code delete()}/{@code deleteById()}.
 * {@code @SQLDelete} -- the Hibernate annotation that turns those into an {@code UPDATE} instead
 * of a real {@code DELETE} -- takes a literal SQL string naming the target table, so it can't be
 * centralized here the way {@code @SQLRestriction} can: every subclass's SQL would need its own
 * table name repeated, defeating the point of inheriting it. Soft-deleting an entity here means
 * calling {@link #softDelete()} then saving it through its repository like any other update; a
 * caller that reaches for {@code repository.delete(...)} directly still performs a real, permanent
 * delete on a {@code SoftDeletableEntity} -- there's nothing in this class stopping that.
 */
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
@Getter
public abstract class SoftDeletableEntity extends Auditable {

    @Column
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public void restore() {
        this.deletedAt = null;
    }
}
