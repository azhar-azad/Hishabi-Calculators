package dev.azhar.hishabi.platform.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProdProfileEnforcerTest {

    private final ProdProfileEnforcer enforcer = new ProdProfileEnforcer();

    @Test
    void doesNothing_whenRenderEnvNotSet() {
        var env = new MockEnvironment();
        assertThatNoException().isThrownBy(() -> enforcer.postProcessEnvironment(env, null));
    }

    @Test
    void doesNothing_whenRenderTrue_andProdProfileActive() {
        var env = new MockEnvironment();
        env.setProperty("RENDER", "true");
        env.setActiveProfiles("prod");
        assertThatNoException().isThrownBy(() -> enforcer.postProcessEnvironment(env, null));
    }

    @Test
    void failsFast_whenRenderTrue_andDevProfileActive() {
        var env = new MockEnvironment();
        env.setProperty("RENDER", "true");
        env.setActiveProfiles("dev");
        assertThatIllegalStateException()
                .isThrownBy(() -> enforcer.postProcessEnvironment(env, null))
                .withMessageContaining("SPRING_PROFILES_ACTIVE=prod");
    }

    @Test
    void failsFast_whenRenderTrue_andBothProdAndDevActive() {
        var env = new MockEnvironment();
        env.setProperty("RENDER", "true");
        env.setActiveProfiles("prod", "dev");
        assertThatIllegalStateException()
                .isThrownBy(() -> enforcer.postProcessEnvironment(env, null))
                .withMessageContaining("SPRING_PROFILES_ACTIVE=prod");
    }

    @Test
    void failsFast_whenRenderTrue_andNoProfileActive() {
        var env = new MockEnvironment();
        env.setProperty("RENDER", "true");
        assertThatIllegalStateException()
                .isThrownBy(() -> enforcer.postProcessEnvironment(env, null))
                .withMessageContaining("SPRING_PROFILES_ACTIVE=prod");
    }
}
