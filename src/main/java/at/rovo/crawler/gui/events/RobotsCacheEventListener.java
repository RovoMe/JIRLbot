package at.rovo.crawler.gui.events;

import at.rovo.caching.drum.event.DrumEvent;

public interface RobotsCacheEventListener
{
    void stateChanged(DrumEvent<? extends DrumEvent<?>> event);

    void actionPerformed(DrumEvent<? extends DrumEvent<?>> event);
}
