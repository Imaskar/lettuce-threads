package fiddle.lettucethreadsbenchmark;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 *
 * Here we will see how threads affect performance of application connected to
 * Redis. Run this benchmark with different values of -t (threads) command line
 * parameter. Note, that latency heavily affects the result. Thats why we test
 * for 3 different connections: unix socket, local TCP, remote TCP.
 *
 * @author imaskar
 */
@State(Scope.Benchmark)
public class LettuceThreads {

  public static final String KEY = "test";

  @State(Scope.Benchmark)
  public static class RedisState {

    public RedisClient client;
    public StatefulRedisConnection<String, String> conn;
    public GenericObjectPool<StatefulRedisConnection<String, String>> pool;

    public void init(RedisURI uri) {
      if (pool != null) {
        pool.close();
      }
      if (client != null) {
        client.shutdown();
      }
      client = RedisClient.create(uri);
      conn = client.connect();
      pool = ConnectionPoolSupport
          .createGenericObjectPool(client::connect, new GenericObjectPoolConfig());
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) throws InterruptedException, ExecutionException {
      RedisURI uri;
      switch (address) {
        case "socket":
          uri = RedisURI.Builder
              .socket("/var/lib/redis/redis6370.sock")
              .build();
          break;
        case "localhost":
          uri = RedisURI.Builder
              .redis("127.0.0.1", 6370)
              .build();
          break;
        case "remote":
          uri = RedisURI.Builder
              .redis("192.168.61.245", 6370)
              .build();
          break;
        default:
          throw new IllegalArgumentException(address);
      }
      init(uri);
      conn.sync().set(KEY, "0");
    }

  }

  @Param({"socket", "localhost", "remote"})
  public static String address;

  @Benchmark
  public long shared(RedisState state) {
    final RedisCommands<String, String> sync = state.conn.sync();
    Long incrby = sync.incrby(KEY, 7);
    return incrby;
  }

  @Benchmark
  public long pooled(RedisState state) throws Exception {
    try (StatefulRedisConnection<String, String> connection = state.pool.borrowObject()) {
      final RedisCommands<String, String> sync = connection.sync();
      Long incrby = sync.incrby(KEY, 7);
      return incrby;
    }
  }

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(LettuceThreads.class.getSimpleName())
        .timeUnit(TimeUnit.MICROSECONDS)
        .warmupTime(TimeValue.seconds(5))
        .warmupIterations(2)
        .measurementTime(TimeValue.seconds(10))
        .measurementIterations(5)
        .forks(5)
        .threads(1)
        .mode(Mode.Throughput)
        .shouldFailOnError(true)
        .shouldDoGC(true)
        .build();

    new Runner(opt).run();
  }
}
