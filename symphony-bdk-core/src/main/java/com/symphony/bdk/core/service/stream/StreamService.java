package com.symphony.bdk.core.service.stream;

import static com.symphony.bdk.core.util.IdUtil.toUrlSafeIdIfNeeded;

import com.symphony.bdk.core.auth.BotAuthSession;
import com.symphony.bdk.core.retry.RetryWithRecovery;
import com.symphony.bdk.core.retry.RetryWithRecoveryBuilder;
import com.symphony.bdk.core.retry.function.SupplierWithApiException;
import com.symphony.bdk.core.service.OboService;
import com.symphony.bdk.core.service.pagination.OffsetBasedPaginatedApi;
import com.symphony.bdk.core.service.pagination.OffsetBasedPaginatedService;
import com.symphony.bdk.core.service.pagination.PaginatedService;
import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.core.service.pagination.model.StreamPaginationAttribute;
import com.symphony.bdk.gen.api.RoomMembershipApi;
import com.symphony.bdk.gen.api.ShareApi;
import com.symphony.bdk.gen.api.StreamsApi;
import com.symphony.bdk.gen.api.model.MemberInfo;
import com.symphony.bdk.gen.api.model.RoomDetail;
import com.symphony.bdk.gen.api.model.ShareContent;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.StreamAttributes;
import com.symphony.bdk.gen.api.model.StreamFilter;
import com.symphony.bdk.gen.api.model.UserId;
import com.symphony.bdk.gen.api.model.V1IMAttributes;
import com.symphony.bdk.gen.api.model.V1IMDetail;
import com.symphony.bdk.gen.api.model.V2AdminStreamFilter;
import com.symphony.bdk.gen.api.model.V2AdminStreamInfo;
import com.symphony.bdk.gen.api.model.V2AdminStreamList;
import com.symphony.bdk.gen.api.model.V2MemberInfo;
import com.symphony.bdk.gen.api.model.V2MembershipList;
import com.symphony.bdk.gen.api.model.V2Message;
import com.symphony.bdk.gen.api.model.V2RoomSearchCriteria;
import com.symphony.bdk.gen.api.model.V2StreamAttributes;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.gen.api.model.V3RoomSearchResults;
import com.symphony.bdk.http.api.ApiException;

import lombok.extern.slf4j.Slf4j;
import org.apiguardian.api.API;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service class for managing streams.
 * <p>
 * This service is used for retrieving information about a particular stream or
 * chatroom, searching streams, listing members, attachments of a particular stream,
 * perform some action related to a stream like:
 * <p><ul>
 * <li>Create a IM or MIM</li>
 * <li>Create a chatroom</li>
 * <li>Activate or Deactivate a chatroom</li>
 * <li></li>
 * </ul></p>
 */
@Slf4j
@API(status = API.Status.STABLE)
public class StreamService implements OboStreamService, OboService<OboStreamService> {

  private final StreamsApi streamsApi;
  private final RoomMembershipApi roomMembershipApi;
  private final ShareApi shareApi;
  private final BotAuthSession authSession;
  private final RetryWithRecoveryBuilder<?> retryBuilder;

  public StreamService(StreamsApi streamsApi, RoomMembershipApi membershipApi, ShareApi shareApi,
      BotAuthSession authSession, RetryWithRecoveryBuilder<?> retryBuilder) {
    this.streamsApi = streamsApi;
    this.roomMembershipApi = membershipApi;
    this.shareApi = shareApi;
    this.authSession = authSession;
    this.retryBuilder = RetryWithRecoveryBuilder.from(retryBuilder)
        .recoveryStrategy(ApiException::isUnauthorized, authSession::refresh);
  }

  public StreamService(StreamsApi streamsApi, RoomMembershipApi membershipApi, ShareApi shareApi,
      RetryWithRecoveryBuilder<?> retryBuilder) {
    this.streamsApi = streamsApi;
    this.roomMembershipApi = membershipApi;
    this.shareApi = shareApi;
    this.authSession = null;
    this.retryBuilder = RetryWithRecoveryBuilder.from(retryBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OboStreamService obo(BotAuthSession oboSession) {
    return new StreamService(streamsApi, roomMembershipApi, shareApi, oboSession, retryBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V2StreamAttributes getStream(@Nonnull String streamId) {
    return executeAndRetry("getStreamInfo", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v2StreamsSidInfoGet(toUrlSafeIdIfNeeded(streamId), authSession.getSessionToken()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<StreamAttributes> listStreams(@Nullable StreamFilter filter) {
    return executeAndRetry("listStreams", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1StreamsListPost(authSession.getSessionToken(), null, null, filter));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<StreamAttributes> listStreams(@Nullable StreamFilter filter, @Nonnull PaginationAttribute pagination) {
    return executeAndRetry("listStreams", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1StreamsListPost(authSession.getSessionToken(), pagination.getSkip(), pagination.getLimit(),
            filter));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @API(status = API.Status.EXPERIMENTAL)
  public java.util.stream.Stream<StreamAttributes> listAllStreams(@Nullable StreamFilter filter) {
    OffsetBasedPaginatedApi<StreamAttributes> api = (offset, limit) -> listStreams(filter, new PaginationAttribute(offset, limit));
    return new OffsetBasedPaginatedService<>(api, PaginatedService.DEFAULT_PAGINATION_CHUNK_SIZE,
        PaginatedService.DEFAULT_PAGINATION_TOTAL_SIZE).stream();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @API(status = API.Status.EXPERIMENTAL)
  public java.util.stream.Stream<StreamAttributes> listAllStreams(@Nullable StreamFilter filter,
      @Nonnull StreamPaginationAttribute pagination) {
    OffsetBasedPaginatedApi<StreamAttributes> api = (offset, limit) -> listStreams(filter, new PaginationAttribute(offset, limit));
    return new OffsetBasedPaginatedService<>(api, pagination.getChunkSize(), pagination.getTotalSize()).stream();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addMemberToRoom(@Nonnull Long userId, @Nonnull String roomId) {
    UserId user = new UserId().id(userId);
    executeAndRetry("addMemberToRoom", roomMembershipApi.getApiClient().getBasePath(),
        () -> roomMembershipApi.v1RoomIdMembershipAddPost(toUrlSafeIdIfNeeded(roomId), authSession.getSessionToken(), user));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeMemberFromRoom(@Nonnull Long userId, @Nonnull String roomId) {
    UserId user = new UserId().id(userId);
    executeAndRetry("removeMemberFrom", roomMembershipApi.getApiClient().getBasePath(),
        () -> roomMembershipApi.v1RoomIdMembershipRemovePost(toUrlSafeIdIfNeeded(roomId), authSession.getSessionToken(), user));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V2Message share(@Nonnull String streamId, @Nonnull ShareContent content) {
    return executeAndRetry("share", shareApi.getApiClient().getBasePath(),
        () -> shareApi.v3StreamSidSharePost(toUrlSafeIdIfNeeded(streamId), authSession.getSessionToken(), content,
            authSession.getKeyManagerToken()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void promoteUserToRoomOwner(@Nonnull Long userId, @Nonnull String roomId) {
    UserId user = new UserId().id(userId);
    executeAndRetry("promoteUserToOwner", roomMembershipApi.getApiClient().getBasePath(),
        () -> roomMembershipApi.v1RoomIdMembershipPromoteOwnerPost(toUrlSafeIdIfNeeded(roomId), authSession.getSessionToken(),
            user));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void demoteUserToRoomParticipant(@Nonnull Long userId, @Nonnull String roomId) {
    UserId user = new UserId().id(userId);
    executeAndRetry("demoteUserToParticipant", roomMembershipApi.getApiClient().getBasePath(),
        () -> roomMembershipApi.v1RoomIdMembershipDemoteOwnerPost(toUrlSafeIdIfNeeded(roomId), authSession.getSessionToken(),
            user));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Stream create(@Nonnull Long... uids) {
    return this.create(Arrays.asList(uids));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Stream create(@Nonnull List<Long> uids) {
    return executeAndRetry("createStreamByUserIds", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1ImCreatePost(authSession.getSessionToken(), uids));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V3RoomDetail create(@Nonnull V3RoomAttributes roomAttributes) {
    return executeAndRetry("createStream", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v3RoomCreatePost(authSession.getSessionToken(), roomAttributes));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V3RoomDetail updateRoom(@Nonnull String roomId, @Nonnull V3RoomAttributes roomAttributes) {
    if(roomAttributes.getPinnedMessageId() != null) {
      String pinnedMessageId = toUrlSafeIdIfNeeded(roomAttributes.getPinnedMessageId());
      roomAttributes.setPinnedMessageId(pinnedMessageId);
    }
    return executeAndRetry("updateRoom", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v3RoomIdUpdatePost(toUrlSafeIdIfNeeded(roomId), authSession.getSessionToken(), roomAttributes));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V3RoomDetail getRoomInfo(@Nonnull String roomId) {
    return executeAndRetry("getRoomInfo", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v3RoomIdInfoGet(toUrlSafeIdIfNeeded(roomId), authSession.getSessionToken()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V3RoomSearchResults searchRooms(@Nonnull V2RoomSearchCriteria query) {
    return executeAndRetry("searchRooms", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v3RoomSearchPost(authSession.getSessionToken(), query, null, null));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V3RoomSearchResults searchRooms(@Nonnull V2RoomSearchCriteria query, @Nonnull PaginationAttribute pagination) {
    return executeAndRetry("searchRooms", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v3RoomSearchPost(authSession.getSessionToken(), query, pagination.getSkip(),
            pagination.getLimit()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @API(status = API.Status.EXPERIMENTAL)
  public java.util.stream.Stream<V3RoomDetail> searchAllRooms(@Nonnull V2RoomSearchCriteria query) {
    OffsetBasedPaginatedApi<V3RoomDetail> api =
        (offset, limit) -> searchRooms(query, new PaginationAttribute(offset, limit)).getRooms();
    return new OffsetBasedPaginatedService<>(api, PaginatedService.DEFAULT_PAGINATION_CHUNK_SIZE,
        PaginatedService.DEFAULT_PAGINATION_TOTAL_SIZE).stream();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @API(status = API.Status.EXPERIMENTAL)
  public java.util.stream.Stream<V3RoomDetail> searchAllRooms(@Nonnull V2RoomSearchCriteria query,
      @Nonnull StreamPaginationAttribute pagination) {
    OffsetBasedPaginatedApi<V3RoomDetail> api =
        (offset, limit) -> searchRooms(query, new PaginationAttribute(offset, limit)).getRooms();
    return new OffsetBasedPaginatedService<>(api, pagination.getChunkSize(), pagination.getTotalSize()).stream();
  }

  /**
   * Deactivate or reactivate a chatroom. At the creation, the chatroom is activated by default.
   *
   * @param roomId The room id
   * @param active Deactivate or activate
   * @return The information of the room after being deactivated or reactivated.
   * @see <a href="https://developers.symphony.com/restapi/reference#de-or-re-activate-room">De/Reactivate Room</a>
   */
  public RoomDetail setRoomActive(@Nonnull String roomId, @Nonnull Boolean active) {
    return executeAndRetry("setRoomActive", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1RoomIdSetActivePost(toUrlSafeIdIfNeeded(roomId), active, authSession.getSessionToken()));
  }

  /**
   * Create a new single or multi party instant message conversation.
   * At least two user IDs must be provided or an error response will be sent.
   * <p>
   * The caller is not included in the members of the created chat.
   * <p>
   * Duplicate users will be included in the membership of the chat but the
   * duplication will be silently ignored.
   * <p>
   * If there is an existing IM conversation with the same set of participants then
   * the id of that existing stream will be returned.
   *
   * @param uids List of user IDs of participants. At least two user IDs must be provided
   * @return The created IM or MIM
   * @see <a href="https://developers.symphony.com/restapi/reference#create-im-or-mim-admin">Create IM or MIM Non-inclusive</a>
   */
  public Stream createInstantMessageAdmin(@Nonnull List<Long> uids) {
    return executeAndRetry("createInstantMessageAdmin", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1AdminImCreatePost(authSession.getSessionToken(), uids));
  }

  /**
   * Updates attributes on an existing IM
   *
   * @param imId  The id or the IM to be updated
   * @param imAttributes  The attributes to be updated in the IM
   * @return  IM information after the update
   * @see <a href="https://developers.symphony.com/restapi/v20.13/reference#update-im">Update IM</a>
   */
  public V1IMDetail updateInstantMessage(@Nonnull String imId, @Nonnull V1IMAttributes imAttributes) {
    if (imAttributes.getPinnedMessageId() != null) {
      String pinnedMessageId = toUrlSafeIdIfNeeded(imAttributes.getPinnedMessageId());
      imAttributes.setPinnedMessageId(pinnedMessageId);
    }
    return executeAndRetry("updateIM", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1ImIdUpdatePost(toUrlSafeIdIfNeeded(imId), authSession.getSessionToken(), imAttributes));
  }

  /**
   * Returns information about a particular IM.
   *
   * @param imId The id of the IM.
   * @return The information about the IM with the given id
   * @see <a href="https://developers.symphony.com/restapi/reference#im-info">IM Info</a>
   */
  public V1IMDetail getInstantMessageInfo(@Nonnull String imId) {
    return executeAndRetry("getIMInfo", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1ImIdInfoGet(toUrlSafeIdIfNeeded(imId), authSession.getSessionToken()));
  }

  /**
   * Deactivate or reactivate a chatroom via AC Portal.
   *
   * @param streamId The stream id
   * @param active   Deactivate or activate
   * @return The information of the room after being deactivated or reactivated.
   */
  public RoomDetail setRoomActiveAdmin(@Nonnull String streamId, @Nonnull Boolean active) {
    return executeAndRetry("setRoomActiveAdmin", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1AdminRoomIdSetActivePost(toUrlSafeIdIfNeeded(streamId), active, authSession.getSessionToken()));
  }

  /**
   * Retrieve all the streams across the enterprise.
   *
   * @param filter The stream searching filter
   * @return List of streams returned according the given filter.
   * @see <a href="https://developers.symphony.com/restapi/reference#list-streams-for-enterprise-v2">List Streams for Enterprise V2</a>
   */
  public V2AdminStreamList listStreamsAdmin(@Nullable V2AdminStreamFilter filter) {
    return executeAndRetry("listStreamsAdmin", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v2AdminStreamsListPost(authSession.getSessionToken(), null, null, filter));
  }

  /**
   * Retrieve all the streams across the enterprise.
   *
   * @param filter     The stream searching filter
   * @param pagination The skip and limit for pagination.
   * @return List of streams returned according the given filter.
   * @see <a href="https://developers.symphony.com/restapi/reference#list-streams-for-enterprise-v2">List Streams for Enterprise V2</a>
   */
  public V2AdminStreamList listStreamsAdmin(@Nullable V2AdminStreamFilter filter,
      @Nonnull PaginationAttribute pagination) {
    return executeAndRetry("listStreamsAdmin", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v2AdminStreamsListPost(authSession.getSessionToken(), pagination.getSkip(),
            pagination.getLimit(), filter));
  }

  /**
   * Retrieve all the streams across the enterprise and return in a {@link java.util.stream.Stream} with default chunk size and total size equals 100.
   *
   * @param filter The stream searching filter
   * @return List of streams returned according the given filter.
   * @see <a href="https://developers.symphony.com/restapi/reference#list-streams-for-enterprise-v2">List Streams for Enterprise V2</a>
   */
  @API(status = API.Status.EXPERIMENTAL)
  public java.util.stream.Stream<V2AdminStreamInfo> listAllStreamsAdmin(@Nullable V2AdminStreamFilter filter) {
    OffsetBasedPaginatedApi<V2AdminStreamInfo> api =
        (offset, limit) -> listStreamsAdmin(filter, new PaginationAttribute(offset, limit)).getStreams();
    return new OffsetBasedPaginatedService<>(api, PaginatedService.DEFAULT_PAGINATION_CHUNK_SIZE,
        PaginatedService.DEFAULT_PAGINATION_TOTAL_SIZE).stream();
  }

  /**
   * Retrieve all the streams across the enterprise and return in a {@link java.util.stream.Stream}.
   *
   * @param filter     The stream searching filter
   * @param pagination The chunkSize and totalSize for stream pagination.
   * @return A {@link java.util.stream.Stream} of streams returned according the given filter.
   * @see <a href="https://developers.symphony.com/restapi/reference#list-streams-for-enterprise-v2">List Streams for Enterprise V2</a>
   */
  @API(status = API.Status.EXPERIMENTAL)
  public java.util.stream.Stream<V2AdminStreamInfo> listAllStreamsAdmin(@Nullable V2AdminStreamFilter filter,
      @Nonnull StreamPaginationAttribute pagination) {
    OffsetBasedPaginatedApi<V2AdminStreamInfo> api =
        (offset, limit) -> listStreamsAdmin(filter, new PaginationAttribute(offset, limit)).getStreams();
    return new OffsetBasedPaginatedService<>(api, pagination.getChunkSize(), pagination.getTotalSize()).stream();
  }

  /**
   * List the current members of an existing stream.
   * The stream can be of type IM, MIM, or ROOM.
   *
   * @param streamId The stream id
   * @return List of member in the stream with the given stream id.
   * @see <a href="https://developers.symphony.com/restapi/reference#stream-members">Stream Members</a>
   */
  public V2MembershipList listStreamMembers(@Nonnull String streamId) {
    return executeAndRetry("listStreamMembers", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1AdminStreamIdMembershipListGet(toUrlSafeIdIfNeeded(streamId), authSession.getSessionToken(), null,
            null));
  }

  /**
   * List the current members of an existing stream.
   * The stream can be of type IM, MIM, or ROOM.
   *
   * @param streamId   The stream id
   * @param pagination The skip and limit for pagination.
   * @return List of member in the stream with the given stream id.
   * @see <a href="https://developers.symphony.com/restapi/reference#stream-members">Stream Members</a>
   */
  public V2MembershipList listStreamMembers(@Nonnull String streamId, @Nonnull PaginationAttribute pagination) {
    return executeAndRetry("listStreamMembers", streamsApi.getApiClient().getBasePath(),
        () -> streamsApi.v1AdminStreamIdMembershipListGet(toUrlSafeIdIfNeeded(streamId), authSession.getSessionToken(),
            pagination.getSkip(), pagination.getLimit()));
  }

  /**
   * List the current members of an existing room and return in a {@link java.util.stream.Stream} with default chunk size and total size equals 100.
   * The stream can be of type IM, MIM, or ROOM.
   *
   * @param streamId The stream id
   * @return A {@link java.util.stream.Stream} of members in the stream with the given stream id.
   * @see <a href="https://developers.symphony.com/restapi/reference#stream-members">Stream Members</a>
   */
  @API(status = API.Status.EXPERIMENTAL)
  public java.util.stream.Stream<V2MemberInfo> listAllStreamMembers(@Nonnull String streamId) {
    OffsetBasedPaginatedApi<V2MemberInfo> api =
        (offset, limit) -> listStreamMembers(toUrlSafeIdIfNeeded(streamId),
            new PaginationAttribute(offset, limit)).getMembers();
    return new OffsetBasedPaginatedService<>(api, PaginatedService.DEFAULT_PAGINATION_CHUNK_SIZE,
        PaginatedService.DEFAULT_PAGINATION_TOTAL_SIZE).stream();
  }

  /**
   * List the current members of an existing room and return in a {@link java.util.stream.Stream}.
   * The stream can be of type IM, MIM, or ROOM.
   *
   * @param streamId   The stream id
   * @param pagination The chunkSize and totalSize for stream pagination with default value equal 100.
   * @return A {@link java.util.stream.Stream} of members in the stream with the given stream id.
   * @see <a href="https://developers.symphony.com/restapi/reference#stream-members">Stream Members</a>
   */
  @API(status = API.Status.EXPERIMENTAL)
  public java.util.stream.Stream<V2MemberInfo> listAllStreamMembers(@Nonnull String streamId,
      @Nonnull StreamPaginationAttribute pagination) {
    OffsetBasedPaginatedApi<V2MemberInfo> api =
        (offset, limit) -> listStreamMembers(toUrlSafeIdIfNeeded(streamId),
            new PaginationAttribute(offset, limit)).getMembers();
    return new OffsetBasedPaginatedService<>(api, pagination.getChunkSize(), pagination.getTotalSize()).stream();
  }

  /**
   * Lists the current members of an existing room.
   *
   * @param roomId The room stream id
   * @return List of members in the room with the given room id.
   * @see <a href="https://developers.symphony.com/restapi/reference#room-members">Room Members</a>
   */
  public List<MemberInfo> listRoomMembers(@Nonnull String roomId) {
    return executeAndRetry("listRoomMembers", roomMembershipApi.getApiClient().getBasePath(),
        () -> roomMembershipApi.v2RoomIdMembershipListGet(toUrlSafeIdIfNeeded(roomId), authSession.getSessionToken()));
  }


  private <T> T executeAndRetry(String name, String address, SupplierWithApiException<T> supplier) {
    checkAuthSession(authSession);
    return RetryWithRecovery.executeAndRetry(retryBuilder, name, address, supplier);
  }
}
