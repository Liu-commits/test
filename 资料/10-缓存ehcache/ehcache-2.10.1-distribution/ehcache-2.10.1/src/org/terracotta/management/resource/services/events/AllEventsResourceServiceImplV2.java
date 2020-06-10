/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.management.resource.services.events;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.events.EventEntityV2;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A resource service for sending events.
 *
 * Since {@link TerracottaEventOutput} does not flushes events by itself, messages are flushed only once every
 * {@link #BATCH_SIZE} times. To prevent events lingering in the queue waiting for the {@link #BATCH_SIZE} quota to be
 * reached, a timer performs a flush every {@link #TIMER_INTERVAL} ms.
 * Finally, since Jersey does not close event outputs itself (even when the TCP connection was dropped,
 * see <a href="https://java.net/jira/browse/JERSEY-2833">JERSEY-2833</a> for details), event outputs get closed if
 * they've been idle for {@link #MAX_IDLE_KEEPALIVE} ms.
 *
 * This must be marked as @Singleton otherwise Jersey will create a new instance of this class per request,
 * creating as many timer threads.
 *
 * @author Ludovic Orban
 */
@Path("/v2/events")
@Singleton
public class AllEventsResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(AllEventsResourceServiceImplV2.class);

  public static final int  BATCH_SIZE     = Integer.getInteger("TerracottaEventOutput.batch_size", 32);
  public static final long TIMER_INTERVAL = Long.getLong("TerracottaEventOutput.timer_interval", 200L);
  public static final long MAX_IDLE_KEEPALIVE = Long.getLong("TerracottaEventOutput.max_idle_keepalive", 15000L);

  private final EventServiceV2 eventService;
  private final Broadcaster broadcaster;

  public AllEventsResourceServiceImplV2() {
    this.eventService = ServiceLocator.locate(EventServiceV2.class);
    this.broadcaster = new Broadcaster();

    Timer flushTimer = new Timer("sse-flush-timer", true);
    flushTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        LOG.debug("There are {} registered SSE event output(s), checking them", broadcaster.outputs.size());
        for (Map.Entry<TerracottaEventOutput, TerracottaEventOutputFlushingMetadata> entry : broadcaster.outputs.entrySet()) {
          TerracottaEventOutput output = entry.getKey();
          TerracottaEventOutputFlushingMetadata metadata = entry.getValue();

          if (metadata.accumulatedIdleTime.addAndGet(TIMER_INTERVAL) >= MAX_IDLE_KEEPALIVE) {
            LOG.debug("A SSE event output has been idle for too long, closing it");
            broadcaster.close(output);
            continue;
          }

          int unflushedCount = metadata.unflushedCount.get();
          if (unflushedCount > 0) {
            LOG.debug("A SSE event output accumulated {} unflushed events during max interval, flushing it", unflushedCount);
            try {
              output.flush();
            } catch (Exception e) {
              LOG.warn("Error flushing SSE from timer, closing event output", e);
              broadcaster.close(output);
            } finally {
              metadata.unflushedCount.addAndGet(-unflushedCount);
            }
            continue;
          }

          LOG.debug("A SSE event output accumulated 0 event during flush interval");
        } // for
      }

    }, TIMER_INTERVAL, TIMER_INTERVAL);
  }

  @GET
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public TerracottaEventOutput getServerSentEvents(@Context UriInfo info, @QueryParam("localOnly") boolean localOnly) {
    LOG.debug(String.format("Invoking AllEventsResourceServiceImplV2.getServerSentEvents: %s", info.getRequestUri()));

    EventServiceListener eventOutput = new EventServiceListener();

    broadcaster.add(eventOutput);
    eventService.registerEventListener(eventOutput, localOnly);

    return eventOutput;
  }


  private class Broadcaster extends SseBroadcaster {

    private final Map<TerracottaEventOutput, TerracottaEventOutputFlushingMetadata> outputs = new ConcurrentHashMap<TerracottaEventOutput, TerracottaEventOutputFlushingMetadata>();

    @Override
    public void onException(final ChunkedOutput<OutboundEvent> chunkedOutput, final Exception exception) {
      LOG.debug("Error writing to OutputEvent", exception);
      close(chunkedOutput);
    }

    @Override
    public <OUT extends ChunkedOutput<OutboundEvent>> boolean add(OUT chunkedOutput) {
      outputs.put((TerracottaEventOutput) chunkedOutput, new TerracottaEventOutputFlushingMetadata());
      return super.add(chunkedOutput);
    }

    @Override
    public void onClose(final ChunkedOutput<OutboundEvent> chunkedOutput) {
      outputs.remove((TerracottaEventOutput) chunkedOutput);
      eventService.unregisterEventListener((EventServiceListener) chunkedOutput);
    }

    public void close(final ChunkedOutput<OutboundEvent> chunkedOutput) {
      try {
        chunkedOutput.close();
      } catch (Exception e) {
        LOG.warn("Error closing SSE event output from timer", e);
      } finally {
        onClose(chunkedOutput);
        remove(chunkedOutput);
      }
    }
  }


  public class EventServiceListener extends TerracottaEventOutput implements EventServiceV2.EventListener {

    @Override
    public synchronized void write(OutboundEvent chunk) throws IOException {
      TerracottaEventOutputFlushingMetadata metadata = broadcaster.outputs.get(this);
      metadata.accumulatedIdleTime.set(0L);
      int unflushedCount = metadata.unflushedCount.incrementAndGet();

      try {
        super.write(chunk);
      } finally {
        if (unflushedCount == BATCH_SIZE) {
          LOG.debug("A SSE event output reached {} unflushed events, flushing it", unflushedCount);
          metadata.unflushedCount.addAndGet(-unflushedCount);
          super.flush();
        } else {
          LOG.debug("A SSE event output accumulating {} unflushed events", unflushedCount);
        }
      }
    }

    @Override
    public void onEvent(EventEntityV2 eventEntity) {
      OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
      eventBuilder.reconnectDelay(100);
      eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE);
      eventBuilder.name(EventEntityV2.class.getSimpleName());
      eventBuilder.data(EventEntityV2.class, eventEntity);
      OutboundEvent event = eventBuilder.build();

      AllEventsResourceServiceImplV2.this.broadcaster.broadcast(event);

      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Event dispatched: {AgentId: %s, Type: %s, ApiVersion: %s, Representables: %s}",
                                eventEntity.getAgentId(), eventEntity.getType(), eventEntity.getApiVersion(),
                                eventEntity.getRootRepresentables()));
      }
    }

    @Override
    public void onError(Throwable throwable) {
      LOG.debug("Error when waiting for management events.", throwable);
      try {
        broadcaster.close(this);
      } catch (Exception e) {
        LOG.warn("Error closing SSE event output", e);
      }
    }
  }


  private static class TerracottaEventOutputFlushingMetadata {
    final AtomicInteger unflushedCount = new AtomicInteger();
    final AtomicLong accumulatedIdleTime = new AtomicLong();
  }

}
