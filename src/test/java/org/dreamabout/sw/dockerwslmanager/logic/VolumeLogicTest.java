package org.dreamabout.sw.dockerwslmanager.logic;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerMount;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VolumeLogicTest {

    private final VolumeLogic logic = new VolumeLogic();

    @Test
    void testGroupVolumes_Null() {
        assertTrue(logic.groupVolumes(null).isEmpty());
    }

    @Test
    void testGroupVolumes_Empty() {
        assertTrue(logic.groupVolumes(Collections.emptyList()).isEmpty());
    }

    @Test
    void testGroupVolumes_WithProject() {
        InspectVolumeResponse vol1 = mock(InspectVolumeResponse.class);
        when(vol1.getLabels()).thenReturn(Collections.singletonMap("com.docker.compose.project", "ProjectA"));
        
        InspectVolumeResponse vol2 = mock(InspectVolumeResponse.class);
        when(vol2.getLabels()).thenReturn(Collections.singletonMap("com.docker.compose.project", "ProjectA"));

        List<InspectVolumeResponse> list = Arrays.asList(vol1, vol2);
        Map<String, List<InspectVolumeResponse>> grouped = logic.groupVolumes(list);

        assertEquals(1, grouped.size());
        assertEquals(2, grouped.get("ProjectA").size());
    }

    @Test
    void testGroupVolumes_Ungrouped() {
        InspectVolumeResponse vol1 = mock(InspectVolumeResponse.class);
        when(vol1.getLabels()).thenReturn(Collections.emptyMap());

        List<InspectVolumeResponse> list = Collections.singletonList(vol1);
        Map<String, List<InspectVolumeResponse>> grouped = logic.groupVolumes(list);

        assertEquals(1, grouped.size());
        assertEquals(1, grouped.get(VolumeLogic.UNGROUPED_LABEL).size());
    }

    @Test
    void testExtractVolumeNames() {
        InspectVolumeResponse vol1 = mock(InspectVolumeResponse.class);
        when(vol1.getName()).thenReturn("vol1");
        
        InspectVolumeResponse vol2 = mock(InspectVolumeResponse.class);
        when(vol2.getName()).thenReturn("vol2");

        Set<String> names = logic.extractVolumeNames(Arrays.asList(vol1, vol2));
        
        assertEquals(2, names.size());
        assertTrue(names.contains("vol1"));
        assertTrue(names.contains("vol2"));
    }
    
    @Test
    void testExtractVolumeNames_Null() {
        assertTrue(logic.extractVolumeNames(null).isEmpty());
    }

    @Test
    void testMapVolumesToContainers() {
        ContainerMount mount1 = mock(ContainerMount.class);
        when(mount1.getName()).thenReturn("vol1");

        Container c1 = mock(Container.class);
        when(c1.getNames()).thenReturn(new String[]{"/container1"});
        when(c1.getMounts()).thenReturn(Collections.singletonList(mount1));

        ContainerMount mount2 = mock(ContainerMount.class);
        when(mount2.getName()).thenReturn("vol1");
        ContainerMount mount3 = mock(ContainerMount.class);
        when(mount3.getName()).thenReturn("vol2");

        Container c2 = mock(Container.class);
        when(c2.getNames()).thenReturn(new String[]{"/container2"});
        when(c2.getMounts()).thenReturn(Arrays.asList(mount2, mount3));

        Map<String, List<String>> mapping = logic.mapVolumesToContainers(Arrays.asList(c1, c2));

        assertEquals(2, mapping.size());
        assertEquals(2, mapping.get("vol1").size());
        assertTrue(mapping.get("vol1").contains("container1"));
        assertTrue(mapping.get("vol1").contains("container2"));
        assertEquals(1, mapping.get("vol2").size());
        assertTrue(mapping.get("vol2").contains("container2"));
    }

    @Test
    void testGetRunningContainerVolumeNames() {
        ContainerMount mount1 = mock(ContainerMount.class);
        when(mount1.getName()).thenReturn("vol1");

        Container c1 = mock(Container.class);
        when(c1.getState()).thenReturn("running");
        when(c1.getMounts()).thenReturn(Collections.singletonList(mount1));

        ContainerMount mount2 = mock(ContainerMount.class);
        when(mount2.getName()).thenReturn("vol2");

        Container c2 = mock(Container.class);
        when(c2.getState()).thenReturn("exited");
        when(c2.getMounts()).thenReturn(Collections.singletonList(mount2));

        Set<String> names = logic.getRunningContainerVolumeNames(Arrays.asList(c1, c2));

        assertEquals(1, names.size());
        assertTrue(names.contains("vol1"));
        assertFalse(names.contains("vol2"));
    }
}
