package org.atsign.client.api;


import static org.atsign.client.api.Metadata.*;
import static org.atsign.client.impl.common.Preconditions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Parent class for key classes in the Atsign Platform
 */
@SuppressWarnings("unused")
public abstract class Keys {

  private static final Metadata PUBLIC_KEY_METADATA = Metadata.builder()
      .isPublic(true)
      .isHidden(false)
      .isCached(false)
      .isEncrypted(false)
      .build();

  private static final Metadata SELF_KEY_METADATA = Metadata.builder()
      .isPublic(false)
      .isHidden(false)
      .isCached(false)
      .isEncrypted(true)
      .build();

  private static final Metadata SHARED_KEY_METADATA = Metadata.builder()
      .isPublic(false)
      .isHidden(false)
      .isCached(false)
      .isEncrypted(true)
      .build();

  private static final Metadata PRIVATE_HIDDEN_KEY_METADATA = Metadata.builder()
      .isPublic(false)
      .isHidden(true)
      .isCached(false)
      .build();

  private static final ThreadLocal<List<RawKeyParser<? extends AtKey>>> RAW_KEY_PARSERS =
      ThreadLocal.withInitial(() -> List.of(new SharedKeyRawKeyParser(),
                                            new PrivateHiddenKeyRawKeyParser(),
                                            new PublicKeyRawKeyParser(),
                                            new SelfKeyRawKeyParser()));

  private static final Pattern ILLEGAL_KEY_CHARS = Pattern.compile("[@:\\s]");

  private static final Pattern NAMESPACE_QUALIFIED_KEY_NAME = Pattern.compile("^(?!shared_key)(.+)\\.([^.]+)$");

  public static Matcher createNamespaceQualifiedKeyNameMatcher(String s) {
    return NAMESPACE_QUALIFIED_KEY_NAME.matcher(s);
  }

  /**
   * Base class for keys in the Atsign Platform.
   */
  @Getter
  @Accessors(fluent = true)
  public static abstract class AtKey {

    private final String name;
    private final AtSign sharedWith;
    private final AtSign sharedBy;
    private final AtomicReference<Metadata> metadata;
    private final String rawKey;

    protected AtKey(AtSign sharedBy, AtSign sharedWith, String name, Metadata metadata, String rawKey) {
      this.sharedBy = checkNotNull(sharedBy, "sharedBy is null");
      this.sharedWith = sharedWith;
      this.name = checkKeyName(name);
      this.metadata = new AtomicReference<>(checkMetadata(metadata));
      this.rawKey = rawKey;
    }

    protected AtKey(AtSign sharedBy, AtSign sharedWith, String name, Metadata metadata) {
      this.sharedBy = checkNotNull(sharedBy, "sharedBy is null");
      this.sharedWith = sharedWith;
      this.name = checkKeyName(name);
      this.metadata = new AtomicReference<>(checkMetadata(metadata));
      this.rawKey = createRawKey();
    }

    public Metadata metadata() {
      return metadata.get();
    }

    /**
     * Sets any {@link Metadata} fields that are currently unset.
     *
     * @param metadata new values if those fields are unset in the key's current {@link Metadata}.
     */
    public void updateMissingMetadata(Metadata metadata) {
      this.metadata.set(checkMetadata(Metadata.merge(this.metadata.get(), metadata)));
    }

    /**
     * Updates {@link Metadata} fields with any non-null fields
     *
     * @param metadata new values (unset fields are ignored).
     */
    public void overwriteMetadata(Metadata metadata) {
      this.metadata.set(checkMetadata(Metadata.merge(metadata, this.metadata.get())));
    }

    /**
     * @return the namespace component of the name or null if no namespace.
     */
    public String namespace() {
      return getNamespace(name);
    }

    /**
     * @return the name without the namespace component.
     */
    public String nameWithoutNamespace() {
      return stripNamespace(name);
    }

    /**
     * @return the rawKey.
     */
    @Override
    public String toString() {
      return rawKey;
    }

    protected String createRawKey() {
      Metadata metadata = this.metadata.get();
      return new StringBuilder()
          .append(isNotNullAndTrue(metadata.isCached()) ? "cached:" : "")
          .append(isNotNullAndTrue(metadata.isPublic()) ? "public:" : "")
          .append(sharedWith != null ? sharedWith + ":" : "")
          .append(name)
          .append(sharedBy)
          .toString();
    }
  }

  /**
   * Models a "public key" in the Atsign Platform specification.
   */
  public static class PublicKey extends AtKey {
    protected PublicKey(AtSign sharedBy, String name, Metadata metadata) {
      super(sharedBy, null, name, metadata);
    }
  }

  @Builder(builderMethodName = "publicKeyBuilder", builderClassName = "PublicKeyBuilder")
  public static PublicKey publicKey(AtSign sharedBy, String name, String namespace, Long ttl, Long ttb, Long ttr,
                                    Boolean ccd, Boolean isCached, Boolean isBinary, Metadata metadata) {
    if (metadata != null) {
      checkAllNull("both metadata and metadata fields set, this is ambiguous and not supported",
                   ttl, ttb, ttr, ccd, isCached, isBinary);
    } else {
      MetadataBuilder builder = PUBLIC_KEY_METADATA.toBuilder();
      setTtlIfNotNull(builder, ttl);
      setTtrIfNotNull(builder, ttr);
      setTtbIfNotNull(builder, ttb);
      setCcdIfNotNull(builder, ccd);
      setIsCachedIfNotNull(builder, isCached);
      setIsBinaryIfNotNull(builder, isBinary);
      metadata = builder.build();
    }
    checkNotNull(sharedBy, "sharedBy is not set");
    checkNotNull(name, "name is not set");
    return new PublicKey(sharedBy, toName(name, namespace), metadata);
  }

  /**
   * A builder for instantiating {@link Keys.PublicKey} instances.
   *
   * <pre>

   * Keys.PublicKey key = Keys.publicKeyBuilder()
   *     .sharedBy(...)   // the AtSign which is sharing the key
   *     .name(...)       // the key name
   *     .namespace(...)
   *     .ttl(...)
   *     .ttb(...)
   *     .ttr(...)
   *     .ccd(...)
   *     .isCached(...)
   *     .isBinary(...)
   *     .metadata(...)
   *     .build();
   * </pre>
   */
  public static class PublicKeyBuilder {
  }

  /**
   * Models a "self key" in the Atsign Platform specification.
   */
  public static class SelfKey extends AtKey {
    protected SelfKey(AtSign sharedBy, AtSign sharedWith, String name, Metadata metadata) {
      super(sharedBy, sharedWith, name, metadata);
    }
  }

  @Builder(builderMethodName = "selfKeyBuilder", builderClassName = "SelfKeyBuilder")
  public static SelfKey selfKey(AtSign sharedBy, AtSign sharedWith, String name, String namespace, Long ttl,
                                Long ttb, Long ttr, Boolean ccd, Boolean isHidden, Boolean isBinary,
                                Metadata metadata) {
    if (metadata != null) {
      checkAllNull("both metadata and metadata fields set, this is ambiguous and not supported",
                   ttl, ttb, ttr, ccd, isBinary);
    } else {
      MetadataBuilder builder = SELF_KEY_METADATA.toBuilder();
      setTtlIfNotNull(builder, ttl);
      setTtrIfNotNull(builder, ttr);
      setTtbIfNotNull(builder, ttb);
      setCcdIfNotNull(builder, ccd);
      setIsHiddenIfNotNull(builder, isHidden);
      setIsBinaryIfNotNull(builder, isBinary);
      metadata = builder.build();
    }
    checkNotNull(sharedBy, "sharedBy is not set");
    checkNotNull(name, "name is not set");
    return new SelfKey(sharedBy, sharedWith, toName(name, namespace), metadata);
  }

  /**
   * A builder for instantiating {@link Keys.SelfKey} instances.
   *
   * <pre>

   * Keys.SelfKey key = Keys.selfKeyBuilder()
   *     .sharedBy(...)   // the AtSign which is sharing the key
   *     .sharedWith(...)
   *     .name(...)       // the key name
   *     .namespace(...)
   *     .ttl(...)
   *     .ttb(...)
   *     .ttr(...)
   *     .ccd(...)
   *     .isHidden(...)
   *     .isBinary(...)
   *     .metadata(...)
   *     .build();
   * </pre>
   */
  public static class SelfKeyBuilder {
  }

  /**
   * Models a "shared key" in the Atsign Platform specification.
   */
  public static class SharedKey extends AtKey {
    protected SharedKey(AtSign sharedBy, AtSign sharedWith, String name, Metadata metadata) {
      super(sharedBy, checkNotNull(sharedWith), name, metadata);
    }
  }

  @Builder(builderMethodName = "sharedKeyBuilder", builderClassName = "SharedKeyBuilder")
  public static SharedKey sharedKey(AtSign sharedBy, AtSign sharedWith, String name, String namespace, Long ttl,
                                    Long ttb, Long ttr, Boolean ccd, Boolean isCached, Boolean isBinary,
                                    Boolean isHidden, Metadata metadata, String rawKey) {
    if (rawKey != null) {
      checkAllNull("both rawKey and other fields set",
                   sharedBy, sharedWith, name, namespace, ttl, ttb, ttr, ccd, isCached, isBinary, isHidden);
      SharedKeyRawKeyParser parser = new SharedKeyRawKeyParser();
      parser.test(rawKey);
      return parser.parse(rawKey, metadata);
    }
    if (metadata != null) {
      checkAllNull("both metadata and metadata fields set",
                   ttl, ttb, ttr, ccd, isCached, isBinary, isHidden);
    } else {
      MetadataBuilder builder = SHARED_KEY_METADATA.toBuilder();
      setTtlIfNotNull(builder, ttl);
      setTtrIfNotNull(builder, ttr);
      setTtbIfNotNull(builder, ttb);
      setCcdIfNotNull(builder, ccd);
      setIsCachedIfNotNull(builder, isCached);
      setIsHiddenIfNotNull(builder, isHidden);
      setIsBinaryIfNotNull(builder, isBinary);
      metadata = builder.build();
    }
    checkNotNull(sharedBy, "sharedBy is not set");
    checkNotNull(sharedWith, "sharedWith is not set");
    checkNotNull(name, "name is not set");
    return new SharedKey(sharedBy, sharedWith, toName(name, namespace), metadata);
  }

  /**
   * A builder for instantiating {@link Keys.SharedKey} instances.
   *
   * <pre>

   * Keys.SharedKey key = Keys.sharedKeyBuilder()
   *     .sharedBy(...)   // the AtSign which is sharing the key
   *     .sharedWith(...) // the AtSign which the key is being shared with
   *     .name(...)       // the key name
   *     .namespace(...)
   *     .ttl(...)
   *     .ttb(...)
   *     .ttr(...)
   *     .ccd(...)
   *     .isCached(...)
   *     .isHidden(...)
   *     .isBinary(...)
   *     .metadata(...)
   *     .rawKey(...)     // the raw key i.e. @sharedWith:name@sharedBy
   *     .build();
   * </pre>
   */
  public static class SharedKeyBuilder {
  }

  /**
   * Represents a "private hidden key" in the Atsign Platform
   */
  public static class PrivateHiddenKey extends AtKey {
    protected PrivateHiddenKey(AtSign sharedBy, String name, Metadata metadata, String rawKey) {
      super(sharedBy, null, name, metadata, checkNotNull(rawKey));
    }
  }

  @Builder(builderMethodName = "privateHiddenKeyBuilder", builderClassName = "PrivateHiddenKeyBuilder")
  public static PrivateHiddenKey privateHiddenKey(AtSign sharedBy, String name, String namespace, Metadata metadata,
                                                  String rawKey) {
    metadata = metadata != null ? metadata : PRIVATE_HIDDEN_KEY_METADATA;
    checkNotNull(sharedBy, "sharedBy is not set");
    checkNotNull(name, "name is not set");
    return new PrivateHiddenKey(sharedBy, toName(name, namespace), metadata, rawKey);
  }

  @Builder(builderMethodName = "keyBuilder", builderClassName = "KeyBuilder")
  public static AtKey key(String rawKey, Metadata metadata) {
    for (RawKeyParser<? extends AtKey> parser : RAW_KEY_PARSERS.get()) {
      if (parser.test(rawKey)) {
        return parser.parse(rawKey, metadata);
      }
    }
    throw new IllegalArgumentException(rawKey + " does NOT match any raw key parser");
  }

  /**
   * A builder for instantiating typed {@link AtKey} instances from raw key names.
   * e.g. @sharedWith:name@sharedBy or public:name@sharedBy. This builder will
   * decode everything based on the raw key name.
   *
   * <pre>
   *
   * Keys.AtKey key = Keys.keyBuilder().rawKey(...).build();
   * </pre>
   *
   * Metadata can also be provided.
   *
   * <pre>
   *
   * Keys.AtKey key = Keys.keyBuilder().rawKey(...).metadata(...).build();
   * </pre>
   */
  public static class KeyBuilder {
  }

  private interface RawKeyParser<T extends AtKey> extends Predicate<String> {
    T parse(String rawKey, Metadata metadata);
  }

  private static class PublicKeyRawKeyParser implements RawKeyParser<PublicKey> {

    private final Pattern PATTERN = Pattern.compile("(cached:)?public:([^@]+)(@.+)");

    private Matcher matcher;

    public boolean test(String rawKey) {
      matcher = PATTERN.matcher(rawKey);
      if (matcher.matches()) {
        return isLegalKeyName(matcher.group(2));
      } else {
        return false;
      }
    }

    public PublicKey parse(String rawKey, Metadata metadata) {
      checkNotNull(matcher, "matcher does not match");
      MetadataBuilder metadataBuilder = PUBLIC_KEY_METADATA.toBuilder();
      if (metadata != null) {
        metadataBuilder = Metadata.merge(metadata, metadataBuilder.build()).toBuilder();
      }
      if (matcher.group(1) != null) {
        metadataBuilder.isCached(true);
      }
      if (matcher.group(2).startsWith("_")) {
        metadataBuilder.isHidden(true);
      }
      return publicKeyBuilder()
          .name(matcher.group(2))
          .sharedBy(AtSign.of(matcher.group(3)))
          .metadata(metadataBuilder.build())
          .build();
    }
  }

  private static class SelfKeyRawKeyParser implements RawKeyParser<SelfKey> {

    private static final Pattern PATTERN = Pattern.compile("(@[^:]+:)?([^@]+)(@.+)");

    private Matcher matcher;

    public boolean test(String rawKey) {
      matcher = PATTERN.matcher(rawKey);
      if (matcher.matches()) {
        return (matcher.group(1) == null || chop(matcher.group(1)).equals(matcher.group(3)))
            && isLegalKeyName(matcher.group(2));
      } else {
        return false;
      }
    }

    public SelfKey parse(String rawKey, Metadata metadata) {
      checkNotNull(matcher, "matcher does not match");
      MetadataBuilder metadataBuilder = SELF_KEY_METADATA.toBuilder();
      if (metadata != null) {
        metadataBuilder = Metadata.merge(metadata, metadataBuilder.build()).toBuilder();
      }
      if (matcher.group(2).startsWith("_")) {
        metadataBuilder.isHidden(true);
      }
      return selfKeyBuilder()
          .sharedWith(AtSign.of(chop(matcher.group(1))))
          .name(matcher.group(2))
          .sharedBy(AtSign.of(matcher.group(3)))
          .metadata(metadataBuilder.build())
          .build();
    }
  }

  private static class SharedKeyRawKeyParser implements RawKeyParser<SharedKey> {

    private static final Pattern PATTERN = Pattern.compile("(cached:)?(@[^:]+):([^@]+)(@.+)");

    private Matcher matcher;

    public boolean test(String rawKey) {
      matcher = PATTERN.matcher(rawKey);
      if (matcher.matches()) {
        return !matcher.group(2).equals(matcher.group(4));
      } else {
        return false;
      }
    }

    public SharedKey parse(String rawKey, Metadata metadata) {
      checkNotNull(matcher, "matcher does not match");
      MetadataBuilder metadataBuilder = SHARED_KEY_METADATA.toBuilder();
      if (metadata != null) {
        metadataBuilder = Metadata.merge(metadata, metadataBuilder.build()).toBuilder();
      }
      if (matcher.group(1) != null) {
        metadataBuilder.isCached(true);
      }
      if (matcher.group(3).startsWith("_")) {
        metadataBuilder.isHidden(true);
      }
      return sharedKeyBuilder()
          .sharedWith(AtSign.of(matcher.group(2)))
          .name(matcher.group(3))
          .sharedBy(AtSign.of(matcher.group(4)))
          .metadata(metadataBuilder.build())
          .build();
    }
  }

  private static class PrivateHiddenKeyRawKeyParser implements RawKeyParser<PrivateHiddenKey> {

    private static final Pattern PATTERN = Pattern.compile("(private|privatekey):([^@]+)(@.+)");

    private Matcher matcher;

    public boolean test(String rawKey) {
      matcher = PATTERN.matcher(rawKey);
      return matcher.matches();
    }

    public PrivateHiddenKey parse(String rawKey, Metadata metadata) {
      checkNotNull(matcher, "matcher does not match");
      MetadataBuilder metadataBuilder;
      if (metadata != null) {
        metadataBuilder = Metadata.merge(metadata, PRIVATE_HIDDEN_KEY_METADATA).toBuilder();
      } else {
        metadataBuilder = PRIVATE_HIDDEN_KEY_METADATA.toBuilder();
      }
      return privateHiddenKeyBuilder()
          .name(matcher.group(2))
          .sharedBy(AtSign.of(matcher.group(3)))
          .metadata(metadataBuilder.build())
          .rawKey(rawKey)
          .build();
    }
  }

  private static String toName(String name, String namespace) {
    if (namespace != null && !namespace.isBlank()) {
      return name + "." + namespace;
    } else {
      return name;
    }
  }

  protected static String getNamespace(String name) {
    Matcher matcher = createNamespaceQualifiedKeyNameMatcher(name);
    if (matcher.matches()) {
      return matcher.group(2);
    } else {
      return null;
    }
  }

  protected static String stripNamespace(String name) {
    Matcher matcher = createNamespaceQualifiedKeyNameMatcher(name);
    if (matcher.matches()) {
      return matcher.group(1);
    } else {
      return name;
    }
  }

  private static String checkKeyName(String s) {
    checkNotBlank(s, "key name is blank");
    if (!isLegalKeyName(s)) {
      throw new IllegalArgumentException("illegal characters in key name");
    }
    return s;
  }

  private static boolean isLegalKeyName(String s) {
    return !ILLEGAL_KEY_CHARS.matcher(s).find();
  }

  private static String chop(String s) {
    return s != null ? s.substring(0, s.length() - 1) : null;
  }

  private static Metadata checkMetadata(Metadata metadata) {
    checkNotNull(metadata, "metadata is null");
    checkTrue(metadata.ttl() == null || metadata.ttl() >= 0, "ttl cannot be negative");
    checkTrue(metadata.ttb() == null || metadata.ttb() >= 0, "ttb cannot be negative");
    checkTrue(metadata.ttr() == null || metadata.ttr() >= -1, "ttr cannot be < -1");
    return metadata;
  }

  private static boolean isNotNullAndTrue(Boolean b) {
    return b != null ? b : false;
  }

}
