package gov.nasa.podaac.swodlr.security;

import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/logout")
public class LogoutController {
  @GetMapping
  public Mono<ServerResponse> logoutUser() {
    return ServerResponse
      .ok()
      .cookie(ResponseCookie.from("session", "").build())
      .cookie(ResponseCookie.from("auth_clients", "").build())
      .bodyValue(true);
  }
}
