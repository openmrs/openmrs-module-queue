package org.openmrs.module.queue.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Location;
import org.openmrs.module.queue.api.dao.QueueRoomDao;
import org.openmrs.module.queue.api.impl.QueueRoomServiceImpl;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueRoom;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueueRoomServiceTest {
    private static final String QUEUE_ROOM_UUID = "4eb8fe43-2813-4kbc-80dc-2e5d30252cc6";

    private static final String QUEUE_ROOM_NAME = "Triage Room 1";

    private static final String QUEUE_UUID = "3eb7fe43-2813-4kbc-80dc-2e5d30252bb5";

    private static final String LOCATION_UUID = "d0938432-1691-11df-97a5-7038c098";

    private QueueRoomServiceImpl queueRoomService;

    @Mock
    private QueueRoomDao dao;

    @Before
    public void setupMocks() {
        MockitoAnnotations.openMocks(this);
        queueRoomService = new QueueRoomServiceImpl();
        queueRoomService.setDao(dao);
    }

    @Test
    public void shouldGetByUuid() {
        QueueRoom queueRoom = mock(QueueRoom.class);
        when(queueRoom.getUuid()).thenReturn(QUEUE_ROOM_UUID);
        when(dao.get(QUEUE_ROOM_UUID)).thenReturn(Optional.of(queueRoom));

        Optional<QueueRoom> result = queueRoomService.getQueueRoomByUuid(QUEUE_ROOM_UUID);
        assertThat(result.isPresent(), is(true));
        result.ifPresent(q -> assertThat(q.getUuid(), is(QUEUE_ROOM_UUID)));
    }

    @Test
    public void shouldCreateNewQueue() {
        QueueRoom queueRoom = mock(QueueRoom.class);
        when(queueRoom.getUuid()).thenReturn(QUEUE_ROOM_UUID);
        when(queueRoom.getName()).thenReturn(QUEUE_ROOM_NAME);
        when(dao.createOrUpdate(queueRoom)).thenReturn(queueRoom);

        QueueRoom result = queueRoomService.createQueueRoom(queueRoom);
        assertThat(result, notNullValue());
        assertThat(result.getUuid(), is(QUEUE_ROOM_UUID));
        assertThat(result.getName(), is(QUEUE_ROOM_NAME));
    }

    @Test
    public void shouldVoidQueue() {
        when(dao.get(QUEUE_ROOM_UUID)).thenReturn(Optional.empty());

        queueRoomService.voidQueueRoom(QUEUE_ROOM_UUID, "API Call");

        assertThat(queueRoomService.getQueueRoomByUuid(QUEUE_ROOM_UUID).isPresent(), is(false));
    }

    @Test
    public void shouldPurgeQueue() {
        QueueRoom queueRoom = mock(QueueRoom.class);
        when(dao.get(QUEUE_ROOM_UUID)).thenReturn(Optional.empty());

        queueRoomService.purgeQueueRoom(queueRoom);
        assertThat(queueRoomService.getQueueRoomByUuid(QUEUE_ROOM_UUID).isPresent(), is(false));
    }

    @Test
    public void shouldGetAllQueueRoomsByLocation() {
        QueueRoom queueRoom = mock(QueueRoom.class);
        Location location = new Location();
        location.setUuid(LOCATION_UUID);
        when(dao.getQueueRoomsByServiceAndLocation(null,location)).thenReturn(Collections.singletonList(queueRoom));

        List<QueueRoom> queueRoomsByLocation = queueRoomService.getQueueRoomsByServiceAndLocation(null, location);
        assertThat(queueRoomsByLocation, notNullValue());
        assertThat(queueRoomsByLocation, hasSize(1));
    }

    @Test
    public void shouldGetAllQueueRoomsByQueue() {
        QueueRoom queueRoom = mock(QueueRoom.class);
        Queue queue = new Queue();
        queue.setUuid(QUEUE_UUID);
        when(dao.getQueueRoomsByServiceAndLocation(queue,null)).thenReturn(Collections.singletonList(queueRoom));

        List<QueueRoom> queueRoomsByLocation = queueRoomService.getQueueRoomsByServiceAndLocation(queue, null);
        assertThat(queueRoomsByLocation, notNullValue());
        assertThat(queueRoomsByLocation, hasSize(1));
    }
}
