// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: main/proto/data_model.proto

package learn_akka_serialization;

public final class DataModel {
  private DataModel() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface OnlineStoreUserOrBuilder extends
      // @@protoc_insertion_point(interface_extends:learn_akka_serialization.OnlineStoreUser)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>int32 userId = 1;</code>
     * @return The userId.
     */
    int getUserId();

    /**
     * <code>string userName = 2;</code>
     * @return The userName.
     */
    java.lang.String getUserName();
    /**
     * <code>string userName = 2;</code>
     * @return The bytes for userName.
     */
    com.google.protobuf.ByteString
        getUserNameBytes();

    /**
     * <code>string userEmail = 4;</code>
     * @return The userEmail.
     */
    java.lang.String getUserEmail();
    /**
     * <code>string userEmail = 4;</code>
     * @return The bytes for userEmail.
     */
    com.google.protobuf.ByteString
        getUserEmailBytes();
  }
  /**
   * Protobuf type {@code learn_akka_serialization.OnlineStoreUser}
   */
  public  static final class OnlineStoreUser extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:learn_akka_serialization.OnlineStoreUser)
      OnlineStoreUserOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use OnlineStoreUser.newBuilder() to construct.
    private OnlineStoreUser(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private OnlineStoreUser() {
      userName_ = "";
      userEmail_ = "";
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
        UnusedPrivateParameter unused) {
      return new OnlineStoreUser();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private OnlineStoreUser(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {

              userId_ = input.readInt32();
              break;
            }
            case 18: {
              java.lang.String s = input.readStringRequireUtf8();

              userName_ = s;
              break;
            }
            case 34: {
              java.lang.String s = input.readStringRequireUtf8();

              userEmail_ = s;
              break;
            }
            default: {
              if (!parseUnknownField(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return learn_akka_serialization.DataModel.internal_static_learn_akka_serialization_OnlineStoreUser_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return learn_akka_serialization.DataModel.internal_static_learn_akka_serialization_OnlineStoreUser_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              learn_akka_serialization.DataModel.OnlineStoreUser.class, learn_akka_serialization.DataModel.OnlineStoreUser.Builder.class);
    }

    public static final int USERID_FIELD_NUMBER = 1;
    private int userId_;
    /**
     * <code>int32 userId = 1;</code>
     * @return The userId.
     */
    public int getUserId() {
      return userId_;
    }

    public static final int USERNAME_FIELD_NUMBER = 2;
    private volatile java.lang.Object userName_;
    /**
     * <code>string userName = 2;</code>
     * @return The userName.
     */
    public java.lang.String getUserName() {
      java.lang.Object ref = userName_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        userName_ = s;
        return s;
      }
    }
    /**
     * <code>string userName = 2;</code>
     * @return The bytes for userName.
     */
    public com.google.protobuf.ByteString
        getUserNameBytes() {
      java.lang.Object ref = userName_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        userName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int USEREMAIL_FIELD_NUMBER = 4;
    private volatile java.lang.Object userEmail_;
    /**
     * <code>string userEmail = 4;</code>
     * @return The userEmail.
     */
    public java.lang.String getUserEmail() {
      java.lang.Object ref = userEmail_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        userEmail_ = s;
        return s;
      }
    }
    /**
     * <code>string userEmail = 4;</code>
     * @return The bytes for userEmail.
     */
    public com.google.protobuf.ByteString
        getUserEmailBytes() {
      java.lang.Object ref = userEmail_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        userEmail_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (userId_ != 0) {
        output.writeInt32(1, userId_);
      }
      if (!getUserNameBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, userName_);
      }
      if (!getUserEmailBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 4, userEmail_);
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (userId_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(1, userId_);
      }
      if (!getUserNameBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, userName_);
      }
      if (!getUserEmailBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, userEmail_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof learn_akka_serialization.DataModel.OnlineStoreUser)) {
        return super.equals(obj);
      }
      learn_akka_serialization.DataModel.OnlineStoreUser other = (learn_akka_serialization.DataModel.OnlineStoreUser) obj;

      if (getUserId()
          != other.getUserId()) return false;
      if (!getUserName()
          .equals(other.getUserName())) return false;
      if (!getUserEmail()
          .equals(other.getUserEmail())) return false;
      if (!unknownFields.equals(other.unknownFields)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + USERID_FIELD_NUMBER;
      hash = (53 * hash) + getUserId();
      hash = (37 * hash) + USERNAME_FIELD_NUMBER;
      hash = (53 * hash) + getUserName().hashCode();
      hash = (37 * hash) + USEREMAIL_FIELD_NUMBER;
      hash = (53 * hash) + getUserEmail().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static learn_akka_serialization.DataModel.OnlineStoreUser parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(learn_akka_serialization.DataModel.OnlineStoreUser prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code learn_akka_serialization.OnlineStoreUser}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:learn_akka_serialization.OnlineStoreUser)
        learn_akka_serialization.DataModel.OnlineStoreUserOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return learn_akka_serialization.DataModel.internal_static_learn_akka_serialization_OnlineStoreUser_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return learn_akka_serialization.DataModel.internal_static_learn_akka_serialization_OnlineStoreUser_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                learn_akka_serialization.DataModel.OnlineStoreUser.class, learn_akka_serialization.DataModel.OnlineStoreUser.Builder.class);
      }

      // Construct using learn_akka_serialization.DataModel.OnlineStoreUser.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
        }
      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        userId_ = 0;

        userName_ = "";

        userEmail_ = "";

        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return learn_akka_serialization.DataModel.internal_static_learn_akka_serialization_OnlineStoreUser_descriptor;
      }

      @java.lang.Override
      public learn_akka_serialization.DataModel.OnlineStoreUser getDefaultInstanceForType() {
        return learn_akka_serialization.DataModel.OnlineStoreUser.getDefaultInstance();
      }

      @java.lang.Override
      public learn_akka_serialization.DataModel.OnlineStoreUser build() {
        learn_akka_serialization.DataModel.OnlineStoreUser result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public learn_akka_serialization.DataModel.OnlineStoreUser buildPartial() {
        learn_akka_serialization.DataModel.OnlineStoreUser result = new learn_akka_serialization.DataModel.OnlineStoreUser(this);
        result.userId_ = userId_;
        result.userName_ = userName_;
        result.userEmail_ = userEmail_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }
      @java.lang.Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.setField(field, value);
      }
      @java.lang.Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }
      @java.lang.Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }
      @java.lang.Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }
      @java.lang.Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }
      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof learn_akka_serialization.DataModel.OnlineStoreUser) {
          return mergeFrom((learn_akka_serialization.DataModel.OnlineStoreUser)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(learn_akka_serialization.DataModel.OnlineStoreUser other) {
        if (other == learn_akka_serialization.DataModel.OnlineStoreUser.getDefaultInstance()) return this;
        if (other.getUserId() != 0) {
          setUserId(other.getUserId());
        }
        if (!other.getUserName().isEmpty()) {
          userName_ = other.userName_;
          onChanged();
        }
        if (!other.getUserEmail().isEmpty()) {
          userEmail_ = other.userEmail_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        learn_akka_serialization.DataModel.OnlineStoreUser parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (learn_akka_serialization.DataModel.OnlineStoreUser) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int userId_ ;
      /**
       * <code>int32 userId = 1;</code>
       * @return The userId.
       */
      public int getUserId() {
        return userId_;
      }
      /**
       * <code>int32 userId = 1;</code>
       * @param value The userId to set.
       * @return This builder for chaining.
       */
      public Builder setUserId(int value) {
        
        userId_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>int32 userId = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearUserId() {
        
        userId_ = 0;
        onChanged();
        return this;
      }

      private java.lang.Object userName_ = "";
      /**
       * <code>string userName = 2;</code>
       * @return The userName.
       */
      public java.lang.String getUserName() {
        java.lang.Object ref = userName_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          userName_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>string userName = 2;</code>
       * @return The bytes for userName.
       */
      public com.google.protobuf.ByteString
          getUserNameBytes() {
        java.lang.Object ref = userName_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          userName_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>string userName = 2;</code>
       * @param value The userName to set.
       * @return This builder for chaining.
       */
      public Builder setUserName(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        userName_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>string userName = 2;</code>
       * @return This builder for chaining.
       */
      public Builder clearUserName() {
        
        userName_ = getDefaultInstance().getUserName();
        onChanged();
        return this;
      }
      /**
       * <code>string userName = 2;</code>
       * @param value The bytes for userName to set.
       * @return This builder for chaining.
       */
      public Builder setUserNameBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
        
        userName_ = value;
        onChanged();
        return this;
      }

      private java.lang.Object userEmail_ = "";
      /**
       * <code>string userEmail = 4;</code>
       * @return The userEmail.
       */
      public java.lang.String getUserEmail() {
        java.lang.Object ref = userEmail_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          userEmail_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>string userEmail = 4;</code>
       * @return The bytes for userEmail.
       */
      public com.google.protobuf.ByteString
          getUserEmailBytes() {
        java.lang.Object ref = userEmail_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          userEmail_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>string userEmail = 4;</code>
       * @param value The userEmail to set.
       * @return This builder for chaining.
       */
      public Builder setUserEmail(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        userEmail_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>string userEmail = 4;</code>
       * @return This builder for chaining.
       */
      public Builder clearUserEmail() {
        
        userEmail_ = getDefaultInstance().getUserEmail();
        onChanged();
        return this;
      }
      /**
       * <code>string userEmail = 4;</code>
       * @param value The bytes for userEmail to set.
       * @return This builder for chaining.
       */
      public Builder setUserEmailBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
        
        userEmail_ = value;
        onChanged();
        return this;
      }
      @java.lang.Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:learn_akka_serialization.OnlineStoreUser)
    }

    // @@protoc_insertion_point(class_scope:learn_akka_serialization.OnlineStoreUser)
    private static final learn_akka_serialization.DataModel.OnlineStoreUser DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new learn_akka_serialization.DataModel.OnlineStoreUser();
    }

    public static learn_akka_serialization.DataModel.OnlineStoreUser getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<OnlineStoreUser>
        PARSER = new com.google.protobuf.AbstractParser<OnlineStoreUser>() {
      @java.lang.Override
      public OnlineStoreUser parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new OnlineStoreUser(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<OnlineStoreUser> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<OnlineStoreUser> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public learn_akka_serialization.DataModel.OnlineStoreUser getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_learn_akka_serialization_OnlineStoreUser_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_learn_akka_serialization_OnlineStoreUser_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\033main/proto/data_model.proto\022\030learn_akk" +
      "a_serialization\"F\n\017OnlineStoreUser\022\016\n\006us" +
      "erId\030\001 \001(\005\022\020\n\010userName\030\002 \001(\t\022\021\n\tuserEmai" +
      "l\030\004 \001(\tB\032\n\030learn_akka_serializationb\006pro" +
      "to3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_learn_akka_serialization_OnlineStoreUser_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_learn_akka_serialization_OnlineStoreUser_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_learn_akka_serialization_OnlineStoreUser_descriptor,
        new java.lang.String[] { "UserId", "UserName", "UserEmail", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
