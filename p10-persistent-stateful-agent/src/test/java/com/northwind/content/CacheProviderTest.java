package com.northwind.content;

import com.embabel.agent.cache.AgentCacheRegion;
import com.embabel.agent.cache.CacheCapability;
import com.embabel.agent.cache.CacheCapabilityException;
import com.embabel.agent.cache.CacheRegionConfig;
import com.embabel.agent.cache.CacheRegions;
import com.embabel.agent.cache.support.CacheBackedAgentProcessSnapshotStore;
import com.embabel.agent.cache.support.SpringCacheAgentCacheProvider;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chapter 68: the cache provider SPI, over Spring's in-memory CacheManager. */
class CacheProviderTest {

    private final SpringCacheAgentCacheProvider provider = new SpringCacheAgentCacheProvider(new ConcurrentMapCacheManager());

    @Test
    void regionsAndCapabilities() {
        System.out.println("regions: " + CacheRegions.AGENT_PROCESS_SNAPSHOTS + ", " + CacheRegions.AGENT_CONTEXTS
                + ", " + CacheRegions.AGENT_PROCESSES_RUNTIME);

        AgentCacheRegion snapshots = provider.getRegion(
                new CacheRegionConfig(CacheRegions.AGENT_PROCESS_SNAPSHOTS, Duration.ZERO, Set.of()));
        System.out.println("spring-cache region capabilities: " + snapshots.getCapabilities());

        var store = new CacheBackedAgentProcessSnapshotStore(snapshots);   // durable snapshots over the region
        assertThat(store).isNotNull();
    }

    @Test
    void aProviderRefusesCapabilitiesItCannotGuarantee() {
        assertThatThrownBy(() -> provider.getRegion(new CacheRegionConfig(CacheRegions.AGENT_PROCESS_SNAPSHOTS,
                Duration.ZERO, Set.of(CacheCapability.ATOMIC_COMPARE_AND_SET))))
                .isInstanceOf(CacheCapabilityException.class)
                .satisfies(e -> System.out.println("refused: " + e.getMessage()));
    }
}
