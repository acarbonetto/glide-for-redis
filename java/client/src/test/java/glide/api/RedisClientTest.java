package glide.api;

import static glide.api.models.commands.SetOptions.CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST;
import static glide.api.models.commands.SetOptions.CONDITIONAL_SET_ONLY_IF_EXISTS;
import static glide.api.models.commands.SetOptions.RETURN_OLD_VALUE;
import static glide.api.models.commands.SetOptions.TIME_TO_LIVE_KEEP_EXISTING;
import static glide.api.models.commands.SetOptions.TIME_TO_LIVE_UNIX_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glide.api.models.Command;
import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RedisClientTest {

    RedisClient service;

    ConnectionManager connectionManager;

    CommandManager commandManager;

    @BeforeEach
    public void setUp() {
        connectionManager = mock(ConnectionManager.class);
        commandManager = mock(CommandManager.class);
        service = new RedisClient(connectionManager, commandManager);
    }

    @Test
    public void customCommand_success() throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        Object value = "testValue";
        String cmd = "GETSTRING";
        CompletableFuture<Object> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(value);
        when(commandManager.submitNewCommand(any(), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Object> response = service.customCommand(new String[] {cmd, key});
        String payload = (String) response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @Test
    public void customCommand_interruptedException() throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        String cmd = "GETSTRING";
        CompletableFuture<Object> testResponse = mock(CompletableFuture.class);
        InterruptedException interruptedException = new InterruptedException();
        when(testResponse.get()).thenThrow(interruptedException);
        when(commandManager.submitNewCommand(any(), any())).thenReturn(testResponse);

        // exercise
        InterruptedException exception =
                assertThrows(
                        InterruptedException.class,
                        () -> {
                            CompletableFuture<Object> response = service.customCommand(new String[] {cmd, key});
                            response.get();
                        });

        // verify
        assertEquals(interruptedException, exception);
    }

    @Test
    public void ping_success() throws ExecutionException, InterruptedException {
        // setup
        Command cmd = Command.builder().requestType(Command.RequestType.PING).build();
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn("PONG");
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.ping();
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals("PONG", payload);
    }

    @Test
    public void pingWithMessage_success() throws ExecutionException, InterruptedException {
        // setup
        String message = "RETURN OF THE PONG";
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.PING)
                        .arguments(new String[] {message})
                        .build();
        CompletableFuture<String> testResponse = new CompletableFuture();
        testResponse.complete(message);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.ping(message);
        String pong = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(message, pong);
    }

    @Test
    public void info_success() throws ExecutionException, InterruptedException {
        // setup
        Command cmd = Command.builder().requestType(Command.RequestType.INFO).build();
        CompletableFuture<Map> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(testPayload);
        when(commandManager.<Map>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Map> response = service.info();
        Map payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @Test
    public void infoWithMultipleOptions_success() throws ExecutionException, InterruptedException {
        // setup
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.INFO)
                        .arguments(new String[] {"ALL", "DEFAULT"})
                        .build();
        CompletableFuture<Map> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(testPayload);
        when(commandManager.<Map>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        InfoOptions options =
                InfoOptions.builder()
                        .section(InfoOptions.Section.ALL)
                        .section(InfoOptions.Section.DEFAULT)
                        .build();
        CompletableFuture<Map> response = service.info(options);
        Map payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @Test
    public void infoEmptyWithOptions_success() throws ExecutionException, InterruptedException {
        // setup
        Command cmd =
                Command.builder().requestType(Command.RequestType.INFO).arguments(new String[] {}).build();
        CompletableFuture<Map> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(testPayload);
        when(commandManager.<Map>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Map> response = service.info(InfoOptions.builder().build());
        Map payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @Test
    public void get_success() throws ExecutionException, InterruptedException {
        // setup
        // TODO: randomize keys
        String key = "testKey";
        String value = "testValue";
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.GET_STRING)
                        .arguments(new String[] {key})
                        .build();
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(value);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.get(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @Test
    public void set_success() throws ExecutionException, InterruptedException {
        // setup
        // TODO: randomize keys
        String key = "testKey";
        String value = "testValue";
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.SET_STRING)
                        .arguments(new String[] {key, value})
                        .build();
        CompletableFuture<Void> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(null);
        when(commandManager.<Void>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Void> response = service.set(key, value);
        Object nullResponse = response.get();

        // verify
        assertEquals(testResponse, response);
        assertNull(nullResponse);
    }

    @Test
    public void set_withOptionsOnlyIfExists_success()
            throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        String value = "testValue";
        SetOptions setOptions =
                SetOptions.builder()
                        .conditionalSet(SetOptions.ConditionalSet.ONLY_IF_EXISTS)
                        .returnOldValue(false)
                        .expiry(
                                SetOptions.TimeToLive.builder()
                                        .type(SetOptions.TimeToLiveType.KEEP_EXISTING)
                                        .build())
                        .build();
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.SET_STRING)
                        .arguments(
                                new String[] {
                                    key, value, CONDITIONAL_SET_ONLY_IF_EXISTS, TIME_TO_LIVE_KEEP_EXISTING
                                })
                        .build();
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(null);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.set(key, value, setOptions);

        // verify
        assertNotNull(response);
        assertNull(response.get());
    }

    @Test
    public void set_withOptionsOnlyIfDoesNotExist_success()
            throws ExecutionException, InterruptedException {
        // setup
        String key = "testKey";
        String value = "testValue";
        SetOptions setOptions =
                SetOptions.builder()
                        .conditionalSet(SetOptions.ConditionalSet.ONLY_IF_DOES_NOT_EXIST)
                        .returnOldValue(true)
                        .expiry(
                                SetOptions.TimeToLive.builder()
                                        .type(SetOptions.TimeToLiveType.UNIX_SECONDS)
                                        .count(60)
                                        .build())
                        .build();
        Command cmd =
                Command.builder()
                        .requestType(Command.RequestType.SET_STRING)
                        .arguments(
                                new String[] {
                                    key,
                                    value,
                                    CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST,
                                    RETURN_OLD_VALUE,
                                    TIME_TO_LIVE_UNIX_SECONDS + " 60"
                                })
                        .build();
        CompletableFuture<String> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(value);
        when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.set(key, value, setOptions);

        // verify
        assertNotNull(response);
        assertEquals(value, response.get());
    }
}
