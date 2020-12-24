package com.zhuinden.commandqueue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class CommandQueueMockitoTest {

    private CommandQueue<Integer> SUT;
    @Mock
    private CommandQueue.Receiver<Integer> receiverMock;
    @Mock
    private CommandQueue.Receiver<Integer> newReceiverMock;

    @Before
    public void setUp() throws Exception {
        SUT = new CommandQueue<>();
        SUT.setReceiver(receiverMock);
    }

    @Test
    public void eventSentIsPassedToReceiver() {
        ArgumentCaptor<Integer> ac = ArgumentCaptor.forClass(Integer.class);
        SUT.sendEvent(5);
        verify(receiverMock, times(1)).receiveCommand(ac.capture());
        Assert.assertThat(ac.getValue(), is(5));
    }

    @Test
    public void eventSentWhilePausedReceiverNotCalled() {
        SUT.setPaused(true);
        SUT.sendEvent(5);
        verifyNoMoreInteractions(receiverMock);
    }

    @Test
    public void eventsAreStoredWhileQueueIsPausedThenEmittedAfterQueueResumed() {
        ArgumentCaptor<Integer> ac = ArgumentCaptor.forClass(Integer.class);
        SUT.setPaused(true);
        SUT.sendEvent(5);
        SUT.setPaused(false);
        verify(receiverMock, times(1)).receiveCommand(ac.capture());
        Assert.assertThat(ac.getValue(), is(5));
    }

    @Test
    public void multipleEventsSentAreReceiverInOrder() {
        ArgumentCaptor<Integer> ac = ArgumentCaptor.forClass(Integer.class);
        SUT.sendEvent(5);
        SUT.sendEvent(6);
        SUT.sendEvent(7);
        verify(receiverMock, times(3)).receiveCommand(ac.capture());
        List<Integer> eventList = ac.getAllValues();
        Assert.assertThat(eventList.get(0), is(5));
        Assert.assertThat(eventList.get(1), is(6));
        Assert.assertThat(eventList.get(2), is(7));
    }

    @Test
    public void receiverNoLongerReceivesEventAfterDetaching() {
        SUT.detachReceiver();
        SUT.sendEvent(5);
        verifyNoMoreInteractions(receiverMock);
    }

    @Test
    public void eventsAreStoredWhileNoReceiverExistsThenEmittedAfterReceiverRegistration() {
        ArgumentCaptor<Integer> ac = ArgumentCaptor.forClass(Integer.class);
        SUT.detachReceiver();
        SUT.sendEvent(5);
        SUT.setReceiver(receiverMock);
        verify(receiverMock, times(1)).receiveCommand(ac.capture());
        Assert.assertThat(ac.getValue(), is(5));
    }

    @Test
    public void settingANewReceiverMakesSureToOnlyReceiveNewEventThere() {
        ArgumentCaptor<Integer> ac = ArgumentCaptor.forClass(Integer.class);
        SUT.setReceiver(newReceiverMock);
        SUT.sendEvent(5);
        verifyNoMoreInteractions(receiverMock);
        verify(newReceiverMock, times(1)).receiveCommand(ac.capture());
        Assert.assertThat(ac.getValue(), is(5));
    }
}
