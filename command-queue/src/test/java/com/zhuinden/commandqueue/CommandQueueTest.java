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

import android.support.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandQueueTest {
    public abstract static class Events {
        public static class First extends Events {}

        public static class Second extends Events {}

        public static class Third extends Events {}

        public static class Fourth extends Events {}

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == getClass();
        }
    }

    @Test
    public void commandQueueWorks() {
        class Holder<T> {
            T item;

            public Holder(T initial) {
                this.item = initial;
            }
        }

        final Holder<Integer> firsts = new Holder<>(0);
        final Holder<Integer> seconds = new Holder<>(0);

        CommandQueue<Events> queue = new CommandQueue<>();

        CommandQueue.Receiver<Events> receiver = new CommandQueue.Receiver<Events>() {
            @Override
            public void receiveCommand(@NonNull Events command) {
                if(command instanceof Events.First) {
                    firsts.item += 1;
                } else if(command instanceof Events.Second) {
                    seconds.item += 1;
                }
            }
        };

        queue.setReceiver(receiver);

        assertThat(firsts.item).isEqualTo(0);
        assertThat(seconds.item).isEqualTo(0);

        queue.sendEvent(new Events.First());

        assertThat(firsts.item).isEqualTo(1);
        assertThat(seconds.item).isEqualTo(0);

        queue.sendEvent(new Events.Second());

        assertThat(firsts.item).isEqualTo(1);
        assertThat(seconds.item).isEqualTo(1);

        queue.detachReceiver();

        queue.sendEvent(new Events.First());
        queue.sendEvent(new Events.Second());
        queue.sendEvent(new Events.Second());
        queue.sendEvent(new Events.Second());

        assertThat(firsts.item).isEqualTo(1);
        assertThat(seconds.item).isEqualTo(1);

        queue.setReceiver(receiver);

        assertThat(firsts.item).isEqualTo(2);
        assertThat(seconds.item).isEqualTo(4);
    }

    @Test
    public void nullEventIsDisallowed() {
        CommandQueue<Void> queue = new CommandQueue<>();

        try {
            queue.sendEvent(null);
            Assert.fail();
        } catch(IllegalArgumentException e) {
            // OK!
        }
    }

    @Test
    public void emittingEventsInReceiverPreservesOrder() {
        final CommandQueue<Events> queue = new CommandQueue<>();

        final List<Events> eventsList = new ArrayList<>();

        CommandQueue.Receiver<Events> receiver = new CommandQueue.Receiver<Events>() {
            @Override
            public void receiveCommand(@NonNull Events command) {
                eventsList.add(command);
                if(command instanceof Events.First) {
                    queue.detachReceiver();
                    queue.sendEvent(new Events.Second());
                    queue.sendEvent(new Events.Third());
                    queue.sendEvent(new Events.Second());
                    queue.sendEvent(new Events.Third());
                    queue.sendEvent(new Events.Second());
                } else if(command instanceof Events.Second) {
                    queue.sendEvent(new Events.Third());
                }
            }
        };

        queue.sendEvent(new Events.Fourth());
        assertThat(eventsList).isEmpty();

        queue.setReceiver(receiver);
        assertThat(eventsList).containsExactly(new Events.Fourth());

        queue.sendEvent(new Events.First());
        assertThat(eventsList).containsExactly(new Events.Fourth(), new Events.First());

        queue.sendEvent(new Events.Fourth());
        assertThat(eventsList).containsExactly(new Events.Fourth(), new Events.First());

        queue.setReceiver(receiver);
        assertThat(eventsList).containsExactly(new Events.Fourth(), new Events.First(), new Events.Second(), new Events.Third(), new Events.Second(), new Events.Third(), new Events.Second(), new Events.Fourth(), new Events.Third(), new Events.Third(), new Events.Third());
    }

    @Test
    public void hasReceiverWorksCorrectly() {
        final CommandQueue<Object> queue = new CommandQueue<>();

        CommandQueue.Receiver<Object> receiver = new CommandQueue.Receiver<Object>() {
            @Override
            public void receiveCommand(@NonNull Object command) {

            }
        };

        assertThat(queue.hasReceiver()).isFalse();

        queue.setReceiver(receiver);

        assertThat(queue.hasReceiver()).isTrue();

        queue.detachReceiver();

        assertThat(queue.hasReceiver()).isFalse();
    }
}
