package org.atsign.client.impl.common;

import java.util.Map;
import java.util.function.Function;

/**
 * Base class for logically enumerations but where using an enum would be too brittle.
 * <p>
 * A subclass owns its pool, keeps its constructor private, and exposes an {@code of} factory that
 * interns, so that repeated tokens share an instance:
 *
 * <pre>{@code
 * public static final class MyToken extends TypedToken {
 *
 *   private static final Map<String, MyToken> POOL = new ConcurrentHashMap<>();
 *
 *   private MyToken(String token) {
 *     super(token);
 *   }
 *
 *   public static MyToken of(String token) {
 *     return intern(token, POOL, MyToken::new);
 *   }
 * }
 * }</pre>
 */
public abstract class TypedToken extends TypedString {

  protected TypedToken(String s) {
    super(s);
  }

  protected static <T extends TypedToken> T intern(String s, Map<String, T> pool, Function<String, T> factory) {
    return isBlank(s) ? null : pool.computeIfAbsent(s, factory);
  }
}
