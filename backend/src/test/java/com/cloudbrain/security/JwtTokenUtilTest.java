package com.cloudbrain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloudbrain.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JwtTokenUtilTest {

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(new ObjectMapper(), "jwt-unit-secret", 60, 120);

    @Test
    void generateAndParseTokenRoundTripsActorContext() {
        ActorContext actor = new ActorContext(10L, ActorRole.PATIENT, 5001L, null, "patient01", "患者一");

        String token = jwtTokenUtil.generateToken(actor, "token-id");

        assertThat(jwtTokenUtil.validateToken(token)).isTrue();
        assertThat(jwtTokenUtil.getUserIdFromToken(token)).isEqualTo(10L);
        assertThat(jwtTokenUtil.getRoleFromToken(token)).isEqualTo("PATIENT");
        assertThat(jwtTokenUtil.getTokenIdFromToken(token)).isEqualTo("token-id");
        assertThat(jwtTokenUtil.parseActorContext(token)).contains(actor);
    }

    @Test
    void invalidMalformedOrTamperedTokensAreRejected() {
        ActorContext actor = new ActorContext(10L, ActorRole.PATIENT, 5001L, null, "patient01", "患者一");
        String token = jwtTokenUtil.generateToken(actor, "token-id");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtTokenUtil.validateToken(null)).isFalse();
        assertThat(jwtTokenUtil.validateToken("bad-token")).isFalse();
        assertThat(jwtTokenUtil.validateToken(tampered)).isFalse();
        assertThatThrownBy(() -> jwtTokenUtil.getUserIdFromToken(tampered))
                .isInstanceOf(ApiException.class)
                .hasMessage("invalid token");
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenUtil expiredUtil = new JwtTokenUtil(new ObjectMapper(), "jwt-unit-secret", -1, 120);
        String token = expiredUtil.generateToken(new ActorContext(10L, ActorRole.PATIENT, 5001L, null, "patient01", null));

        assertThat(expiredUtil.validateToken(token)).isFalse();
        assertThat(expiredUtil.parseActorContext(token)).isEmpty();
    }

    @Test
    void hashTokenIsStableSha256Hex() {
        assertThat(jwtTokenUtil.hashToken("refresh-token"))
                .hasSize(64)
                .isEqualTo(jwtTokenUtil.hashToken("refresh-token"))
                .isNotEqualTo(jwtTokenUtil.hashToken("other-token"));
    }
}
