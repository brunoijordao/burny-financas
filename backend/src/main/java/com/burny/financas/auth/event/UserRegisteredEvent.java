package com.burny.financas.auth.event;

/**
 * Published after a user registration transaction commits. Plain POJO event (Spring supports these
 * natively) so consuming modules like {@code categories} don't need a compile-time dependency on
 * this event type beyond this shared {@code auth.event} package, and {@code auth} never depends on
 * them back.
 */
public record UserRegisteredEvent(Long userId) {
}
