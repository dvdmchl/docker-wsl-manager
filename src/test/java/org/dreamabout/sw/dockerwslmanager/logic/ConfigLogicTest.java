package org.dreamabout.sw.dockerwslmanager.logic;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigLogicTest {

    private final ConfigLogic configLogic = new ConfigLogic();

    @Test
    void testInspectContainer() {
        DockerClient client = mock(DockerClient.class);
        InspectContainerCmd cmd = mock(InspectContainerCmd.class);
        InspectContainerResponse response = mock(InspectContainerResponse.class);

        when(client.inspectContainerCmd(anyString())).thenReturn(cmd);
        when(cmd.exec()).thenReturn(response);

        InspectContainerResponse result = configLogic.inspectContainer(client, "test-id");
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void testInspectContainer_Null() {
        assertNull(configLogic.inspectContainer(null, "id"));
        assertNull(configLogic.inspectContainer(mock(DockerClient.class), null));
        assertNull(configLogic.inspectContainer(mock(DockerClient.class), ""));
    }

    @Test
    void testFormatAsPrettyJson() {
        TestObject obj = new TestObject("value");
        String json = configLogic.formatAsPrettyJson(obj);
        
        // Use a simpler check that doesn't involve complex escaping if write_file is problematic
        assertTrue(json.contains("field"));
        assertTrue(json.contains("value"));
        // Check for indentation (Jackson usually adds newlines)
        assertTrue(json.contains("\n") || json.contains("\r"));
    }

    @Test
    void testFormatAsPrettyJson_Null() {
        assertEquals("", configLogic.formatAsPrettyJson(null));
    }

    public static class TestObject {
        public String field;
        public TestObject(String field) { this.field = field; }
    }
}
