package com.symphony.bdk.core.service.stream;

import static com.symphony.bdk.core.util.IdUtil.fromUrlSafeId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.auth.BotAuthSession;
import com.symphony.bdk.core.retry.RetryWithRecoveryBuilder;
import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.core.service.pagination.model.StreamPaginationAttribute;
import com.symphony.bdk.core.test.JsonHelper;
import com.symphony.bdk.core.test.MockApiClient;
import com.symphony.bdk.gen.api.RoomMembershipApi;
import com.symphony.bdk.gen.api.ShareApi;
import com.symphony.bdk.gen.api.StreamsApi;
import com.symphony.bdk.gen.api.model.MemberInfo;
import com.symphony.bdk.gen.api.model.RoomDetail;
import com.symphony.bdk.gen.api.model.ShareContent;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.StreamAttributes;
import com.symphony.bdk.gen.api.model.StreamFilter;
import com.symphony.bdk.gen.api.model.StreamType;
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
import com.symphony.bdk.http.api.ApiClient;
import com.symphony.bdk.http.api.ApiException;
import com.symphony.bdk.http.api.ApiRuntimeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StreamServiceTest {

  private static final String V1_IM_CREATE = "/pod/v1/im/create";
  private static final String V1_IM_CREATE_ADMIN = "/pod/v1/admin/im/create";
  private static final String V1_IM_UPDATE = "/pod/v1/im/{id}/update";
  private static final String V1_IM_INFO = "/pod/v1/im/{id}/info";
  private static final String V1_ROOM_SET_ACTIVE = "/pod/v1/room/{id}/setActive";
  private static final String V1_ROOM_SET_ACTIVE_ADMIN = "/pod/v1/admin/room/{id}/setActive";
  private static final String V1_STREAM_LIST = "/pod/v1/streams/list";
  private static final String V1_ADD_MEMBER_TO_ROOM = "/pod/v1/room/{id}/membership/add";
  private static final String V1_REMOVE_MEMBER_FROM_ROOM = "/pod/v1/room/{id}/membership/remove";
  private static final String V1_STREAM_MEMBERS = "/pod/v1/admin/stream/{id}/membership/list";
  private static final String V1_PROMOTE_MEMBER = "/pod/v1/room/{id}/membership/promoteOwner";
  private static final String V1_DEMOTE_MEMBER = "/pod/v1/room/{id}/membership/demoteOwner";
  private static final String V2_STREAM_INFO = "/pod/v2/streams/{sid}/info";
  private static final String V2_STREAM_LIST_ADMIN = "/pod/v2/admin/streams/list";
  private static final String V2_ROOM_MEMBERS = "/pod/v2/room/{id}/membership/list";
  private static final String V3_ROOM_CREATE = "/pod/v3/room/create";
  private static final String V3_ROOM_SEARCH = "/pod/v3/room/search";
  private static final String V3_ROOM_INFO = "/pod/v3/room/{id}/info";
  private static final String V3_ROOM_UPDATE = "/pod/v3/room/{id}/update";
  private static final String V3_SHARE = "/agent/v3/stream/{sid}/share";

  private StreamService service;
  private MockApiClient mockApiClient;
  private RoomMembershipApi spyRoomMembershipApi;
  private BotAuthSession authSession;
  private StreamsApi streamsApi;
  private ShareApi shareApi;

  @BeforeEach
  void setUp() {
    this.mockApiClient = new MockApiClient();
    this.authSession = mock(BotAuthSession.class);
    ApiClient podClient = mockApiClient.getApiClient("/pod");
    ApiClient agentClient = mockApiClient.getApiClient("/agent");

    this.spyRoomMembershipApi = spy(new RoomMembershipApi(podClient));
    this.streamsApi = spy(new StreamsApi(podClient));
    this.shareApi = new ShareApi(agentClient);
    this.service = new StreamService(this.streamsApi, this.spyRoomMembershipApi, this.shareApi,
        this.authSession, new RetryWithRecoveryBuilder<>());

    when(authSession.getSessionToken()).thenReturn("1234");
    when(authSession.getKeyManagerToken()).thenReturn("1234");
  }

  @Test
  void nonOboEndpointShouldThrowExceptionInOboMode() {
    this.service = new StreamService(this.streamsApi, this.spyRoomMembershipApi, this.shareApi,
        new RetryWithRecoveryBuilder<>());

    assertThrows(IllegalStateException.class, () -> this.service.getStream(""));
  }

  @Test
  void getStreamInOboMode() throws IOException {
    this.mockApiClient.onGet(V2_STREAM_INFO.replace("{sid}", "p9B316LKDto7iOECc8Xuz3qeWsc0bdA"),
        JsonHelper.readFromClasspath("/stream/v2_stream_attributes.json"));

    this.service = new StreamService(this.streamsApi, this.spyRoomMembershipApi, this.shareApi,
        new RetryWithRecoveryBuilder<>());
    V2StreamAttributes stream = this.service.obo(this.authSession).getStream("p9B316LKDto7iOECc8Xuz3qeWsc0bdA");

    assertEquals("p9B316LKDto7iOECc8Xuz3qeWsc0bdA", stream.getId());
    assertEquals("INTERNAL", stream.getOrigin());
  }

  @Test
  void createIMorMIMTest() {
    this.mockApiClient.onPost(V1_IM_CREATE, "{\"id\": \"xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA\"}");

    Stream stream = this.service.create(Arrays.asList(7215545078541L, 7215512356741L));

    assertEquals("xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA", stream.getId());
  }

  @Test
  void createIMorMIMTestFailed() {
    this.mockApiClient.onPost(400, V1_IM_CREATE, "{}");

    List<Long> userIds = Arrays.asList(7215545078541L, 7215512356741L);
    assertThrows(ApiRuntimeException.class, () -> this.service.create(userIds));
  }

  @Test
  void createIMorMIMInOboModeTest() {
    this.mockApiClient.onPost(V1_IM_CREATE, "{\"id\": \"xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA\"}");

    Stream stream = this.service.obo(this.authSession).create(Arrays.asList(7215545078541L, 7215512356741L));

    assertEquals("xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA", stream.getId());
  }

  @Test
  void createIMTest() {
    this.mockApiClient.onPost(V1_IM_CREATE, "{\"id\": \"xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA\"}");

    Stream stream = this.service.create(7215545078541L);

    assertEquals("xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA", stream.getId());
  }

  @Test
  void createIMTestFailed() {
    this.mockApiClient.onPost(400, V1_IM_CREATE, "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.create(7215545078541L));
  }

  @Test
  void createIMInOboModeTest() {
    this.mockApiClient.onPost(V1_IM_CREATE, "{\"id\": \"xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA\"}");

    Stream stream = this.service.obo(this.authSession).create(7215545078541L);

    assertEquals("xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA", stream.getId());
  }

  @Test
  void createRoomChatTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_CREATE, JsonHelper.readFromClasspath(
        "/stream/v3_room_detail.json"));

    V3RoomDetail roomDetail = this.service.create(new V3RoomAttributes());

    assertEquals("API room", roomDetail.getRoomAttributes().getName() );
    assertEquals("Created via the API", roomDetail.getRoomAttributes().getDescription());
    assertEquals("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", roomDetail.getRoomSystemInfo().getId());
  }

  @Test
  void createRoomChatTestFailed() {
    this.mockApiClient.onPost(400, V3_ROOM_CREATE, "{}");

    V3RoomAttributes v3RoomAttributes = new V3RoomAttributes();
    assertThrows(ApiRuntimeException.class, () -> this.service.create(v3RoomAttributes));
  }

  @Test
  void createRoomChatInOboModeTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_CREATE, JsonHelper.readFromClasspath(
        "/stream/v3_room_detail.json"));

    V3RoomDetail roomDetail = this.service.obo(this.authSession).create(new V3RoomAttributes());

    assertEquals("API room", roomDetail.getRoomAttributes().getName());
    assertEquals("Created via the API", roomDetail.getRoomAttributes().getDescription());
    assertEquals("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", roomDetail.getRoomSystemInfo().getId());
  }

  @Test
  void searchRoomsTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_SEARCH, JsonHelper.readFromClasspath("/stream/room_search.json"));

    V3RoomSearchResults searchResults = this.service.searchRooms(new V2RoomSearchCriteria());

    assertEquals(searchResults.getCount(), 2);
    assertEquals("Automobile Industry Room", searchResults.getRooms().get(0).getRoomAttributes().getName());
  }

  @Test
  void searchRoomsTestFailed() {
    this.mockApiClient.onPost(400, V3_ROOM_SEARCH, "{}");

    V2RoomSearchCriteria v2RoomSearchCriteria = new V2RoomSearchCriteria();
    assertThrows(ApiRuntimeException.class, () -> this.service.searchRooms(v2RoomSearchCriteria));
  }

  @Test
  void searchRoomsSkipLimitTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_SEARCH, JsonHelper.readFromClasspath("/stream/room_search.json"));

    V3RoomSearchResults searchResults =
        this.service.searchRooms(new V2RoomSearchCriteria(), new PaginationAttribute(0, 100));

    assertEquals(2, searchResults.getCount());
    assertEquals("Automobile Industry Room", searchResults.getRooms().get(0).getRoomAttributes().getName());
  }

  @Test
  void searchRoomsInOboModeTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_SEARCH, JsonHelper.readFromClasspath("/stream/room_search.json"));

    V3RoomSearchResults searchResults = this.service.obo(this.authSession).searchRooms(new V2RoomSearchCriteria());

    assertEquals(2, searchResults.getCount());
    assertEquals("Automobile Industry Room", searchResults.getRooms().get(0).getRoomAttributes().getName());
  }

  @Test
  void searchRoomsSkipLimitInOboModeTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_SEARCH, JsonHelper.readFromClasspath("/stream/room_search.json"));

    V3RoomSearchResults searchResults =
        this.service.obo(this.authSession).searchRooms(new V2RoomSearchCriteria(), new PaginationAttribute(0, 100));

    assertEquals(2, searchResults.getCount());
    assertEquals("Automobile Industry Room", searchResults.getRooms().get(0).getRoomAttributes().getName());
  }

  @Test
  void searchAllRoomsTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_SEARCH, JsonHelper.readFromClasspath("/stream/room_search.json"));

    List<V3RoomDetail> searchResults =
        this.service.searchAllRooms(new V2RoomSearchCriteria()).collect(Collectors.toList());

    assertEquals(2, searchResults.size());
    assertEquals("Automobile Industry Room", searchResults.get(0).getRoomAttributes().getName());
  }

  @Test
  void searchAllRoomsStreamPaginationTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_SEARCH, JsonHelper.readFromClasspath("/stream/room_search.json"));

    List<V3RoomDetail> searchResults =
        this.service.searchAllRooms(new V2RoomSearchCriteria(), new StreamPaginationAttribute(100, 100))
            .collect(Collectors.toList());

    assertEquals(2, searchResults.size());
    assertEquals("Automobile Industry Room", searchResults.get(0).getRoomAttributes().getName());
  }

  @Test
  void searchAllRoomsInOboModeTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_SEARCH, JsonHelper.readFromClasspath("/stream/room_search.json"));

    List<V3RoomDetail> searchResults =
        this.service.obo(this.authSession).searchAllRooms(new V2RoomSearchCriteria()).collect(Collectors.toList());

    assertEquals(2, searchResults.size());
    assertEquals("Automobile Industry Room", searchResults.get(0).getRoomAttributes().getName());
  }

  @Test
  void searchAllRoomsStreamPaginationInOboModeTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_SEARCH, JsonHelper.readFromClasspath("/stream/room_search.json"));

    List<V3RoomDetail> searchResults =
        this.service.obo(this.authSession)
            .searchAllRooms(new V2RoomSearchCriteria(), new StreamPaginationAttribute(100, 100))
            .collect(Collectors.toList());

    assertEquals(2, searchResults.size());
    assertEquals("Automobile Industry Room", searchResults.get(0).getRoomAttributes().getName());
  }

  @Test
  void getRoomInfoTest() throws IOException {
    this.mockApiClient.onGet(V3_ROOM_INFO.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"),
        JsonHelper.readFromClasspath("/stream/v3_room_detail.json"));

    V3RoomDetail roomDetail = this.service.getRoomInfo("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA");

    assertEquals("API room", roomDetail.getRoomAttributes().getName());
    assertEquals("Created via the API", roomDetail.getRoomAttributes().getDescription());
    assertEquals("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", roomDetail.getRoomSystemInfo().getId());
  }

  @Test
  void getRoomInfoTest_base64() throws IOException {
    this.mockApiClient.onGet(V3_ROOM_INFO.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"),
        JsonHelper.readFromClasspath("/stream/v3_room_detail.json"));

    V3RoomDetail roomDetail = this.service.getRoomInfo(fromUrlSafeId("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"));

    assertEquals("API room", roomDetail.getRoomAttributes().getName());
  }

  @Test
  void getRoomInfoTestFailed() {
    this.mockApiClient.onGet(400, V3_ROOM_INFO.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.getRoomInfo("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"));
  }

  @Test
  void getRoomInfoInOboModeTest() throws IOException {
    this.mockApiClient.onGet(V3_ROOM_INFO.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"),
        JsonHelper.readFromClasspath("/stream/v3_room_detail.json"));

    V3RoomDetail roomDetail = this.service.obo(this.authSession).getRoomInfo("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA");

    assertEquals("API room", roomDetail.getRoomAttributes().getName());
    assertEquals("Created via the API", roomDetail.getRoomAttributes().getDescription());
    assertEquals("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", roomDetail.getRoomSystemInfo().getId());
  }

  @Test
  void setRoomActiveTest() throws IOException {
    this.mockApiClient.onPost(V1_ROOM_SET_ACTIVE.replace("{id}", "HNmksPVAR6-f14WqKXmqHX___qu8LMLgdA"),
        JsonHelper.readFromClasspath("/stream/room_detail.json"));

    RoomDetail roomDetail = this.service.setRoomActive("HNmksPVAR6-f14WqKXmqHX___qu8LMLgdA", true);

    assertTrue(roomDetail.getRoomSystemInfo().getActive());
  }

  @Test
  void setRoomActiveTestFailed() {
    this.mockApiClient.onPost(400, V1_ROOM_SET_ACTIVE.replace("{id}", "HNmksPVAR6-f14WqKXmqHX___qu8LMLgdA"), "{}");

    assertThrows(ApiRuntimeException.class,
        () -> this.service.setRoomActive("HNmksPVAR6-f14WqKXmqHX___qu8LMLgdA", true));
  }

  @Test
  void updateRoomTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_UPDATE.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"),
        JsonHelper.readFromClasspath("/stream/v3_room_detail.json"));

    V3RoomDetail roomDetail = this.service.updateRoom("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", new V3RoomAttributes());

    assertEquals("API room", roomDetail.getRoomAttributes().getName());
    assertEquals("Created via the API", roomDetail.getRoomAttributes().getDescription());
    assertEquals("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", roomDetail.getRoomSystemInfo().getId());
  }

  @Test
  void updateRoomTest_base64() throws IOException, ApiException {
    this.mockApiClient.onPost(V3_ROOM_UPDATE.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"),
        JsonHelper.readFromClasspath("/stream/v3_room_detail.json"));

    V3RoomAttributes attributes = new V3RoomAttributes();
    attributes.setPinnedMessageId(fromUrlSafeId("wxv6PbPtTSvnjOwVLCss93___oMVTf_AbQ"));
    this.service.updateRoom(fromUrlSafeId("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"), attributes);

    final ArgumentCaptor<V3RoomAttributes> attributesArgumentCaptor = ArgumentCaptor.forClass(V3RoomAttributes.class);
    final ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);

    verify(streamsApi).v3RoomIdUpdatePost(idArgumentCaptor.capture(), any(String.class),
        attributesArgumentCaptor.capture());

    assertEquals("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", idArgumentCaptor.getValue());
    assertEquals("wxv6PbPtTSvnjOwVLCss93___oMVTf_AbQ", attributesArgumentCaptor.getValue().getPinnedMessageId());
  }

  @Test
  void updateRoomTestFailed() {
    this.mockApiClient.onPost(400, V3_ROOM_UPDATE.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"), "{}");

    V3RoomAttributes v3RoomAttributes = new V3RoomAttributes();
    assertThrows(ApiRuntimeException.class,
        () -> this.service.updateRoom("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", v3RoomAttributes));
  }

  @Test
  void updateRoomInOboModeTest() throws IOException {
    this.mockApiClient.onPost(V3_ROOM_UPDATE.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"),
        JsonHelper.readFromClasspath("/stream/v3_room_detail.json"));

    V3RoomDetail roomDetail =
        this.service.obo(this.authSession).updateRoom("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", new V3RoomAttributes());

    assertEquals("API room", roomDetail.getRoomAttributes().getName());
    assertEquals("Created via the API", roomDetail.getRoomAttributes().getDescription());
    assertEquals("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", roomDetail.getRoomSystemInfo().getId());
  }

  @Test
  void updateRoomInOboModeTest_base64() throws IOException, ApiException {
    this.mockApiClient.onPost(V3_ROOM_UPDATE.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"),
        JsonHelper.readFromClasspath("/stream/v3_room_detail.json"));

    V3RoomAttributes attributes = new V3RoomAttributes();
    attributes.setPinnedMessageId(fromUrlSafeId("wxv6PbPtTSvnjOwVLCss93___oMVTf_AbQ"));
    this.service.obo(this.authSession).updateRoom(fromUrlSafeId("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"), attributes);

    final ArgumentCaptor<V3RoomAttributes> attributesArgumentCaptor = ArgumentCaptor.forClass(V3RoomAttributes.class);
    final ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);

    verify(streamsApi).v3RoomIdUpdatePost(idArgumentCaptor.capture(), any(String.class),
        attributesArgumentCaptor.capture());

    assertEquals("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", idArgumentCaptor.getValue());
    assertEquals("wxv6PbPtTSvnjOwVLCss93___oMVTf_AbQ", attributesArgumentCaptor.getValue().getPinnedMessageId());
  }

  @Test
  void listStreamsTest() throws IOException {
    this.mockApiClient.onPost(V1_STREAM_LIST, JsonHelper.readFromClasspath("/stream/list_stream.json"));

    List<StreamAttributes> streams =
        this.service.listStreams(new StreamFilter().addStreamTypesItem(new StreamType().type(
            StreamType.TypeEnum.IM)));

    assertEquals(1, streams.size());
    assertEquals("iWyZBIOdQQzQj0tKOLRivX___qu6YeyZdA", streams.get(0).getId());
  }

  @Test
  void listStreamsTestFailed() {
    this.mockApiClient.onPost(400, V1_STREAM_LIST, "{}");

    StreamFilter streamFilter = new StreamFilter();
    assertThrows(ApiRuntimeException.class, () -> this.service.listStreams(streamFilter));
  }

  @Test
  void listStreamsWithSkipLimit() throws IOException {
    this.mockApiClient.onPost(V1_STREAM_LIST, JsonHelper.readFromClasspath("/stream/list_stream.json"));

    List<StreamAttributes> streams =
        this.service.listStreams(new StreamFilter().addStreamTypesItem(new StreamType().type(
            StreamType.TypeEnum.IM)), new PaginationAttribute(0, 100));
    assertEquals(1, streams.size());
    assertEquals("iWyZBIOdQQzQj0tKOLRivX___qu6YeyZdA", streams.get(0).getId());
  }

  @Test
  void listAllStreamsTest() throws IOException {
    this.mockApiClient.onPost(V1_STREAM_LIST, JsonHelper.readFromClasspath("/stream/list_stream.json"));

    List<StreamAttributes> streams =
        this.service.listAllStreams(new StreamFilter().addStreamTypesItem(new StreamType().type(
            StreamType.TypeEnum.IM))).collect(Collectors.toList());
    assertEquals(1, streams.size());
    assertEquals("iWyZBIOdQQzQj0tKOLRivX___qu6YeyZdA", streams.get(0).getId());
  }

  @Test
  void listAllStreamsPaginationTest() throws IOException {
    this.mockApiClient.onPost(V1_STREAM_LIST, JsonHelper.readFromClasspath("/stream/list_stream.json"));

    List<StreamAttributes> streams =
        this.service.listAllStreams(new StreamFilter().addStreamTypesItem(new StreamType().type(
            StreamType.TypeEnum.IM)), new StreamPaginationAttribute(100, 100)).collect(Collectors.toList());
    assertEquals(1, streams.size());
    assertEquals("iWyZBIOdQQzQj0tKOLRivX___qu6YeyZdA", streams.get(0).getId());
  }

  @Test
  void getStreamInfoTest() throws IOException {
    this.mockApiClient.onGet(V2_STREAM_INFO.replace("{sid}", "p9B316LKDto7iOECc8Xuz3qeWsc0bdA"),
        JsonHelper.readFromClasspath("/stream/v2_stream_attributes.json"));

    V2StreamAttributes stream = this.service.getStream("p9B316LKDto7iOECc8Xuz3qeWsc0bdA");

    assertEquals("p9B316LKDto7iOECc8Xuz3qeWsc0bdA", stream.getId());
    assertEquals("INTERNAL", stream.getOrigin());
  }

  @Test
  void getStreamInfoTestFailed() {
    this.mockApiClient.onGet(400, V2_STREAM_INFO.replace("{sid}", "p9B316LKDto7iOECc8Xuz3qeWsc0bdA"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.getStream("p9B316LKDto7iOECc8Xuz3qeWsc0bdA"));
  }

  @Test
  void createAdminIMorMIMTest() {
    this.mockApiClient.onPost(V1_IM_CREATE_ADMIN, "{\n\"id\": \"xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA\"\n}");

    Stream stream = this.service.createInstantMessageAdmin(Arrays.asList(7215545078541L, 7215545078461L));

    assertEquals("xhGxbTcvTDK6EIMMrwdOrX___quztr2HdA", stream.getId());
  }

  @Test
  void createAdminIMorMIMTestFailed() {
    this.mockApiClient.onPost(400, V1_IM_CREATE_ADMIN, "{}");

    assertThrows(ApiRuntimeException.class,
        () -> this.service.createInstantMessageAdmin(Arrays.asList(7215545078541L, 7215545078461L)));
  }

  @Test
  void updateIMTest() throws IOException {
    this.mockApiClient.onPost(V1_IM_UPDATE.replace("{id}", "usnBKBkH_BVrGOiVpaupEH___okFfE7QdA"),
        JsonHelper.readFromClasspath("/stream/im_info.json"));

    V1IMAttributes attributes = new V1IMAttributes();
    attributes.setPinnedMessageId("vd7qwNb6hLoUV0BfXXPC43___oPIvkwJbQ");
    V1IMDetail imDetail = this.service.updateInstantMessage("usnBKBkH_BVrGOiVpaupEH___okFfE7QdA", attributes);

    assertEquals("usnBKBkH_BVrGOiVpaupEH___okFfE7QdA", imDetail.getImSystemInfo().getId());
    assertEquals("vd7qwNb6hLoUV0BfXXPC43___oPIvkwJbQ", imDetail.getV1IMAttributes().getPinnedMessageId());
  }

  @Test
  void updateIMTest_base64() throws IOException, ApiException {
    this.mockApiClient.onPost(V1_IM_UPDATE.replace("{id}", "bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"),
        JsonHelper.readFromClasspath("/stream/im_info.json"));

    V1IMAttributes attributes = new V1IMAttributes();
    attributes.setPinnedMessageId(fromUrlSafeId("wxv6PbPtTSvnjOwVLCss93___oMVTf_AbQ"));
    this.service.updateInstantMessage(fromUrlSafeId("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA"), attributes);

    final ArgumentCaptor<V1IMAttributes> attributesArgumentCaptor = ArgumentCaptor.forClass(V1IMAttributes.class);
    final ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);

    verify(streamsApi).v1ImIdUpdatePost(idArgumentCaptor.capture(), any(String.class),
        attributesArgumentCaptor.capture());

    assertEquals("bjHSiY4iz3ar4iIh6-VzCX___peoM7cPdA", idArgumentCaptor.getValue());
    assertEquals("wxv6PbPtTSvnjOwVLCss93___oMVTf_AbQ", attributesArgumentCaptor.getValue().getPinnedMessageId());
  }

  @Test
  void updateIMTestFail() {
    this.mockApiClient.onPost(400, V1_IM_UPDATE.replace("{id}", "p9B316LKDto7iOECc8Xuz3qeWsc0bdA"), "{}");

    assertThrows(ApiRuntimeException.class,
        () -> this.service.updateInstantMessage("p9B316LKDto7iOECc8Xuz3qeWsc0bdA", new V1IMAttributes()));
  }

  @Test
  void getIMInfoTest() throws IOException {
    this.mockApiClient.onGet(V1_IM_INFO.replace("{id}", "usnBKBkH_BVrGOiVpaupEH___okFfE7QdA"),
        JsonHelper.readFromClasspath("/stream/im_info.json"));

    V1IMDetail imDetail = this.service.getInstantMessageInfo("usnBKBkH_BVrGOiVpaupEH___okFfE7QdA");

    assertEquals("usnBKBkH_BVrGOiVpaupEH___okFfE7QdA", imDetail.getImSystemInfo().getId());
    assertEquals("vd7qwNb6hLoUV0BfXXPC43___oPIvkwJbQ", imDetail.getV1IMAttributes().getPinnedMessageId());
  }

  @Test
  void getIMInfoTestFail() {
    this.mockApiClient.onGet(400, V1_IM_INFO.replace("{id}", "p9B316LKDto7iOECc8Xuz3qeWsc0bdA"), "{}");

    assertThrows(ApiRuntimeException.class,
        () -> this.service.getInstantMessageInfo("p9B316LKDto7iOECc8Xuz3qeWsc0bdA"));
  }

  @Test
  void setRoomActiveAdminTest() throws IOException {
    this.mockApiClient.onPost(V1_ROOM_SET_ACTIVE_ADMIN.replace("{id}", "HNmksPVAR6-f14WqKXmqHX___qu8LMLgdA"), JsonHelper
        .readFromClasspath("/stream/room_detail.json"));

    RoomDetail roomDetail = this.service.setRoomActiveAdmin("HNmksPVAR6-f14WqKXmqHX___qu8LMLgdA", true);

    assertTrue(roomDetail.getRoomSystemInfo().getActive());
  }

  @Test
  void setRoomActiveAdminTestFailed() {
    this.mockApiClient.onPost(400, V1_ROOM_SET_ACTIVE_ADMIN.replace("{id}", "HNmksPVAR6-f14WqKXmqHX___qu8LMLgdA"),
        "{}");

    assertThrows(ApiRuntimeException.class,
        () -> this.service.setRoomActiveAdmin("HNmksPVAR6-f14WqKXmqHX___qu8LMLgdA", true));
  }

  @Test
  void listStreamsAdminTest() throws IOException {
    this.mockApiClient.onPost(V2_STREAM_LIST_ADMIN, JsonHelper.readFromClasspath("/stream/v2_admin_stream_list.json"));

    V2AdminStreamList streamList = this.service.listStreamsAdmin(new V2AdminStreamFilter());

    assertEquals(4, streamList.getCount());
    assertEquals("Q2KYGm7JkljrgymMajYTJ3___qcLPr1UdA", streamList.getStreams().get(0).getId());
    assertEquals("_KnoYrMkhEn3H2_8vE0kl3___qb5SANQdA", streamList.getStreams().get(1).getId());
    assertEquals("fBoaBSRUyb5Rq3YgeSqZvX___qbf5IAhdA", streamList.getStreams().get(2).getId());
  }

  @Test
  void listStreamAdminTestFailed() {
    this.mockApiClient.onPost(400, V2_STREAM_LIST_ADMIN, "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.listStreamsAdmin(new V2AdminStreamFilter()));
  }

  @Test
  void listStreamsAdminSkipLimitTest() throws IOException {
    this.mockApiClient.onPost(V2_STREAM_LIST_ADMIN, JsonHelper.readFromClasspath("/stream/v2_admin_stream_list.json"));

    V2AdminStreamList streamList =
        this.service.listStreamsAdmin(new V2AdminStreamFilter(), new PaginationAttribute(0, 100));

    assertEquals(4, streamList.getCount());
    assertEquals("Q2KYGm7JkljrgymMajYTJ3___qcLPr1UdA", streamList.getStreams().get(0).getId());
    assertEquals("_KnoYrMkhEn3H2_8vE0kl3___qb5SANQdA", streamList.getStreams().get(1).getId());
    assertEquals("fBoaBSRUyb5Rq3YgeSqZvX___qbf5IAhdA", streamList.getStreams().get(2).getId());
  }

  @Test
  void listAllStreamsAdminTest() throws IOException {
    this.mockApiClient.onPost(V2_STREAM_LIST_ADMIN, JsonHelper.readFromClasspath("/stream/v2_admin_stream_list.json"));

    List<V2AdminStreamInfo> streamList =
        this.service.listAllStreamsAdmin(new V2AdminStreamFilter()).collect(Collectors.toList());

    assertEquals(4, streamList.size());
    assertEquals("Q2KYGm7JkljrgymMajYTJ3___qcLPr1UdA", streamList.get(0).getId());
    assertEquals("_KnoYrMkhEn3H2_8vE0kl3___qb5SANQdA", streamList.get(1).getId());
    assertEquals("fBoaBSRUyb5Rq3YgeSqZvX___qbf5IAhdA", streamList.get(2).getId());
  }

  @Test
  void listAllStreamsAdminPaginationTest() throws IOException {
    this.mockApiClient.onPost(V2_STREAM_LIST_ADMIN, JsonHelper.readFromClasspath("/stream/v2_admin_stream_list.json"));

    List<V2AdminStreamInfo> streamList =
        this.service.listAllStreamsAdmin(new V2AdminStreamFilter(), new StreamPaginationAttribute(100, 100))
            .collect(Collectors.toList());

    assertEquals(4, streamList.size());
    assertEquals("Q2KYGm7JkljrgymMajYTJ3___qcLPr1UdA", streamList.get(0).getId());
    assertEquals("_KnoYrMkhEn3H2_8vE0kl3___qb5SANQdA", streamList.get(1).getId());
    assertEquals("fBoaBSRUyb5Rq3YgeSqZvX___qbf5IAhdA", streamList.get(2).getId());
  }

  @Test
  void listStreamMembersTest() throws IOException {
    this.mockApiClient.onGet(V1_STREAM_MEMBERS.replace("{id}", "1234"),
        JsonHelper.readFromClasspath("/stream/v2_membership_list.json"));

    V2MembershipList membersList = this.service.listStreamMembers("1234");

    assertEquals(2, membersList.getCount());
    assertEquals(1485366753320L, membersList.getMembers().get(0).getJoinDate());
    assertEquals(1485366753279L, membersList.getMembers().get(1).getJoinDate());
  }

  @Test
  void listStreamMembersTestFailed() {
    this.mockApiClient.onGet(400, V1_STREAM_MEMBERS.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.listStreamMembers("1234"));
  }

  @Test
  void listStreamMembersSkipLimitTest() throws IOException {
    this.mockApiClient.onGet(V1_STREAM_MEMBERS.replace("{id}", "1234"),
        JsonHelper.readFromClasspath("/stream/v2_membership_list.json"));

    V2MembershipList membersList = this.service.listStreamMembers("1234", new PaginationAttribute(0, 100));

    assertEquals(2, membersList.getCount());
    assertEquals(1485366753320L, membersList.getMembers().get(0).getJoinDate());
    assertEquals(1485366753279L, membersList.getMembers().get(1).getJoinDate());
  }

  @Test
  void listAllStreamMembersTest() throws IOException {
    this.mockApiClient.onGet(V1_STREAM_MEMBERS.replace("{id}", "1234"),
        JsonHelper.readFromClasspath("/stream/v2_membership_list.json"));

    List<V2MemberInfo> membersList = this.service.listAllStreamMembers("1234").collect(Collectors.toList());

    assertEquals(2, membersList.size());
    assertEquals(1485366753320L, membersList.get(0).getJoinDate());
    assertEquals(1485366753279L, membersList.get(1).getJoinDate());
  }

  @Test
  void listAllStreamMembersPaginationTest() throws IOException {
    this.mockApiClient.onGet(V1_STREAM_MEMBERS.replace("{id}", "1234"),
        JsonHelper.readFromClasspath("/stream/v2_membership_list.json"));

    List<V2MemberInfo> membersList =
        this.service.listAllStreamMembers("1234", new StreamPaginationAttribute(100, 100)).collect(Collectors.toList());

    assertEquals(2, membersList.size());
    assertEquals(1485366753320L, membersList.get(0).getJoinDate());
    assertEquals(1485366753279L, membersList.get(1).getJoinDate());
  }

  @Test
  void listRoomMemberTest() {
    this.mockApiClient.onGet(V2_ROOM_MEMBERS.replace("{id}", "1234"), "[\n"
        + "  {\n"
        + "    \"id\": 7078106103900,\n"
        + "    \"owner\": false,\n"
        + "    \"joinDate\": 1461430710531\n"
        + "  },\n"
        + "  {\n"
        + "    \"id\": 7078106103809,\n"
        + "    \"owner\": true,\n"
        + "    \"joinDate\": 1461426797875\n"
        + "  }\n"
        + "]");

    List<MemberInfo> memberInfos = this.service.listRoomMembers("1234");

    assertEquals(2, memberInfos.size());
    assertEquals(7078106103900L, memberInfos.get(0).getId());
    assertFalse(memberInfos.get(0).getOwner());
    assertEquals(7078106103809L, memberInfos.get(1).getId());
    assertTrue(memberInfos.get(1).getOwner());
  }

  @Test
  void listRoomMemberTestFailed() {
    this.mockApiClient.onGet(400, V2_ROOM_MEMBERS.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.listRoomMembers("1234"));
  }

  @Test
  void addMemberToRoomTest() throws ApiException {
    this.mockApiClient.onPost(V1_ADD_MEMBER_TO_ROOM.replace("{id}", "1234"), "{}");

    this.service.addMemberToRoom(12345L, "1234");

    verify(this.spyRoomMembershipApi).v1RoomIdMembershipAddPost(eq("1234"), eq("1234"), eq(new UserId().id(12345L)));
  }

  @Test
  void addMemberToRoomTestFailed() {
    this.mockApiClient.onPost(400, V1_ADD_MEMBER_TO_ROOM.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.addMemberToRoom(12345L, "1234"));
  }

  @Test
  void removeMemberFromRoomTest() throws ApiException {
    this.mockApiClient.onPost(V1_REMOVE_MEMBER_FROM_ROOM.replace("{id}", "1234"), "{}");

    this.service.removeMemberFromRoom(12345L, "1234");

    verify(this.spyRoomMembershipApi).v1RoomIdMembershipRemovePost(eq("1234"), eq("1234"), eq(new UserId().id(12345L)));
  }

  @Test
  void removeMemberFromRoomTestFailed() {
    this.mockApiClient.onPost(400, V1_REMOVE_MEMBER_FROM_ROOM.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.removeMemberFromRoom(12345L, "1234"));
  }

  @Test
  void shareTest() throws IOException {
    this.mockApiClient.onPost(V3_SHARE.replace("{sid}", "1234"), JsonHelper.readFromClasspath("/stream/v3_share.json"));

    V2Message message = this.service.share("1234", new ShareContent());

    assertEquals(message.getId(), "HsaTBf7ClJRWvzNWaCp_4H___qlrh4WVdA");
    assertEquals(message.getFromUserId(), 7696581430532L);
    assertEquals(message.getStreamId(), "7w68A8sAG_qv1GwVc9ODzX___ql_RJ6zdA");
  }

  @Test
  void shareTestFailed() {
    this.mockApiClient.onPost(400, V3_SHARE.replace("{sid}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.share("1234", new ShareContent()));
  }

  @Test
  void demoteUserTest() throws ApiException {
    this.mockApiClient.onPost(V1_DEMOTE_MEMBER.replace("{id}", "1234"), "{}");

    this.service.demoteUserToRoomParticipant(12345L, "1234");

    verify(this.spyRoomMembershipApi).v1RoomIdMembershipDemoteOwnerPost(eq("1234"), eq("1234"),
        eq(new UserId().id(12345L)));
  }

  @Test
  void demoteUserTestFailed() {
    this.mockApiClient.onPost(400, V1_DEMOTE_MEMBER.replace("{id}", "1234"), "{}");

    assertThrows(ApiRuntimeException.class, () -> this.service.demoteUserToRoomParticipant(12345L, "1234"));
  }

  @Test
  void promoteUserTest() throws ApiException {
    this.mockApiClient.onPost(V1_PROMOTE_MEMBER.replace("{id}", "1234"), "{}");

    this.service.promoteUserToRoomOwner(12345L, "1234");

    verify(this.spyRoomMembershipApi).v1RoomIdMembershipPromoteOwnerPost(eq("1234"), eq("1234"),
        eq(new UserId().id(12345L)));
  }
}
