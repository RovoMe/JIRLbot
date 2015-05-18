package at.rovo.crawler.gui;

import java.io.File;
import java.io.FileFilter;

public class auxFileFilter implements FileFilter
{

    @Override
    public boolean accept(File pathname)
    {
        if (pathname.getName().endsWith(".aux"))
        {
            return true;
        }
        return false;
    }

}
