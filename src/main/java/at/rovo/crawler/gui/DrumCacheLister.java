package at.rovo.crawler.gui;

import at.rovo.caching.drum.util.DrumUtils;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

@SuppressWarnings("unused")
public abstract class DrumCacheLister<V extends Serializable, A extends Serializable> extends JPanel
        implements ActionListener, TreeSelectionListener
{
    /**
     *
     */
    private static final long serialVersionUID = 7463923637859399707L;
    private String cache;
    private DefaultMutableTreeNode bucketTree;
    private DefaultMutableTreeNode dataTree;
    private JPanel contentArea = new JPanel();
    private CardLayout cardManager = new CardLayout();
    private Class<V> valueClass = null;
    private Class<A> auxClass = null;

    public DrumCacheLister(String cacheName, Class<V> valueClass, Class<A> auxClass)
    {
        this.cache = cacheName;
        this.bucketTree = new DefaultMutableTreeNode(this.cache);
        this.dataTree = new DefaultMutableTreeNode(this.cache);
        this.valueClass = valueClass;
        this.auxClass = auxClass;

        this.setLayout(new BorderLayout());

        JPanel leftArea = new JPanel();
        JButton bucketButton = new JButton("Bucket Files");
        JButton cacheButton = new JButton("Data Store");

        bucketButton.addActionListener(this);
        cacheButton.addActionListener(this);

        leftArea.add(bucketButton);
        leftArea.add(cacheButton);
        this.add(leftArea, BorderLayout.WEST);

        JTree bucketTree = new JTree(this.bucketTree);
        bucketTree.addTreeSelectionListener(this);
        JTree dataTree = new JTree(this.dataTree);

        this.contentArea.setLayout(this.cardManager);
        this.contentArea.add(new JScrollPane(bucketTree), "Bucket Files");
        this.contentArea.add(new JScrollPane(dataTree), "Data Store");
        this.add(contentArea, BorderLayout.CENTER);

        this.showBucketFiles();
    }

    private void readBucketFiles() throws Exception
    {
        String appDir = System.getProperty("user.dir");
        File cacheDir = new File(appDir + "/cache/" + this.cache);

        if (!cacheDir.isDirectory())
        {
            throw new Exception("Could not find cache directory 'urlSeen'!");
        }

        File[] kvFiles = cacheDir.listFiles(new kvFileFilter());
        File[] auxFiles = cacheDir.listFiles(new auxFileFilter());

        if (kvFiles.length != auxFiles.length)
        {
            throw new Exception("Uneven number of key/value files and auxiliary data files!");
        }

        for (int i = 0; i < kvFiles.length; i++)
        {
            this.bucketTree.add(new DefaultMutableTreeNode(
                    "Bucket " + i + " - " + kvFiles[i].length() + " bytes / " + auxFiles[i].length() + " bytes"));
        }
    }

    private void addBucketFileContent(DefaultMutableTreeNode node, File kvFile, File auxFile)
    {
        RandomAccessFile rkvFile = null;
        RandomAccessFile rauxFile = null;

        try
        {
            rkvFile = new RandomAccessFile(kvFile, "r");
            rauxFile = new RandomAccessFile(auxFile, "r");

            while (rkvFile.getFilePointer() < rkvFile.length())
            {
                StringBuilder buffer = new StringBuilder();

                char op = (char) rkvFile.readByte();
                if ('c' == op)
                {
                    buffer.append("CHECK - ");
                }
                else if ('u' == op)
                {
                    buffer.append("UPDATE - ");
                }
                else if ('b' == op)
                {
                    buffer.append("CHECK+UPDATE - ");
                }
                else
                {
                    buffer.append("UNDEFINED SYMBOL: ");
                    buffer.append(op);
                }

                if (rkvFile.getFilePointer() + 8 < rkvFile.length())
                {
                    long key = rkvFile.readLong();
                    buffer.append("Key: ");
                    buffer.append(key);

                    int valueSize = rkvFile.readInt();
                    if (valueSize > 0)
                    {
                        byte[] valueBytes = new byte[valueSize];
                        rkvFile.read(valueBytes);
                        V value = DrumUtils.deserialize(valueBytes, this.valueClass);
                        buffer.append(" - ");
                        buffer.append("Value: ");
                        buffer.append(value);
                    }
                }
                if (rauxFile.getFilePointer() + 4 < rauxFile.length())
                {
                    int auxSize = rauxFile.readInt();
                    if (auxSize > 0)
                    {
                        byte[] auxBytes = new byte[auxSize];
                        rauxFile.read(auxBytes);
                        A aux = DrumUtils.deserialize(auxBytes, this.auxClass);
                        buffer.append(" - ");
                        buffer.append("Aux: ");
                        buffer.append(aux);
                    }
                }
                node.add(new DefaultMutableTreeNode(buffer.toString()));
            }
        }
        catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (rkvFile != null)
                {
                    rkvFile.close();
                }
                if (rauxFile != null)
                {
                    rauxFile.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void readDataStore() throws Exception
    {
        String appDir = System.getProperty("user.dir");
        File cacheDir = new File(appDir + "/cache/" + this.cache);

        if (!cacheDir.isDirectory())
        {
            throw new Exception("Could not find cache directory 'urlSeen'!");
        }

        File[] dbFiles = cacheDir.listFiles(new dbFileFilter());
        if (dbFiles.length != 1)
        {
            throw new Exception("Invalid number of 'cache.db' files");
        }

        addDataStoreContent(dbFiles[0]);
    }

    private void addDataStoreContent(File dbFile)
    {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("cache.db");

        RandomAccessFile rdbFile = null;
        try
        {
            rdbFile = new RandomAccessFile(dbFile, "r");

            while (rdbFile.getFilePointer() < rdbFile.length())
            {
                StringBuilder buffer = new StringBuilder();
                long key = rdbFile.readLong();
                buffer.append("Key: ");
                buffer.append(key);

                int valueSize = rdbFile.readInt();
                if (valueSize > 0)
                {
                    byte[] valueBytes = new byte[valueSize];
                    rdbFile.read(valueBytes);
                    V value = DrumUtils.deserialize(valueBytes, this.valueClass);
                    buffer.append(" - ");
                    buffer.append("Value: ");
                    buffer.append(value);
                }

                node.add(new DefaultMutableTreeNode(buffer.toString()));
            }
        }
        catch (ClassNotFoundException | IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (rdbFile != null)
            {
                try
                {
                    rdbFile.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        this.dataTree.add(node);
    }

    private void showBucketFiles()
    {
        try
        {
            this.contentArea.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            this.readBucketFiles();
            this.cardManager.show(this.contentArea, "Bucket Files");
            this.contentArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
    }

    private void showDataStore()
    {
        try
        {
            this.contentArea.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            this.readDataStore();
            this.cardManager.show(this.contentArea, "Data Store");
            this.contentArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();
        if ("Bucket Files".equals(command))
        {
            this.showBucketFiles();
        }
        else if ("Data Store".equals(command))
        {
            this.showDataStore();
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent e)
    {
        TreePath treePath = e.getPath();
        if (treePath.getPathCount() == 2)
        {
            this.contentArea.setCursor(new Cursor(Cursor.WAIT_CURSOR));

            String bucketId = treePath.getPathComponent(1).toString();
            int start = "Bucket ".length();
            int id = Integer.parseInt(bucketId.substring(start, bucketId.indexOf(" ", start)));

            String appDir = System.getProperty("user.dir");
            File cacheDir = new File(appDir + "/cache/" + this.cache);

            File[] kvFiles = cacheDir.listFiles(new kvFileFilter());
            File[] auxFiles = cacheDir.listFiles(new auxFileFilter());

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getPathComponent(1);
            this.addBucketFileContent(node, kvFiles[id], auxFiles[id]);

            this.contentArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        else if (treePath.getPathCount() == 1)
        {
            this.contentArea.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            this.bucketTree.removeAllChildren();
            try
            {
                this.readBucketFiles();
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
            }
            this.contentArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }
}