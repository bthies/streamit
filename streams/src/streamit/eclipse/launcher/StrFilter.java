package streamit.eclipse.launcher;

import org.eclipse.swt.widgets.TableItem;

public class StrFilter {
	
	private TableItem ti;
	private int row;
	
	private String name;
	private String[] fields;
		
	private String inputType;
	private String outputType;

	private String popCount;
	private String pushCount;
	private String peekCount;

	private String stage;
	private int numWork;

	// private int numPhases;
		
	public StrFilter(String n, String[] f) {
		ti = null;
		row = -1;
		name = n;
		fields = f;
		popCount = StreamItLauncherPlugin.NOT_APPLICABLE;
		pushCount = StreamItLauncherPlugin.NOT_APPLICABLE;
		peekCount = StreamItLauncherPlugin.NOT_APPLICABLE;
		stage = StreamItLauncherPlugin.INIT_METHOD;
		numWork = 0;
	}
	
	public String[] toRow() {
		String[] row = new String[StreamItLauncherPlugin.FILTER_VIEW_HEADERS.length];
		row[0] = name;
		row[1] = inputType;
		row[2] = outputType;
		row[3] = getFields();
		row[4] = popCount;
		row[5] = pushCount;
		row[6] = peekCount;
		row[7] = stage;
		row[8] = String.valueOf(numWork);
		return row;
	}
	
	public TableItem getTableItem() {
		return ti;
	}
	
	public int getRow() {
		return row;
	}
	
	public String getName() {
		return name;
	}
	
	public String getFields() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < fields.length; i++) {
			sb.append(fields[i]);
			if (i != fields.length - 1) sb.append("; ");
		}
		return sb.toString();
	}

	public String getInputType() {
		return inputType;
	}
	
	public String getOutputType() {
		return outputType;
	}

	public String getPopCount() {
		return popCount;
	}

	public String getPushCount() {
		return pushCount;
	}
	
	public String getPeekCount() {
		return peekCount;
	}
	
	public String getStage() {
		return stage;
	}
	
	public String getNumWork() {
		return String.valueOf(numWork);
	}
	
	public void setTableItem(TableItem t) {
		ti = t;
	}
	
	public void setRow(int r) {
		row = r;
	}
	
	public void setInputOutputType(boolean setInput, String type) {
		if (setInput) {
			inputType = type;
		} else {
			outputType = type;
		} 
	}
		
	public void setPushCount(String pc) {
		pushCount = pc;
	}
		
	public void setPopCount(String pc) {
		popCount = pc;
	}
		
	public void setPeekCount(String pc) {
		peekCount = pc;
	}
	
	public void setStage(String s) {
		stage = s;
	}
	
	public void worked() {
		numWork++;
	}
}