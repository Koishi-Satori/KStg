package top.kkoishi.stg.test.common.ui;

import org.jetbrains.annotations.NotNull;
import top.kkoishi.stg.common.ui.Menu;
import top.kkoishi.stg.logic.GenericSystem;

import java.awt.*;

public final class PauseMenu extends Menu {
    public PauseMenu () {
        super(GenericSystem.STATE_PAUSE);
    }

    @Override
    public void paintBackground (@NotNull Graphics2D r) {

    }
}
