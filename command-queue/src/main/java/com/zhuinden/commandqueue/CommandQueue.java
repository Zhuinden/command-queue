package com.zhuinden.commandqueue;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A class that can be used to send events, and events are enqueued while there is no observer.
 * Allows only a single receiver at a time. Enqueued events are emitted on registration.
 *
 * @param <T> the type of the event
 */
public class CommandQueue<T> {
    private final ConcurrentLinkedQueue<T> queuedEvents = new ConcurrentLinkedQueue<>();
    private boolean paused;
    private boolean distinctOnly;

    public CommandQueue() {
    }

    public CommandQueue(boolean distinctOnly) {
        this.distinctOnly = distinctOnly;
    }

    /**
     * The receiver receives commands when it is set.
     *
     * @param <T> the type of the event
     */
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

    private boolean canEmitEvents() {
        return receiver != null && !isEmittingEvent && !paused;
    }

    private T previouslyEmittedEvent;

    private void emitEvents(@NonNull final Receiver<T> receiver) {
        while(canEmitEvents() && !queuedEvents.isEmpty() && this.receiver == receiver) {
            T event = queuedEvents.poll();
            isEmittingEvent = true;
            sendCommandToReceiver(receiver, event);
            isEmittingEvent = false;
        }

        if(this.receiver != receiver) {
            emitEvents(this.receiver);
        }
    }

    private void sendCommandToReceiver(@NonNull Receiver<T> receiver, @NonNull T event) {
        T previousEvent = previouslyEmittedEvent;
        if(previousEvent != null && event.equals(previousEvent) && distinctOnly) {
            return; // don't send duplicate commands if distinct only
        }
        this.previouslyEmittedEvent = event;
        receiver.receiveCommand(event);
    }

    /**
     * Sets the receiver. If there are any enqueued events, the receiver will receive them when set.
     *
     * @param receiver the event receiver
     */
    public void setReceiver(@Nullable final Receiver<T> receiver) {
        this.receiver = receiver;
        if(receiver != null) {
            emitEvents(receiver);
        }
    }

    /**
     * Sets whether the queue is paused. The paused queue emits only when it is unpaused, and a receiver is available.
     *
     * @param paused whether the queue is paused
     */
    public void setPaused(boolean paused) {
        boolean wasPaused = this.paused;
        this.paused = paused;
        if(wasPaused && !paused) {
            final Receiver<T> currentReceiver = receiver;
            if(currentReceiver != null) {
                emitEvents(currentReceiver);
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
        if(!canEmitEvents()) {
            queuedEvents.add(event);
        } else {
            sendCommandToReceiver(receiver, event);
        }
    }
}