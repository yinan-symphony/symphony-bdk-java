package com.symphony.bdk.examples;

import static com.symphony.bdk.core.config.BdkConfigLoader.loadFromSymphonyDir;

import com.symphony.bdk.core.SymphonyBdk;
import com.symphony.bdk.core.auth.BotAuthSession;
import com.symphony.bdk.core.service.pagination.model.CursorPaginationAttribute;
import com.symphony.bdk.core.service.user.constant.UserFeature;
import com.symphony.bdk.gen.api.model.ApplicationDetail;
import com.symphony.bdk.gen.api.model.FollowersListResponse;
import com.symphony.bdk.gen.api.model.FollowingListResponse;
import com.symphony.bdk.gen.api.model.Signal;
import com.symphony.bdk.gen.api.model.StreamAttributes;
import com.symphony.bdk.gen.api.model.StreamFilter;
import com.symphony.bdk.gen.api.model.StreamType;
import com.symphony.bdk.gen.api.model.UserFilter;
import com.symphony.bdk.gen.api.model.V2UserDetail;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ServiceExampleMain {

  public static void main(String[] args) throws Exception {

    final SymphonyBdk bdk = new SymphonyBdk(loadFromSymphonyDir("config.yaml"));

    final UserFilter filter = new UserFilter()
        .status(UserFilter.StatusEnum.DISABLED)
        .feature(UserFeature.canCreatePublicRoom.name());

    log.info("Get user by filter");
    final List<V2UserDetail> userDetailList = bdk.users().listUsersDetail(filter);

    log.info("Retrieve {} records", userDetailList.size());
    log.info("First record: ");
    log.info(userDetailList.get(0).getUserAttributes().getUserName());
    log.info("Account type: {}", userDetailList.get(0).getUserAttributes().getAccountType());
    log.info("Company: {}", userDetailList.get(0).getUserAttributes().getCompanyName());
    log.info("Role: {}", userDetailList.get(0).getRoles());

    BotAuthSession oboSession = bdk.obo("hong.le");

    StreamFilter streamFilter = new StreamFilter().addStreamTypesItem(new StreamType().type(StreamType.TypeEnum.IM));
    final List<StreamAttributes> streamsList = bdk.obo(oboSession).streams().listStreams(streamFilter);
    log.info("Streams List: {}", streamsList);

    final List<Signal> signalList = bdk.obo(oboSession).signals().listSignals();
    log.info("Retrieve {} signals", signalList.size());
    log.info("First signal:");
    log.info("Signal name: {}", signalList.get(0).getName());
    log.info("Signal query: {}", signalList.get(0).getQuery());

    bdk.users().followUser(Collections.singletonList(13056700579879L), 13056700579873L);
    bdk.users().unfollowUser(Collections.singletonList(13056700579879L), 13056700579873L);

    final FollowersListResponse followersList =
        bdk.users().listUserFollowers(13056700579873L, new CursorPaginationAttribute(4, 0, 10));
    log.info("Retrieve {} followers", followersList.getCount());

    final FollowingListResponse followingList =
        bdk.users().listUsersFollowing(13056700579873L, new CursorPaginationAttribute(4, 0, 10));
    log.info("Following {} users", followingList.getCount());

    ApplicationDetail appDetail = bdk.applications().getApplication("my-app");
    log.info(appDetail.getApplicationInfo().getAppId());
    log.info(appDetail.getCert());
    log.info(appDetail.getAuthenticationKeys().getCurrent().getKey());
  }
}
