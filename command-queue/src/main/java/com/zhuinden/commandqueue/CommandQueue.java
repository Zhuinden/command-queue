package com.zhuinden.commandqueue;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A class that can be used to send events, and events are enqueued while there is no observer.
 *
 * Allows only a single receiver at a time. Enqueued events are emitted on registration.
 *
 * @param <T> the type of the event
 */
public class CommandQueue<T> {
    private final ConcurrentLinkedQueue<T> queuedEvents = new ConcurrentLinkedQueue<>();

    public interface Receiver<T> {
        void receiveCommand(@NonNull T command);
    }

    private Receiver<T> receiver;

    /**
     * Sets the receiver. If there are any enqueued events, the receiver will receive them when set.
     *
     * @param receiver the event receiver
     */
    public void setReceiver(@Nullable final Receiver<T> receiver) {
        this.receiver = receiver;
        if(receiver != null) {
            List<T> copy = new ArrayList<>(queuedEvents);
            if(!copy.isEmpty()) {
                queuedEvents.clear();
                for(T event : copy) {
                    sendEvent(event);
                }
            }
        }
    }

    /**
     * Removes the currently set receiver.
     */
    public void detachReceiver() {
        this.receiver = null;
    }

    /**
     * Sends the event to the receiver. If there is no receiver, then the event is enqueued until a new receiver is set.
     *
     * @param event the event
     */
    public void sendEvent(@NonNull final T event) {
        if(event == null) {
            throw new IllegalArgumentException("Null value is not allowed as an event");
        }
        if(receiver == null) {
            queuedEvents.add(event);
        } else {
            receiver.receiveCommand(event);
        }
    }
}