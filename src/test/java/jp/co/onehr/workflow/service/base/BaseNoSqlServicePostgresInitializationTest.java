package jp.co.onehr.workflow.service.base;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.thunderz99.cosmos.condition.Condition;
import io.github.thunderz99.cosmos.impl.postgres.PostgresDatabaseImpl;
import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.dto.base.BaseData;
import jp.co.onehr.workflow.util.InfraUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.assertThat;

class BaseNoSqlServicePostgresInitializationTest {

    private static final int THREAD_COUNT = 4;

    /**
     * Mirrors onehr-core's regression for java-cosmos 0.8.35 (ONEHR_CORE-6872): lazy table initialization
     * must wait for a competing initializer instead of returning early, and every concurrent first request
     * must then succeed.
     */
    @Test
    @EnabledIf("isPostgres")
    void getColl_should_wait_for_concurrent_table_initialization_success() throws Exception {
        var configuration = ProcessConfiguration.getConfiguration();
        var database = (PostgresDatabaseImpl) CosmosDB.getDefaultDatabaseByEnv();
        var dataSource = database.getDataSource();

        // scenario: Four first requests for one host arrive while another initializer holds the table lock.
        {
            var suffix = UUID.randomUUID().toString().replace("-", "");
            var schemaName = "wf_init_" + suffix;
            var host = "wf-init-" + suffix;
            var service = new ConcurrentInitializationService();
            var partition = service.getPartition();
            var lockKey = "%s.%s".formatted(TableUtil.checkAndNormalizeValidEntityName(schemaName),
                    TableUtil.checkAndNormalizeValidEntityName(partition)).hashCode();
            var executor = Executors.newFixedThreadPool(THREAD_COUNT);
            var started = new CountDownLatch(THREAD_COUNT);
            var start = new CountDownLatch(1);
            var futures = new ArrayList<Future<List<ConcurrentInitializationEntity>>>();

            try {
                try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
                    stmt.execute("CREATE SCHEMA " + TableUtil.checkAndNormalizeValidEntityName(schemaName));
                }
                configuration.registerDB(host, database, schemaName);

                try (var lockConnection = dataSource.getConnection()) {
                    lockConnection.setAutoCommit(false);
                    int lockHolderPid;
                    try (var stmt = lockConnection.createStatement()) {
                        stmt.execute("SELECT pg_advisory_xact_lock(" + lockKey + ")");
                        try (var rs = stmt.executeQuery("SELECT pg_backend_pid()")) {
                            assertThat(rs.next()).isTrue();
                            lockHolderPid = rs.getInt(1);
                        }
                    }

                    for (var i = 0; i < THREAD_COUNT; i++) {
                        var id = "concurrent-initialization-" + i;
                        futures.add(executor.submit(() -> {
                            started.countDown();
                            start.await();
                            service.upsert(host, new ConcurrentInitializationEntity(id));
                            return service.find(host, Condition.filter("id", id).limit(1));
                        }));
                    }
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                    start.countDown();

                    // Initialization must wait for the competing transaction instead of returning early.
                    var waiting = false;
                    var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                    try (var conn = dataSource.getConnection(); var pstmt = conn.prepareStatement("""
                            SELECT 1
                            FROM pg_locks held
                            JOIN pg_locks waiting
                              ON waiting.locktype = held.locktype
                             AND waiting.database IS NOT DISTINCT FROM held.database
                             AND waiting.classid = held.classid
                             AND waiting.objid = held.objid
                             AND waiting.objsubid = held.objsubid
                            WHERE held.pid = ?
                              AND held.locktype = 'advisory'
                              AND held.granted
                              AND NOT waiting.granted
                            """)) {
                        pstmt.setInt(1, lockHolderPid);
                        while (!waiting && System.nanoTime() < deadline && futures.stream().noneMatch(Future::isDone)) {
                            try (var rs = pstmt.executeQuery()) {
                                waiting = rs.next();
                            }
                            if (!waiting) {
                                Thread.sleep(10);
                            }
                        }
                    }
                    assertThat(waiting).isTrue();
                    assertThat(futures).noneMatch(Future::isDone);

                    // The competing initializer rolls back before creating the table.
                    lockConnection.rollback();
                }

                for (var future : futures) {
                    assertThat(future.get(10, TimeUnit.SECONDS)).hasSize(1);
                }
                assertThat(service.find(host, Condition.filter().limit(THREAD_COUNT + 1))).hasSize(THREAD_COUNT);
            } finally {
                executor.shutdownNow();
                var terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
                database.disableTTL(schemaName, partition + "_recycle");
                try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
                    stmt.execute("DROP SCHEMA IF EXISTS " + TableUtil.checkAndNormalizeValidEntityName(schemaName) + " CASCADE");
                }
                assertThat(terminated).isTrue();
            }
        }

        // scenario: Another host is invalidated while the first request of this host is initializing.
        {
            var suffix = UUID.randomUUID().toString().replace("-", "");
            var schemaName = "wf_init_" + suffix;
            var host = "wf-init-" + suffix;
            var otherHost = "wf-init-other-" + suffix;
            var service = new ConcurrentInitializationService();
            var partition = service.getPartition();
            var lockKey = "%s.%s".formatted(TableUtil.checkAndNormalizeValidEntityName(schemaName),
                    TableUtil.checkAndNormalizeValidEntityName(partition)).hashCode();
            var executor = Executors.newFixedThreadPool(2);
            var secondThread = new CompletableFuture<Thread>();

            try {
                try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
                    stmt.execute("CREATE SCHEMA " + TableUtil.checkAndNormalizeValidEntityName(schemaName));
                }
                configuration.registerDB(host, database, schemaName);

                Future<List<ConcurrentInitializationEntity>> first;
                Future<List<ConcurrentInitializationEntity>> second;
                try (var lockConnection = dataSource.getConnection()) {
                    lockConnection.setAutoCommit(false);
                    int lockHolderPid;
                    try (var stmt = lockConnection.createStatement()) {
                        stmt.execute("SELECT pg_advisory_xact_lock(" + lockKey + ")");
                        try (var rs = stmt.executeQuery("SELECT pg_backend_pid()")) {
                            assertThat(rs.next()).isTrue();
                            lockHolderPid = rs.getInt(1);
                        }
                    }

                    try (var conn = dataSource.getConnection(); var pstmt = conn.prepareStatement("""
                            SELECT count(*)
                            FROM pg_locks held
                            JOIN pg_locks waiting
                              ON waiting.locktype = held.locktype
                             AND waiting.database IS NOT DISTINCT FROM held.database
                             AND waiting.classid = held.classid
                             AND waiting.objid = held.objid
                             AND waiting.objsubid = held.objsubid
                            WHERE held.pid = ?
                              AND held.locktype = 'advisory'
                              AND held.granted
                              AND NOT waiting.granted
                            """)) {
                        pstmt.setInt(1, lockHolderPid);

                        first = executor.submit(() -> {
                            service.upsert(host, new ConcurrentInitializationEntity("first"));
                            return service.find(host, Condition.filter("id", "first").limit(1));
                        });

                        // The first request starts the initialization and waits for the table lock.
                        var waiters = 0;
                        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                        while (waiters == 0 && System.nanoTime() < deadline && !first.isDone()) {
                            try (var rs = pstmt.executeQuery()) {
                                assertThat(rs.next()).isTrue();
                                waiters = rs.getInt(1);
                            }
                            if (waiters == 0) {
                                Thread.sleep(10);
                            }
                        }
                        assertThat(waiters).isEqualTo(1);

                        // This guards that invalidating another host keeps this host's state and lock. It does not
                        // reproduce the removed copy-and-replace race, whose window has no seam that a test can control.
                        configuration.invalidateSchemaCache(otherHost);

                        second = executor.submit(() -> {
                            secondThread.complete(Thread.currentThread());
                            service.upsert(host, new ConcurrentInitializationEntity("second"));
                            return service.find(host, Condition.filter("id", "second").limit(1));
                        });

                        // The second request must wait for the running initialization of its host in this JVM,
                        // instead of starting another initialization that also waits for the table lock.
                        var thread = secondThread.get(5, TimeUnit.SECONDS);
                        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                        while (thread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline && !second.isDone()) {
                            Thread.sleep(10);
                        }
                        assertThat(thread.getState()).isEqualTo(Thread.State.BLOCKED);
                        try (var rs = pstmt.executeQuery()) {
                            assertThat(rs.next()).isTrue();
                            assertThat(rs.getInt(1)).isEqualTo(1);
                        }
                        assertThat(first.isDone()).isFalse();
                        assertThat(second.isDone()).isFalse();
                    }

                    // The competing initializer rolls back before creating the table.
                    lockConnection.rollback();
                }

                assertThat(first.get(10, TimeUnit.SECONDS)).hasSize(1);
                assertThat(second.get(10, TimeUnit.SECONDS)).hasSize(1);
            } finally {
                executor.shutdownNow();
                var terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
                database.disableTTL(schemaName, partition + "_recycle");
                try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
                    stmt.execute("DROP SCHEMA IF EXISTS " + TableUtil.checkAndNormalizeValidEntityName(schemaName) + " CASCADE");
                }
                assertThat(terminated).isTrue();
            }
        }
    }

    static boolean isPostgres() {
        return InfraUtil.isPostgres(CosmosDB.getDefaultDatabaseByEnv());
    }

    private static class ConcurrentInitializationService extends BaseCRUDService<ConcurrentInitializationEntity> {

        private ConcurrentInitializationService() {
            super(ConcurrentInitializationEntity.class, "ConcurrentInitializationEntities");
        }
    }

    public static class ConcurrentInitializationEntity extends BaseData {

        public ConcurrentInitializationEntity() {
        }

        private ConcurrentInitializationEntity(String id) {
            this.id = id;
        }
    }
}
