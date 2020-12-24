/*
 * Copyright 2018 Gabor Varadi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhuinden.commandqueue;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A class that can be used to send events, and events are enqueued while there is no observer.
 * Allows only a single receiver at a time. Enqueued events are emitted on registration.
 *
 * @param <T> the type of the event
 */
public class CommandQueue<T> {
    private final long threadId = Thread.currentThread().getId();

    private final ConcurrentLinkedQueue<T> queuedEvents = new ConcurrentLinkedQueue<T>();
    private boolean paused;
    private boolean distinctOnly;
    private int limit = -1;

    public CommandQueue() {
    }

    public static class Builder<T> {
        private boolean distinctOnly = false;

        private int limit = -1;

        public Builder<T> distinctOnly() {
            this.distinctOnly = true;
            return this;
        }

        public Builder<T> limit(int limit) {
            this.limit = limit;
            return this;
        }

        public CommandQueue<T> build() {
            CommandQueue<T> commandQueue = new CommandQueue<T>();
            commandQueue.distinctOnly = distinctOnly;
            commandQueue.limit = limit;
            return commandQueue;
        }
    }

    /**
     * The receiver receives commands when it is set.
     *
     * @param <T> the type of the event
     */
    public interface Receiver<T> {
        void receiveCommand(@Nonnull T command);
    }

    private Receiver<T> receiver;

    /**
     * Returns if the command queue has a receiver.
     *
     * @return whether there is a receiver
     */
    public boolean hasReceiver() {
        verifyCurrentThread();
        return receiver != null;
    }

    private boolean isEmittingEvent = false;

    private boolean canEmitEvents() {
        return receiver != null && !isEmittingEvent && !paused;
    }

    private T previouslyEmittedEvent;

    private void emitEvents(@Nonnull final Receiver<T> receiver) {
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

    private void sendCommandToReceiver(@Nonnull Receiver<T> receiver, @Nonnull T event) {
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
        verifyCurrentThread();

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
        verifyCurrentThread();

        final boolean wasPaused = this.paused;

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
        verifyCurrentThread();

        this.receiver = null;
    }

    /**
     * Sends the event to the receiver. If there is no receiver, then the event is enqueued until a new receiver is set.
     *
     * @param event the event
     */
    public void sendEvent(@Nonnull final T event) {
        verifyCurrentThread();

        //noinspection ConstantConditions
        if(event == null) {
            throw new IllegalArgumentException("Null value is not allowed as an event");
        }
        if(!canEmitEvents()) {
            if(limit == -1 || queuedEvents.size() < limit) { // drop new events that don't fit the queue
                queuedEvents.add(event);
            }
        } else {
            sendCommandToReceiver(receiver, event);
        }
    }

    private void verifyCurrentThread() {
        if(threadId != Thread.currentThread().getId()) {
            throw new IllegalStateException(
                    "A command queue can only be accessed on the thread where it was created.");
        }
    }
}