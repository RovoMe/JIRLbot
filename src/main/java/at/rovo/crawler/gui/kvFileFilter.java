package at.rovo.crawler.gui;

import java.io.File;
import java.io.FileFilter;

public class kvFileFilter implements FileFilter
{
    @Override
    public boolean accept(File pathname)
    {
        return pathname.getName().endsWith("kv");
    }

}
