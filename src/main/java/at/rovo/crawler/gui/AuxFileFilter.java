package at.rovo.crawler.gui;

import java.io.File;
import java.io.FileFilter;

public class AuxFileFilter implements FileFilter
{

    @Override
    public boolean accept(File pathname)
    {
        return (pathname.getName().endsWith(".aux"));
    }

}
