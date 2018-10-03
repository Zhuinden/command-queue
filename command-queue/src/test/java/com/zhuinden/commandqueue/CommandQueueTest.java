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

    @Test
    public void setPausedWorks() {
        final List<Object> commands = new ArrayList<>();

        Object command1 = new Object();
        Object command2 = new Object();
        Object command3 = new Object();

        final CommandQueue<Object> queue = new CommandQueue<>();

        CommandQueue.Receiver<Object> receiver = new CommandQueue.Receiver<Object>() {
            @Override
            public void receiveCommand(@NonNull Object command) {
                commands.add(command);
            }
        };

        queue.sendEvent(command1);

        assertThat(commands).isEmpty();

        queue.setReceiver(receiver);

        assertThat(commands).containsExactly(command1);

        queue.setPaused(true);

        queue.sendEvent(command2);

        assertThat(commands).containsExactly(command1);

        queue.detachReceiver();

        queue.setReceiver(receiver);

        assertThat(commands).containsExactly(command1);

        queue.setPaused(false);

        assertThat(commands).containsExactly(command1, command2);

        queue.setPaused(true);

        queue.detachReceiver();

        queue.sendEvent(command3);

        assertThat(commands).containsExactly(command1, command2);

        queue.setPaused(false);

        assertThat(commands).containsExactly(command1, command2);

        queue.setReceiver(receiver);

        assertThat(commands).containsExactly(command1, command2, command3);
    }

    @Test
    public void setPausedFalseDoesNotEmitWhileEventsAreEmitted() {
        final Object event1 = new Object();
        final Object event2 = new Object();
        final Object event3 = new Object();

        final List<Object> commands = new ArrayList<>();

        final CommandQueue<Object> commandQueue = new CommandQueue<>();

        final CommandQueue.Receiver<Object> fakeReceiver = new CommandQueue.Receiver<Object>() {
            @Override
            public void receiveCommand(@NonNull Object command) {
                throw new IllegalStateException("This shouldn't be called");
            }
        };

        final CommandQueue.Receiver<Object> receiver = new CommandQueue.Receiver<Object>() {
            @Override
            public void receiveCommand(@NonNull Object command) {
                commands.add(command);

                if(command == event1) {
                    commandQueue.setPaused(true);
                }
                if(command == event2) {
                    commandQueue.setPaused(true);
                    commandQueue.sendEvent(event3);
                    commandQueue.setReceiver(fakeReceiver);
                    commandQueue.setPaused(false);
                    commandQueue.setReceiver(this);
                }
            }
        };

        commandQueue.setReceiver(receiver);
        commandQueue.sendEvent(event1);
        assertThat(commands).containsExactly(event1);
        commandQueue.sendEvent(event2);
        assertThat(commands).containsExactly(event1);
        commandQueue.setPaused(false);
        assertThat(commands).containsExactly(event1, event2, event3);
    }

    @Test
    public void setReceiverInReceiveCommandShouldntFreezeQueue() {
        final Object event1 = new Object();
        final Object event2 = new Object();
        final Object event3 = new Object();

        final List<Object> commands1 = new ArrayList<>();
        final List<Object> commands2 = new ArrayList<>();

        final CommandQueue<Object> commandQueue = new CommandQueue<>();

        final CommandQueue.Receiver<Object> receiver2 = new CommandQueue.Receiver<Object>() {
            @Override
            public void receiveCommand(@NonNull Object command) {
                commands2.add(command);
                if(command == event2) {
                    commandQueue.sendEvent(event3);
                }
            }
        };

        final CommandQueue.Receiver<Object> receiver1 = new CommandQueue.Receiver<Object>() {
            @Override
            public void receiveCommand(@NonNull Object command) {
                commands1.add(command);
                if(command == event1) {
                    commandQueue.detachReceiver();
                    commandQueue.sendEvent(event2);
                    commandQueue.setReceiver(receiver2);
                }
            }
        };

        commandQueue.sendEvent(event1);
        commandQueue.setReceiver(receiver1);

        assertThat(commands1).containsExactly(event1);
        assertThat(commands2).containsExactly(event2, event3);
    }
}
