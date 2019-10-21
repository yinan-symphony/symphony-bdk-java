package com.symphony.ms.songwriter.command.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth")
public class OAuthController {
  private OAuthAuthenticationProvider authProvider;

  public OAuthController(OAuthAuthenticationProvider authProvider) {
    this.authProvider = authProvider;
  }

  @GetMapping
  public ResponseEntity<String> receiveCode(
      @RequestParam(value="code") String code,
      @RequestParam(value="state") String userId) {

    authProvider.authorizeCode(code, userId);

    return ResponseEntity.ok().build();
  }

}
