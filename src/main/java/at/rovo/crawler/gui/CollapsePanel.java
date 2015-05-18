package at.rovo.crawler.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class CollapsePanel extends JPanel
{
    private static final long serialVersionUID = 6968177441850485173L;

    protected final ActionPanel actionPanel;
    protected final Component comp;

    public CollapsePanel(String title, Component component)
    {
        this.comp = component;

        MouseAdapter ma = new MouseAdapter()
        {

            @Override
            public void mousePressed(MouseEvent e)
            {
                ActionPanel ap = (ActionPanel) e.getSource();
                if (ap.target.contains(e.getPoint()))
                {
                    boolean expanded = ap.toggleSelection();
                    comp.setVisible(expanded);
                }
            }
        };

        this.actionPanel = new ActionPanel(title, ma);

        this.setBorder(BorderFactory.createTitledBorder(""));
        this.setLayout(new BorderLayout());
        this.add(this.actionPanel, BorderLayout.NORTH);
        this.add(this.comp, BorderLayout.CENTER);
        this.comp.setVisible(false);
    }
}
