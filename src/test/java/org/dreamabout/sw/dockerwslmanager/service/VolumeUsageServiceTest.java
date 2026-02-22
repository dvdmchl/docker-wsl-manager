package org.dreamabout.sw.dockerwslmanager.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VolumeUsageServiceTest {

    private final VolumeUsageService service = new VolumeUsageService();

    @Test
    public void testParseDockerSize() {
        assertEquals(0L, service.parseDockerSize(null));
        assertEquals(0L, service.parseDockerSize(""));
        assertEquals(0L, service.parseDockerSize("N/A"));
        
        assertEquals(9L, service.parseDockerSize("9B"));
        assertEquals(9L, service.parseDockerSize("9 B"));
        
        assertEquals(1024L, service.parseDockerSize("1KB"));
        assertEquals(1024L, service.parseDockerSize("1 KB"));
        assertEquals(1048576L, service.parseDockerSize("1MB"));
        assertEquals(1073741824L, service.parseDockerSize("1GB"));
        
        // From docker system df output examples
        assertEquals(1191853424L, service.parseDockerSize("1.11GB")); // 1.11 * 1024^3
        assertEquals(69824675L, service.parseDockerSize("66.59MB"));  // 66.59 * 1024^2
        assertEquals(924774L, service.parseDockerSize("903.1kB"));   // 903.1 * 1024 (as seen in docker output)
    }
}
