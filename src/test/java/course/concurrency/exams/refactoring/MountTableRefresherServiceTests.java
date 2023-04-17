package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class MountTableRefresherServiceTests {

    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager manager;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        manager = mock(Others.MountTableManager.class);
        service = new MountTableRefresherService(__ -> manager);
        service.setCacheUpdateTimeout(1000);
        routerStore = mock(Others.RouterStore.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
        // service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
        // service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(true);

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(false);

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(4)).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSuccessedTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(true, false, true, false);

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache, times(2)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh())
                .thenReturn(true, true, true)
                .thenThrow(new RuntimeException());

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, only()).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh())
                .thenReturn(true, true, true)
                .thenAnswer(invocation -> {
                    Thread.sleep(10000);
                    return true;
                });

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, only()).invalidate(anyString());
    }

}
