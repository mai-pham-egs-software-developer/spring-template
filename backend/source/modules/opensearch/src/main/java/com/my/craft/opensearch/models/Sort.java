package com.my.craft.opensearch.models;

import java.util.ArrayList;
import java.util.List;

import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;

/**
 * Ordered sort fields; {@link #SCORE} sorts by relevance. Paged searches ({@link PitRequest},
 * {@link PageRequest}) need the last field to be unique per result so equal values keep a fixed
 * order between requests -- see {@link #withTieBreaker(String)}.
 */
public record Sort(List<Order> orders) {

    public static final String SCORE = "_score";

    public enum Direction { ASC, DESC }

    public record Order(String field, Direction direction) {}

    public Sort {
        orders = orders == null ? List.of() : List.copyOf(orders);
    }

    /** {@code _score} descending. */
    public static Sort relevance() {
        return by(SCORE, Direction.DESC);
    }

    public static Sort by(String field, Direction direction) {
        return new Sort(List.of(new Order(field, direction)));
    }

    public Sort then(String field, Direction direction) {
        List<Order> combined = new ArrayList<>(orders);
        combined.add(new Order(field, direction));
        return new Sort(combined);
    }

    /** Appends {@code field} ascending unless this sort already ends with it. */
    public Sort withTieBreaker(String field) {
        if (!orders.isEmpty() && orders.get(orders.size() - 1).field().equals(field)) {
            return this;
        }
        return then(field, Direction.ASC);
    }

    public List<SortOptions> toSortOptions() {
        return orders.stream().map(Sort::toSortOption).toList();
    }

    private static SortOptions toSortOption(Order order) {
        SortOrder direction = order.direction() == Direction.ASC ? SortOrder.Asc : SortOrder.Desc;
        if (SCORE.equals(order.field())) {
            return SortOptions.of(so -> so.score(sc -> sc.order(direction)));
        }
        return SortOptions.of(so -> so.field(f -> f.field(order.field()).order(direction)));
    }
}
