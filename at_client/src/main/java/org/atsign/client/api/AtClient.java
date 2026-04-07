package org.atsign.client.api;

import static org.atsign.client.api.Keys.*;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import org.atsign.client.impl.exceptions.AtException;

/**
 * Represents something which enables users to interact with an AtServer as
 * a map-like interface.
 * Users are able to invoke put, get and delete operations for the different key
 * types ({@link PublicKey}, {@link SelfKey} and {@link SharedKey}).
 * Users can also subscribe to notifications, an {@link AtClient} is an
 * {@link org.atsign.client.api.AtEvents.AtEventBus}, allowing users to
 * register {@link org.atsign.client.api.AtEvents.AtEventListener}s.
 * {@link AtClient}s are closeable resources and should be treated accordingly.
 */
public interface AtClient extends AtEvents.AtEventBus, AutoCloseable {

  /**
   * The methods in this interface are from the perspective of this {@link AtSign}.
   * For example when executing put the sharedBy {@link AtSign}
   * should be the {@link AtSign} of the {@link AtClient}.
   *
   * @return the {@link AtSign} which this {@link AtClient} is representing.
   */
  AtSign getAtSign();

  /**
   * Users are able to get this and execute At Protocol commands directly.
   *
   * @return The underlying {@link AtCommandExecutor} which this {@link AtClient} is using to interact
   *         the At Server.
   */
  AtCommandExecutor getCommandExecutor();

  /**
   * Used to get a String associated with a shared key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.The {@link AtClient} {@link AtSign} should be the
   * sharedBy or the sharedWith for the key.
   *
   * @param sharedKey A {@link SharedKey}
   * @return A String value associated with this key.
   * @throws AtException If command fails in any way.
   */
  String get(SharedKey sharedKey) throws AtException;

  /**
   * Used to get a byte array associated with a shared key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.The {@link AtClient} {@link AtSign} should be the
   * sharedBy or the sharedWith for the key.
   *
   * @param sharedKey A {@link SharedKey}
   * @return A byte array value associated with this key.
   * @throws AtException If command fails in any way.
   */
  byte[] getBinary(SharedKey sharedKey) throws AtException;

  /**
   * Used to associate a String with a shared key.
   * The {@link AtClient} {@link AtSign} should be the sharedBy or the
   * sharedWith for the key.
   *
   * @param sharedKey A {@link SharedKey}.
   * @param value The string value (this cannot be null).
   * @throws AtException If command fails in any way.
   */
  void put(SharedKey sharedKey, String value) throws AtException;

  /**
   * Used to delete a shared key.
   * The {@link AtClient} {@link AtSign} should be the sharedBy for the key.
   *
   * @param sharedKey A {@link SharedKey}.
   * @throws AtException If command fails in any way.
   */
  void delete(SharedKey sharedKey) throws AtException;

  /**
   * Used to get a String associated with a self key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.The {@link AtClient} {@link AtSign} should be the
   * sharedBy for the key.
   *
   * @param selfKey A {@link SelfKey}
   * @return A String value associated with this key.
   * @throws AtException If command fails in any way.
   */
  String get(SelfKey selfKey) throws AtException;

  /**
   * Used to get a byte array associated with a self key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.The {@link AtClient} {@link AtSign} should be the
   * sharedBy for the key.
   *
   * @param selfKey A {@link SelfKey}
   * @return A byte array value associated with this key.
   * @throws AtException If command fails in any way.
   */
  byte[] getBinary(SelfKey selfKey) throws AtException;

  /**
   * Used to associate a String with a self key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.The {@link AtClient} {@link AtSign} should be the
   * sharedBy for the key.
   *
   * @param selfKey A {@link SelfKey}.
   * @param value The string value to be associated with the {@link SelfKey} (this cannot be null).
   * @throws AtException If command fails in any way.
   */
  void put(SelfKey selfKey, String value) throws AtException;

  /**
   * Used to delete a self key.
   * The {@link AtClient} {@link AtSign} should be the sharedBy for the key.
   *
   * @param selfKey A {@link SelfKey}.
   * @throws AtException If command fails in any way.
   */
  void delete(SelfKey selfKey) throws AtException;

  /**
   * Used to get a String associated with a specific key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.
   *
   * @param publicKey A {@link PublicKey}.
   * @return A String value associated with this key.
   * @throws AtException If command fails in any way.
   */
  String get(PublicKey publicKey) throws AtException;

  /**
   * Used to get a String associated with a public key.
   *
   * @param publicKey A {@link PublicKey}.
   * @param options Can be used to control the caching behavior.
   * @return A String value associated with this key.
   * @throws AtException If command fails in any way.
   */
  String get(PublicKey publicKey, GetRequestOptions options) throws AtException;

  /**
   * Used to get a byte array associated with a public key.
   *
   * @param publicKey A {@link PublicKey}.
   * @return A byte array value associated with this key.
   * @throws AtException If command fails in any way.
   */
  byte[] getBinary(PublicKey publicKey) throws AtException;

  /**
   * Used to get a byte array associated with a public key.
   *
   * @param publicKey A {@link PublicKey}.
   * @param options Can be used to control the caching behavior.
   * @return A byte array value associated with this key.
   * @throws AtException If command fails in any way.
   */
  byte[] getBinary(PublicKey publicKey, GetRequestOptions options) throws AtException;

  /**
   * Used to associate a String with a public key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.The {@link AtClient} {@link AtSign} should be the
   * sharedBy for the key.
   *
   * @param publicKey A {@link PublicKey}.
   * @param value The string value associated with the {@link PublicKey} (this cannot be null).
   * @throws AtException If command fails in any way.
   */
  void put(PublicKey publicKey, String value) throws AtException;

  /**
   * Used to delete a public key.
   * The {@link AtClient} {@link AtSign} should be the sharedBy for the key.
   *
   * @param publicKey A {@link PublicKey}.
   * @throws AtException If command fails in any way.
   */
  void delete(PublicKey publicKey) throws AtException;

  /**
   * Used to associate a byte array with a shared key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.The {@link AtClient} {@link AtSign} should be the
   * sharedBy for the key.
   *
   * @param sharedKey A {@link SharedKey}.
   * @param value The byte array to associated with the {@link SharedKey}
   * @throws AtException If command fails in any way.
   */
  void put(SharedKey sharedKey, byte[] value) throws AtException;

  /**
   * Used to associate a byte array with a self key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.The {@link AtClient} {@link AtSign} should be the
   * sharedBy for the key.
   *
   * @param selfKey A {@link SelfKey}.
   * @param value The byte array to associated with the {@link SelfKey}
   * @throws AtException If command fails in any way.
   */
  void put(SelfKey selfKey, byte[] value) throws AtException;

  /**
   * Used to associate a byte array with a public key from the perspective of the
   * {@link AtClient}'s {@link AtSign}.The {@link AtClient} {@link AtSign} should be the
   * sharedBy for the key.
   *
   * @param publicKey A {@link PublicKey}.
   * @param value The byte array to associated with the {@link PublicKey}
   * @throws AtException If command fails in any way.
   */
  void put(PublicKey publicKey, byte[] value) throws AtException;

  /**
   * Used to retrieve a list of typed {@link AtKeys} that are visible to this {@link AtClient}'s
   * {@link AtSign} and have a key name what matches a regular expression.
   *
   * @param regex A regular expression String that will be used to filter the returned keys.
   * @return list of {@link AtKeys} subclass instances.
   * @throws AtException If command fails in any way.
   */
  default List<AtKey> getAtKeys(String regex) throws AtException {
    return getAtKeys(regex, false);
  }

  /**
   * Used to retrieve a list of typed {@link AtKeys}, with metadata populated that are visible
   * to this {@link AtClient}'s {@link AtSign} and have a key name what matches a regular expression.
   *
   * @param regex A regular expression String that will be be used to filter the returned keys.
   * @param fetchMetadata If true then for lookup metadata for every key.
   * @return list of {@link AtKeys} subclass instances.
   * @throws AtException If command fails in any way.
   */
  List<AtKey> getAtKeys(String regex, boolean fetchMetadata) throws AtException;

  /**
   * Used to send a monitor command and to ensure notifications are dispatched to registered
   * {@link org.atsign.client.api.AtEvents.AtEventListener}s.
   */
  void startMonitor();

  /**
   * Used to "clear" the monitor command.
   * {@link org.atsign.client.api.AtEvents.AtEventListener}s.
   */
  void stopMonitor();

  /**
   * Used to check if this {@link AtClient} has sent a monitor command.
   *
   * @return true is monitor has been set (and not stopped).
   */
  boolean isMonitorRunning();

  /**
   * Data class used to model options to the {@link org.atsign.client.api.AtClient} get methods
   */
  @Value
  @Builder
  class GetRequestOptions {
    boolean bypassCache;
  }

}
