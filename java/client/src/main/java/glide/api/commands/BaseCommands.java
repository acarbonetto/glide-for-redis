package glide.api.commands;

import java.util.concurrent.CompletableFuture;

/** Base Commands interface to handle generic command and transaction requests. */
public interface BaseCommands {

    CompletableFuture<Object> customCommand(String[] args);
}
