// package com.hms.shared.orchestrator;

// import java.util.List;
// import java.util.concurrent.CompletableFuture;

// import org.slf4j.Logger;
// import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
// import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// public class ThreadOrchestrator<T> implements Runnable {

//     // private final Logger LOG = org.slf4j.LoggerFactory.getLogger(SseEmitterOrchestrator.class);
//     private final String ORCHESTRATOR_THREAD_NAME = "SseEmitterOrchestrator-Thread";
//     private final String HANDLER_THREAD_NAME_PREFIX = "SseEmitterHandler-Thread-";
//     private final ThreadPoolTaskExecutor taskExecutor;
//     private final List<OrchestratorHandler<T>> handlers;
//     private final T data;

//     private ThreadOrchestrator(T data, List<OrchestratorHandler<T>> handlers) {
//         this.data = data;
//         this.handlers = handlers;

//         this.taskExecutor = new ThreadPoolTaskExecutor();
//         this.taskExecutor.setThreadNamePrefix(HANDLER_THREAD_NAME_PREFIX);
//         this.taskExecutor.setCorePoolSize(handlers.size());
//         this.taskExecutor.setMaxPoolSize(handlers.size());
//         this.taskExecutor.setWaitForTasksToCompleteOnShutdown(false);
//     }

//     public void execute() {

//         Thread.ofPlatform().name(ORCHESTRATOR_THREAD_NAME).start(this);
//     }

//     @Override
//     public void run() {
//         this.taskExecutor.initialize();

//         try {
//             List<CompletableFuture<Void>> futures = handlers.stream()
//                     .map(handler -> CompletableFuture.runAsync(() -> {
//                         try {
//                             handler.handle(data);
//                         } catch (Exception e) {
//                             LOG.error("Error during SSE handling: " + e.getMessage(), e);
//                         }
//                     }, taskExecutor))
//                     .toList();
//             CompletableFuture<Void> completed = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//             emitter.onError((e) -> {
//                 LOG.warn("SSE Emitter completed with error", e);
//                 completed.cancel(true);
//                 if (!taskExecutor.getThreadPoolExecutor().isShutdown()) {
//                     taskExecutor.shutdown();
//                 }
//             });
//             emitter.onCompletion(() -> {
//                 LOG.info("SSE Emitter completed");
//                 completed.cancel(true);
//                 if (!taskExecutor.getThreadPoolExecutor().isShutdown()) {
//                     taskExecutor.shutdown();
//                 }
//             });
//             completed.join();
//         } finally {
//             emitter.complete();
//             if (!taskExecutor.getThreadPoolExecutor().isShutdown()) {
//                 taskExecutor.shutdown();
//             }
//         }
//     }

//     public static <T> Builder<T> builder() {
//         return new Builder<>();
//     }

//     public static class Builder<T> {
//         private SseEmitter emitter;
//         private T data;
//         private List<OrchestratorHandler<T>> handlers = new java.util.ArrayList<>();

//         public Builder<T> withEmitter(SseEmitter emitter) {
//             this.emitter = emitter;
//             return this;
//         }

//         public Builder<T> withData(T data) {
//             this.data = data;
//             return this;
//         }

//         public Builder<T> addHandler(OrchestratorHandler<T> handler) {
//             this.handlers.add(handler);
//             return this;
//         }

//         public Builder<T> addHandlers(List<OrchestratorHandler<T>> handlers) {
//             this.handlers.addAll(handlers);
//             return this;
//         }

//         public ThreadOrchestrator<T> build() {
//             return new ThreadOrchestrator<>(emitter, data, handlers);
//         }
//     }
// }
