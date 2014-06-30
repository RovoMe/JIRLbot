package at.rovo.crawler.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import at.rovo.caching.drum.event.DiskWriterEvent;
import at.rovo.caching.drum.event.DiskWriterStateUpdate;
import at.rovo.caching.drum.event.DrumEvent;
import at.rovo.caching.drum.event.InMemoryBufferEvent;
import at.rovo.caching.drum.event.InMemoryBufferStateUpdate;
import at.rovo.caching.drum.event.MergerStateUpdate;

@SuppressWarnings("unused")
public class DrumEventTab extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 9061283492546786362L;
	
	private JButton[] memoryBuffer = null;
	private JButton[] bucketFile = null;
	private JLabel merger = null;
	private JLabel mainMerger = null;
	private JLabel statusBarMergerLabel = null;
	private JLabel statusBarMainMergerLabel = null;
	
	public DrumEventTab(int numBuckets)
	{
		this.memoryBuffer = new JButton[numBuckets];
		for (int i=0; i<numBuckets; i++)
		{
			this.memoryBuffer[i] = new JButton(""+i);
			this.memoryBuffer[i].setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			this.memoryBuffer[i].setBackground(Color.WHITE);
		}
		this.bucketFile = new JButton[numBuckets];
		for (int i=0; i<numBuckets; i++)
		{
			this.bucketFile[i] = new JButton(""+i);
			this.bucketFile[i].setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			this.bucketFile[i].setBackground(Color.WHITE);
		}
		this.merger = new JLabel("");
		this.mainMerger = new JLabel("");
		
		this.setLayout(new BorderLayout());
		
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout());
		this.add(content, BorderLayout.CENTER);
		this.add(this.initStatusBar(), BorderLayout.SOUTH);
		
		JPanel scrollArea = new JPanel();
		scrollArea.setLayout(new BorderLayout());
		scrollArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		content.add(new JScrollPane(scrollArea));
		
		JPanel bufferAndBucketSection = new JPanel();
		bufferAndBucketSection.setLayout(new GridLayout(2,1));
		bufferAndBucketSection.add(this.initBufferSection(this.memoryBuffer));
		bufferAndBucketSection.add(this.initBucketFileSection(this.bucketFile));
		
		scrollArea.add(bufferAndBucketSection, BorderLayout.CENTER);
		scrollArea.add(this.initMergerSection(), BorderLayout.SOUTH);
	}
	
	private JPanel initBufferSection(JButton[] memoryBuffer)
	{
		// create a title border
		JPanel infoSection = new JPanel();
		infoSection.setLayout(new BorderLayout());
		infoSection.setBorder(BorderFactory.createTitledBorder("Memory Buffer"));
		
		// create a inner border which has a 10 pixel boundary on every end
		JPanel innerBorder = new JPanel();
		innerBorder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridLayout gridLayout = new GridLayout();
		gridLayout.setColumns(20);
		gridLayout.setRows(memoryBuffer.length/20);
		innerBorder.setLayout(gridLayout);
		
		for (JButton button : memoryBuffer)
		{
			innerBorder.add(button);
		}
		
		infoSection.add(innerBorder, BorderLayout.CENTER);
		infoSection.add(this.initBufferSectionLegend(), BorderLayout.SOUTH);
		
		return infoSection;
	}
	
	private JPanel initBufferSectionLegend()
	{
		// create a title border
		JPanel infoSection = new JPanel();
		infoSection.setLayout(new BorderLayout());
		infoSection.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		JPanel legend = new JPanel();
		legend.setLayout(new BorderLayout());
		legend.setBorder(BorderFactory.createTitledBorder("Legend"));
		
		// create a inner border which has a 10 pixel boundary on every end
		JPanel innerBorder = new JPanel();
		innerBorder.setLayout(new GridLayout(1,3));
		innerBorder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		JButton empty = new JButton("Empty");
		empty.setBackground(Color.WHITE);
		JButton withinLimit = new JButton("Within limit");
		withinLimit.setBackground(Color.YELLOW);
		JButton full = new JButton("Size exceeded");
		full.setBackground(Color.RED);
		
		innerBorder.add(empty);
		innerBorder.add(withinLimit);
		innerBorder.add(full);
		
		legend.add(innerBorder, BorderLayout.CENTER);
		infoSection.add(legend, BorderLayout.CENTER);
		
		return infoSection;
	}
	
	private JPanel initBucketFileSection(JButton[] bucketFile)
	{
		// create a title border
		JPanel infoSection = new JPanel();
		infoSection.setLayout(new BorderLayout());
		infoSection.setBorder(BorderFactory.createTitledBorder("Bucket File"));
		
		// create a inner border which has a 10 pixel boundary on every end
		JPanel innerBorder = new JPanel();
		innerBorder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridLayout gridLayout = new GridLayout();
		gridLayout.setColumns(20);
		gridLayout.setRows(memoryBuffer.length/20);
		innerBorder.setLayout(gridLayout);
		
		for (JButton bucket : bucketFile)
		{
			innerBorder.add(bucket);
		}
		
		infoSection.add(innerBorder, BorderLayout.CENTER);
		infoSection.add(this.initBucketSectionLegend(), BorderLayout.SOUTH);
		
		return infoSection;
	}
	
	private JPanel initBucketSectionLegend()
	{
		// create a title border
		JPanel infoSection = new JPanel();
		infoSection.setLayout(new BorderLayout());
		infoSection.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		JPanel legend = new JPanel();
		legend.setLayout(new BorderLayout());
		legend.setBorder(BorderFactory.createTitledBorder("Legend"));
		
		// create a inner border which has a 10 pixel boundary on every end
		JPanel innerBorder = new JPanel();
		innerBorder.setLayout(new GridLayout(1,7));
		innerBorder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		
		JButton empty = new JButton("Empty");
		empty.setBackground(Color.WHITE);
		JButton waitingOnData = new JButton("Waiting on Data");
		waitingOnData.setBackground(Color.YELLOW);
		JButton dataReceived = new JButton("Data received");
		dataReceived.setBackground(Color.CYAN);
		JButton waitingOnLock = new JButton("Waiting on Lock");
		waitingOnLock.setBackground(Color.ORANGE);
		JButton writing = new JButton("Writing");
		writing.setBackground(Color.GREEN);
		JButton finished = new JButton("Finished");
		finished.setBackground(Color.GRAY);
		JButton error = new JButton("Error");
		error.setBackground(Color.RED);
		
		innerBorder.add(empty);
		innerBorder.add(waitingOnData);
		innerBorder.add(dataReceived);
		innerBorder.add(waitingOnLock);
		innerBorder.add(writing);
		innerBorder.add(finished);
		innerBorder.add(error);
		
		infoSection.add(innerBorder, BorderLayout.CENTER);
		
		return infoSection;
	}
	
	private JPanel initMergerSection()
	{
		// create a title border
		JPanel infoSection = new JPanel();
		infoSection.setLayout(new BorderLayout());
		infoSection.setBorder(BorderFactory.createTitledBorder("Merger"));
		
		// create a inner border which has a 10 pixel boundary on every end
		JPanel innerBorder = new JPanel();
		innerBorder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		innerBorder.setLayout(new BorderLayout());
		
		innerBorder.add(this.merger, BorderLayout.NORTH);
		innerBorder.add(this.mainMerger, BorderLayout.SOUTH);
		
		infoSection.add(innerBorder, BorderLayout.NORTH);
		
		infoSection.setSize(infoSection.getSize().width, 20);
		
		return infoSection;
	}
	
	private JPanel initStatusBar()
	{
		JPanel statusBar = new JPanel();
		
		statusBar.setBorder(BorderFactory.createCompoundBorder());
		statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
		
		this.statusBarMergerLabel = new JLabel("Main-Thread merger: ");
		this.statusBarMergerLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusBar.add(this.statusBarMergerLabel);
		

		statusBar.add(new JSeparator(JSeparator.VERTICAL));
		
		this.statusBarMainMergerLabel = new JLabel("Merger thread: ");
		this.statusBarMainMergerLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusBar.add(this.statusBarMainMergerLabel);
				
		return statusBar;
	}

	public void stateChanged(DrumEvent<? extends DrumEvent<?>> event)
	{
		if (event instanceof InMemoryBufferStateUpdate)
		{
			InMemoryBufferStateUpdate stateUpdate = (InMemoryBufferStateUpdate)event;
			int bucketId = stateUpdate.getBucketId();
			switch (stateUpdate.getState())
			{
				case EMPTY:
					this.memoryBuffer[bucketId].setBackground(Color.WHITE);
					this.memoryBuffer[bucketId].setToolTipText("Written: key/value: 0; auxiliary data: 0");
					break;
				case WITHIN_LIMIT:
					this.memoryBuffer[bucketId].setBackground(Color.YELLOW);
					break;
				case EXCEEDED_LIMIT:
					this.memoryBuffer[bucketId].setBackground(Color.RED);
					break;
			}
//			if (this.isVisible())
				this.memoryBuffer[bucketId].repaint();
		}
		else if (event instanceof DiskWriterStateUpdate)
		{
//			System.out.println("Received drum event: "+event);
			
			DiskWriterStateUpdate stateUpdate = (DiskWriterStateUpdate)event;
			int bucketId = stateUpdate.getBucketId();
			if (!event.getThread().getName().endsWith("-Writer-"+(bucketId)))
				return;
			switch (stateUpdate.getState())
			{
				case EMPTY:
					this.bucketFile[bucketId].setBackground(Color.WHITE);
					break;
				case WAITING_ON_DATA:
					this.bucketFile[bucketId].setBackground(Color.YELLOW);
					break;
				case DATA_RECEIVED:
					this.bucketFile[bucketId].setBackground(Color.CYAN);
					break;
				case WAITING_ON_LOCK:
					this.bucketFile[bucketId].setBackground(Color.ORANGE);
					break;
				case WRITING:
					this.bucketFile[bucketId].setBackground(Color.GREEN);
					break;
				case FINISHED:
					this.bucketFile[bucketId].setBackground(Color.GRAY);
					break;
				case FINISHED_WITH_ERROR:
					this.bucketFile[bucketId].setBackground(Color.RED);
					break;
			}
			if (this.isVisible())
				this.bucketFile[bucketId].repaint();
		}
		else if (event instanceof MergerStateUpdate)
		{
			MergerStateUpdate stateUpdate = (MergerStateUpdate)event;
			if (!event.getThread().getName().endsWith("-Merger"))
			{
				switch(stateUpdate.getState())
				{
					case WAITING_ON_MERGE_REQUEST:
						this.mainMerger.setBackground(Color.YELLOW);
						this.mainMerger.setText("Waiting on merge request");
						this.statusBarMainMergerLabel.setText("Main-Thread merger: Waiting on merge request");
						break;
					case MERGE_REQUESTED:
						this.mainMerger.setBackground(Color.CYAN);
						this.mainMerger.setText("Merge requested");
						this.statusBarMainMergerLabel.setText("Main-Thread merger: Merge requested");
						break;
					case WAITING_ON_LOCK:
						this.mainMerger.setBackground(Color.ORANGE);
						this.mainMerger.setText("Waiting on Lock of bucket writer "+stateUpdate.getBucketId());
						this.statusBarMainMergerLabel.setText("Main-Thread merger: Waiting on Lock of bucket writer "+stateUpdate.getBucketId());
						break;
					case MERGING:
						this.mainMerger.setBackground(Color.GREEN);
						this.mainMerger.setText("Merging content of bucket file "+stateUpdate.getBucketId()+" into disk storage");
						this.statusBarMainMergerLabel.setText("Main-Thread merger: Merging content of bucket file "+stateUpdate.getBucketId()+" into disk storage");
						break;
					case FINISHED:
						this.mainMerger.setBackground(Color.GRAY);
						this.mainMerger.setText("Work done - Merger finished");
						this.statusBarMainMergerLabel.setText("Main-Thread merger: Work done - Merger finished");
						break;
					case FINISHED_WITH_ERRORS:
						this.mainMerger.setBackground(Color.RED);
						this.mainMerger.setText("Merger finished with an error!");
						this.statusBarMainMergerLabel.setText("Main-Thread merger: Merger finished with an error!");
						break;
				}
				
				if (this.isVisible())
				{
					this.mainMerger.repaint();
					this.statusBarMainMergerLabel.repaint();
				}
			}
			else
			{
				switch(stateUpdate.getState())
				{
					case WAITING_ON_MERGE_REQUEST:
						this.merger.setBackground(Color.YELLOW);
						this.merger.setText("Waiting on merge request");
						this.statusBarMergerLabel.setText("Merger thread: Waiting on merge request");
						break;
					case MERGE_REQUESTED:
						this.merger.setBackground(Color.CYAN);
						this.merger.setText("Merge requested");
						this.statusBarMergerLabel.setText("Merger thread: Merge requested");
						break;
					case WAITING_ON_LOCK:
						this.merger.setBackground(Color.ORANGE);
						this.merger.setText("Waiting on Lock of bucket writer "+stateUpdate.getBucketId());
						this.statusBarMergerLabel.setText("Merger thread: Waiting on Lock of bucket writer "+stateUpdate.getBucketId());
						break;
					case MERGING:
						this.merger.setBackground(Color.GREEN);
						this.merger.setText("Merging content of bucket file "+stateUpdate.getBucketId()+" into disk storage");
						this.statusBarMergerLabel.setText("Merger thread: Merging content of bucket file "+stateUpdate.getBucketId()+" into disk storage");
						break;
					case FINISHED:
						this.merger.setBackground(Color.GRAY);
						this.merger.setText("Work done - Merger finished");
						this.statusBarMergerLabel.setText("Merger thread: Work done - Merger finished");
						break;
					case FINISHED_WITH_ERRORS:
						this.merger.setBackground(Color.RED);
						this.merger.setText("Merger finished with an error!");
						this.statusBarMergerLabel.setText("Merger thread: Merger finished with an error!");
						break;
				}
				
				if (this.isVisible())
				{
					this.merger.repaint();
					this.statusBarMergerLabel.repaint();
				}
			}
		}
	}

	public void actionPerformed(DrumEvent<? extends DrumEvent<?>> event)
	{
		if (event instanceof InMemoryBufferEvent)
		{
			InMemoryBufferEvent bufferEvent = (InMemoryBufferEvent)event;
			int bucketId = bufferEvent.getBucketId();
			
			StringBuffer buffer = new StringBuffer();
			buffer.append("Written: key/value: ");
			buffer.append(bufferEvent.getKVSize());
			buffer.append("; auxiliary data: ");
			buffer.append(bufferEvent.getAuxSize());
			
			this.memoryBuffer[bucketId].setToolTipText(buffer.toString());
		}
		else if (event instanceof DiskWriterEvent)
		{					
			DiskWriterEvent writerEvent = (DiskWriterEvent)event;
			int bucketId = writerEvent.getBucketId();
			
			StringBuffer buffer = new StringBuffer();
			buffer.append("Written: key/value: ");
			buffer.append(writerEvent.getKVBytes());
			buffer.append("; auxiliary data: ");
			buffer.append(writerEvent.getAuxBytes());
			
			this.bucketFile[bucketId].setToolTipText(buffer.toString());
		}
	}
}
