package dev.primeclient.core.modules.qol;

import dev.primeclient.core.module.ModuleManager;
import dev.primeclient.core.keybind.KeybindManager;
import dev.primeclient.core.state.AlwaysDayState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlwaysDayModuleTest {

    private AlwaysDayModule module;

    @BeforeEach
    void setUp() {
        ModuleManager manager = new ModuleManager(new dev.primeclient.core.event.EventBus(), new KeybindManager());
        module = manager.register(new AlwaysDayModule());
    }

    @AfterEach
    void reset() {
        AlwaysDayState.setActive(false);
    }

    @Test
    void togglesAlwaysDayState() {
        module.setEnabled(true);
        assertTrue(AlwaysDayState.active());

        module.setEnabled(false);
        assertFalse(AlwaysDayState.active());
    }
}
