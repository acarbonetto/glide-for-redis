package glide.api;

import static glide.ffi.resolvers.SocketListenerResolver.getSocket;

import glide.api.commands.BaseCommands;
import glide.api.commands.ResponseHandlers;
import glide.api.commands.ServerCommands;
import glide.api.models.Command;
import glide.api.commands.GenericCommands;
import glide.api.commands.StringCommands;
import glide.api.models.commands.SetOptions;
import glide.api.models.configuration.RedisClientConfiguration;
import glide.connectors.handlers.CallbackDispatcher;
import glide.connectors.handlers.ChannelHandler;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Async (non-blocking) client for Redis in Standalone mode. Use {@link
 * #CreateClient(RedisClientConfiguration)} to request a client to Redis.
 */
public class RedisClient extends BaseClient implements BaseCommands, GenericCommands,
    ServerCommands, StringCommands {

    /**
     * Request an async (non-blocking) Redis client in Standalone mode.
     *
     * @param config - Redis Client Configuration
     * @return a Future to connect and return a RedisClient
     */
    public static CompletableFuture<RedisClient> CreateClient(RedisClientConfiguration config) {
        ChannelHandler channelHandler = buildChannelHandler();
        ConnectionManager connectionManager = buildConnectionManager(channelHandler);
        CommandManager commandManager = buildCommandManager(channelHandler);
        // TODO: Support exception throwing, including interrupted exceptions
        return connectionManager
                .connectToRedis(config)
                .thenApply(ignore -> new RedisClient(connectionManager, commandManager));
    }

    protected static ChannelHandler buildChannelHandler() {
        CallbackDispatcher callbackDispatcher = new CallbackDispatcher();
        return new ChannelHandler(callbackDispatcher, getSocket());
    }

    protected static ConnectionManager buildConnectionManager(ChannelHandler channelHandler) {
        return new ConnectionManager(channelHandler);
    }

    protected static CommandManager buildCommandManager(ChannelHandler channelHandler) {
        return new CommandManager(channelHandler);
    }

    protected RedisClient(ConnectionManager connectionManager, CommandManager commandManager) {
        super(connectionManager, commandManager);
    }

    /**
     * Executes a single custom command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in args.
     *
     * @param args command and arguments for the custom command call
     * @return CompletableFuture with the response
     */
    public CompletableFuture<Object> customCommand(String[] args) {
        Command command =
                Command.builder().requestType(Command.RequestType.CUSTOM_COMMAND).arguments(args).build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleObjectResponse);
    }

    /**
     * Get information and statistics about the Redis server.
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     *
     * @return CompletableFuture with the response
     */
    public CompletableFuture<Map> info() {
        Command command =
            Command.builder().requestType(Command.RequestType.INFO).build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleMapResponse);
    }

    /**
     * Get information and statistics about the Redis server.
     *  See https://redis.io/commands/info/ for details.
     *
     * @param options A list of InfoSection values specifying which sections of information to retrieve.
     * When no parameter is provided, the default option is assumed.
     * @return CompletableFuture with the response
     */
    public CompletableFuture<Map> info(String[] options) {
        Command command =
            Command.builder().requestType(Command.RequestType.INFO).arguments(args).build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleMapResponse);
    }

    /**
     * Get the value associated with the given key, or null if no such value exists. @see
     * <a href="https://redis.io/commands/get/">redis.io</a> for details.
     *
     * @param key - The key to retrieve from the database.
     * @return If `key` exists, returns the value of `key` as a string. Otherwise, return null
     */
    public CompletableFuture<String> get(String key) {
        Command command =
            Command.builder()
                .requestType(Command.RequestType.GET_STRING)
                .arguments(new String[] {key})
                .build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleStringResponse);
    }

    /**
     * Set the given key with the given value. Return value is dependent on the passed options. @see
     * <a href="https://redis.io/commands/set/">redis.io</a> for details.
     *
     * @param key - The key to store.
     * @param value - The value to store with the given key.
     * @return null
     */
    public CompletableFuture<Void> set(String key, String value) {
        Command command =
            Command.builder()
                .requestType(Command.RequestType.SET_STRING)
                .arguments(new String[] {key, value})
                .build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleVoidResponse);
    }

    /**
     * Set the given key with the given value. Return value is dependent on the passed options. @see
     * <a href="https://redis.io/commands/set/">redis.io</a> for details.
     *
     * @param key - The key to store.
     * @param value - The value to store with the given key.
     * @param options - The Set options
     * @return string or null If value isn't set because of `onlyIfExists` or `onlyIfDoesNotExist`
     *     conditions, return null. If `returnOldValue` is set, return the old value as a string.
     */
    public CompletableFuture<String> set(String key, String value, SetOptions options) {
        LinkedList<String> argsList = new LinkedList<>();
        argsList.add(key);
        argsList.add(value);
        argsList.addAll(SetOptions.createSetOptions(options));
        String[] args = argsList.toArray(new String[0]);
        Command command =
            Command.builder()
                .requestType(Command.RequestType.SET_STRING)
                .arguments(args)
                .build();
        return commandManager.submitNewCommand(command, ResponseHandlers::handleStringResponse);
    }
}
