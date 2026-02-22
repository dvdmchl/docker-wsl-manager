package org.dreamabout.sw.dockerwslmanager.logic;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class VolumePathResolverTest {

    private final VolumePathResolver resolver = new VolumePathResolver("Ubuntu");

    @Test
    void testResolveNamedVolumePath() {
        Optional<String> path = resolver.resolveNamedVolumePath("my-vol");
        assertTrue(path.isPresent());
        assertEquals("\\\\wsl.localhost\\Ubuntu\\var\\lib\\docker\\volumes\\my-vol\\_data", path.get());
    }

    @Test
    void testResolveBindMountPath_Windows() {
        String winPath = "C:\\Users\\test\\data";
        Optional<String> path = resolver.resolveBindMountPath(winPath);
        assertTrue(path.isPresent());
        assertEquals(winPath, path.get());
    }

    @Test
    void testResolveBindMountPath_Mnt() {
        String linuxPath = "/mnt/c/Users/test/data";
        Optional<String> path = resolver.resolveBindMountPath(linuxPath);
        assertTrue(path.isPresent());
        assertEquals("c:\\Users\\test\\data", path.get());
    }

    @Test
    void testResolveBindMountPath_LinuxInternal() {
        String linuxPath = "/var/log/app";
        Optional<String> path = resolver.resolveBindMountPath(linuxPath);
        assertTrue(path.isPresent());
        assertEquals("\\\\wsl.localhost\\Ubuntu\\var\\log\\app", path.get());
    }

    @Test
    void testResolveEmpty() {
        assertFalse(resolver.resolveNamedVolumePath("").isPresent());
        assertFalse(resolver.resolveBindMountPath(null).isPresent());
    }
}
