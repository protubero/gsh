package de.gebit.gsh;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Writing tabular formatted text on the console.
 * Holding back output data for 2 seconds to get a calculation basis for the column widths.
 *  
 * To be documented ... 
 * 
 * @author MSchaefer
 *
 */
public class TableWriter {

    public static class Column {

	private String header;
	private Integer size;
	private Integer maxSize;

	public Column minSize(int minSize) {
	    if (minSize < 1) {
		throw new RuntimeException("invalid column min size: " + maxSize);
	    }
	    if (maxSize != null && maxSize.intValue() < minSize) {
		throw new RuntimeException("column max size < min size: " + maxSize + " < " + size);
	    }
	    if (this.size == null || this.size.intValue() < minSize) {
		this.size = minSize;
	    }
	    return this;
	}

	public Column maxSize(int maxSize) {
	    if (maxSize < 1) {
		throw new RuntimeException("invalid column max size: " + maxSize);
	    }
	    if (size != null && size.intValue() > maxSize) {
		throw new RuntimeException("column max size < min size: " + maxSize + " < " + size);
	    }
	    if (this.maxSize == null || this.maxSize.intValue() > maxSize) {
		this.maxSize = maxSize;
	    }
	    return this;
	}

	public Column header(String text) {
	    this.header = text;
	    return this;
	}

	void write(Writer out, Object obj) throws IOException {
	    fixSize();
	    String str = objToStr(obj);
	    if (str.length() == size.intValue()) {
		out.write(str);
	    } else if (str.length() > size.intValue()) {
		out.write(str.substring(0, size.intValue() - 1));
		out.write("~");
	    } else {
		out.write(StringUtils.rightPad(str, size.intValue()));
	    }
	    out.write(" ");
	}

	private void fixSize() {
	    if (header != null) {
		check(header);
	    }
	    if (size == null) {
		if (maxSize != null) {
		    size = maxSize;
		} else {
		    size = 30;
		}
	    }
	}

	String objToStr(Object obj) {
	    String str = null;
	    if (obj == null) {
		str = "";
	    } else {
		str = String.valueOf(obj);
	    }
	    return str;
	}

	void check(Object obj) {
	    String str = objToStr(obj);
	    String[] parts = str.replaceAll("\r", "").split("\n");
	    for (String part  : parts) {
		int valueSize = part.trim().length();
		if (maxSize == null || valueSize < maxSize.intValue()) {
		    if (size == null || size.intValue() < valueSize) {
			size = valueSize;
		    }
		}
	    }
	}
    }

    private List<Column> columns = new ArrayList<>();
    private List<Object[]> rows = new ArrayList<>();
    private Writer out;
    private Long time;
    boolean hasHeader = false;
    private int margin = 2;

    private TableWriter(Writer out) {
	this.out = out;
    }

    public TableWriter() {
	this(new OutputStreamWriter(System.out));
    }

    public Column column() {
	Column col = new Column();
	columns.add(col);
	return col;
    }

    private void checkTime() {
	if (rows == null) {
	    return;
	}
	if (time == null) {
	    time = Long.valueOf(System.currentTimeMillis());
	} else {
	    if (System.currentTimeMillis() - time.longValue() > 1000L * 2) {
		flush();
	    }
	}
    }

    private void check(Object[] row) {
	if (row.length > columns.size()) {
	    throw new RuntimeException("too much cells: " + row.length + " > " + columns.size());
	}
	for (int i = 0; i < row.length; i++) {
	    columns.get(i).check(row[i]);
	}
    }

    private void write(Object[] row) {
	List<String[]> lines = new ArrayList<>();
	
	for (int i = 0; i < row.length; i++) {
	    String str = columns.get(i).objToStr(row[i]);
	    String[] parts = str.replaceAll("\r", "").split("\n");
	    for (int j = 0; j < parts.length; j++) {
		String[] tLine = null;
		if (lines.size() < j + 1) {
		    tLine = new String[row.length];
		    lines.add(tLine);
		} else {
		    tLine = lines.get(j);
		}
		tLine[i] = parts[j];
	    }
	}
	
	lines.stream().forEach(l -> writeLine(l));
    }

    private void writeLine(Object[] row) {
	if (row.length > columns.size()) {
	    throw new RuntimeException("too much cells: " + row.length + " > " + columns.size());
	}
	try {
	    out.write("  ");
	    for (int i = 0; i < row.length; i++) {
		columns.get(i).write(out, row[i]);
	    }
	    out.write(System.lineSeparator());
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    public void row(Object first, Object... objects) {
	Object[] arr = new Object[objects.length + 1];
	arr[0] = first;
	System.arraycopy(objects, 0, arr, 1, objects.length);
	row(arr);
    }

    public void row(Object[] objects) {
	checkTime();
	if (rows == null) {
	    write(objects);
	} else {
	    rows.add(objects);
	}
    }

    public void row(List<Object> objects) {
	row(objects.toArray(new Object[objects.size()]));
    }

    public void row(Object first) {
	row(new Object[] { first });
    }

    public void flush() {
	if (rows != null) {
	    rows.forEach(r -> check(r));
	    columns.forEach(c -> hasHeader = hasHeader || c.header != null);

	    if (hasHeader) {
		String[] headerRow = new String[columns.size()];
		for (int i = 0; i < columns.size(); i++) {
		    Column col = columns.get(i);
		    if (col.header == null) {
			headerRow[i] = "";
		    } else {
			headerRow[i] = col.header;
		    }
		}
		write(headerRow);
		writeBar();
	    }

	    rows.forEach(r -> write(r));
	    rows = null;
	}
	try {
	    out.flush();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    private void writeBar() {
	int charCount = columns.size() - 1;
	for (int i = 0; i < columns.size(); i++) {
	    Column col = columns.get(i);
	    col.fixSize();
	    charCount += col.size.intValue();
	}
	try {
	    out.write("  ");
	    out.write(StringUtils.repeat('-', charCount));
	    out.write(System.lineSeparator());
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    public static void main(String[] args) {
	TableWriter tw = new TableWriter(new OutputStreamWriter(System.out));

	tw.column().header("eins");
	tw.column().header("zwei");
	tw.column().header("drei");

	tw.row("eins", "zwei", "drei\r\nnext");
	tw.row("eins", "zwgfgei", "drei");
	tw.row("eins", "zwei", "drefdgi");

	tw.flush();
    }
}
