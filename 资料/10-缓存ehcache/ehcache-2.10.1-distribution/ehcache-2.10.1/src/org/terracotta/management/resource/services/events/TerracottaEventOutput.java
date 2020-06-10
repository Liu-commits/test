package org.terracotta.management.resource.services.events;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.server.ChunkedOutput;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

/**
 * This class exists to overcome an defect seen in Chrome where the Jersey EventOutput uses "\n\n" as the
 * chunkDelimeter. The symptom would be a net::ERR_INVALID_CHUNKED_ENCODING on the Javascript EventSource receiving a
 * Server-Sent Event and that event would be malformed.
 * <p/>
 * Another problem this class solves is that it makes {@link #write(Object)} thread-safe: it should be but isn't in
 * ChunkedOutput. To keep performance intact despite thread-safety, messages are only flushed when specifically
 * requested. This means that {@link #flush()} must be called manually otherwise the events will never reach the client.
 *
 * @author gkeim
 * @author Ludovic Orban
 */
public abstract class TerracottaEventOutput extends ChunkedOutput<OutboundEvent> {

  private final Field         flushingField;

  public TerracottaEventOutput() {
    super("\n\n".getBytes(Charset.forName("UTF-8")));

    try {
      flushingField = ChunkedOutput.class.getDeclaredField("flushing");
      flushingField.setAccessible(true);
    } catch (NoSuchFieldException nsfe) {
      throw new RuntimeException(nsfe);
    }
    switchOffAutoFlushing(true);
  }

  private void switchOffAutoFlushing(boolean flushing) {
    try {
      flushingField.set(this, flushing);
    } catch (IllegalAccessException iae) {
      throw new RuntimeException(iae);
    }
  }

  public synchronized void flush() throws IOException {
    try {
      switchOffAutoFlushing(false);
      super.write(null);
    } finally {
      switchOffAutoFlushing(true);
    }
  }

  @Override
  public synchronized void close() throws IOException {
    switchOffAutoFlushing(false);
    super.close();
  }
}
