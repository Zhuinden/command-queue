package com.zhuinden.commandqueue;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
     * Returns if the command queue has a receiver.
     *
     * @return whether there is a receiver
     */
    public boolean hasReceiver() {
        return receiver != null;
    }

    private boolean isEmittingEvent = false;

    /**
     * Sets the receiver. If there are any enqueued events, the receiver will receive them when set.
     *
     * @param receiver the event receiver
     */
    public void setReceiver(@Nullable final Receiver<T> receiver) {
        this.receiver = receiver;
        if(receiver != null) {
            while(this.receiver != null && !queuedEvents.isEmpty()) {
                T event = queuedEvents.poll();
                isEmittingEvent = true;
                receiver.receiveCommand(event);
                isEmittingEvent = false;
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
        if(receiver == null || isEmittingEvent) {
            queuedEvents.add(event);
        } else {
            receiver.receiveCommand(event);
        }
    }
}