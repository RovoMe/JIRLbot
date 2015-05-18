package at.rovo.crawler.gui;

import at.rovo.crawler.gui.events.STAREventListener;

public class STARTab extends DrumEventTab implements STAREventListener
{
    /**
     *
     */
    private static final long serialVersionUID = -8568831204173115635L;

    public STARTab(int numBuckets)
    {
        super(numBuckets);

        Manager.getInstance().registerForStarEvents(this);
    }
}
