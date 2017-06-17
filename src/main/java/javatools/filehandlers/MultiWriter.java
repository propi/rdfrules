package javatools.filehandlers;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  

This writer forwards to multiple writers*/

public class MultiWriter extends Writer {

	protected Collection<Writer> writers;

	public MultiWriter(Collection<Writer> writers) {
		this.writers=writers;
	}

	public MultiWriter(Writer... writers) {
		this(Arrays.asList(writers));
	}

	@Override
	public void close() throws IOException {
		for(Writer w: writers) w.close();
	}

	@Override
	public void flush() throws IOException {
		for(Writer w: writers) w.flush();
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		for(Writer w: writers) w.write(cbuf,off,len);
	}
	
	
	@Override
	public void write(int c) throws IOException {
		for(Writer w: writers) w.write(c);
	}
	
	@Override
	public void write(String str) throws IOException {
		for(Writer w: writers) w.write(str);
	}
}
