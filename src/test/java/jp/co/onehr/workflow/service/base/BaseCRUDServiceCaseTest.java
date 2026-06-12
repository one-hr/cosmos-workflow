package jp.co.onehr.workflow.service.base;

import java.util.ArrayList;
import java.util.List;

import io.github.thunderz99.cosmos.dto.BulkPatchOperation;
import io.github.thunderz99.cosmos.v4.PatchOperations;
import jp.co.onehr.workflow.base.BaseTest;
import jp.co.onehr.workflow.dto.base.BaseData;
import jp.co.onehr.workflow.dto.base.BulkResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseCRUDServiceCaseTest extends BaseTest {

    private final SampleEntityService service = new SampleEntityService();

    @Test
    void patch_should_work() throws Exception {
        // Patch an existing document.
        {
            var created = service.create(host, new SampleEntity(getUuid(), "before"));
            try {
                var result = service.patch(host, created.id, PatchOperations.create().set("/name", "after"));
                assertThat(result.id).isEqualTo(created.id);
                assertThat(result.name).isEqualTo("after");
            } finally {
                service.purge(host, created.id);
            }
        }

        // Reject a blank document ID.
        {
            assertThatThrownBy(() -> service.patch(host, " ", PatchOperations.create()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("id should not be empty");
        }

        // Reject null patch operations.
        {
            assertThatThrownBy(() -> service.patch(host, getUuid(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("PatchOperations operations should not be null");
        }
    }

    @Test
    void bulkPatch_should_work() throws Exception {
        // Apply different operations to each document.
        {
            var created = createSampleEntities(2);
            try {
                var operations = List.of(
                        BulkPatchOperation.of(created.get(0).id, PatchOperations.create().set("/name", "patched-1")),
                        BulkPatchOperation.of(created.get(1).id, PatchOperations.create().set("/name", "patched-2")));

                var result = service.bulkPatch(host, operations);

                assertSuccessful(result, 2);
                assertThat(result.successList)
                        .extracting(data -> data.name)
                        .containsExactlyInAnyOrder("patched-1", "patched-2");
            } finally {
                purgeSampleEntities(created);
            }
        }

        // Apply per-item operations to more than 100 documents.
        {
            var created = createSampleEntities(101);
            try {
                var operations = created.stream()
                        .map(data -> BulkPatchOperation.of(
                                data.id, PatchOperations.create().set("/name", "patched-" + data.id)))
                        .toList();

                var result = service.bulkPatch(host, operations);

                assertSuccessful(result, 101);
                assertThat(result.successList)
                        .allSatisfy(data -> assertThat(data.name).isEqualTo("patched-" + data.id));
            } finally {
                purgeSampleEntities(created);
            }
        }

        // Return an empty result without calling the database.
        {
            var result = service.bulkPatch(host, List.of());

            assertSuccessful(result, 0);
        }

        // Reject a null item in the operation list.
        {
            var operations = new ArrayList<BulkPatchOperation>();
            operations.add(null);

            assertThatThrownBy(() -> service.bulkPatch(host, operations))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("BulkPatchOperation[0] should not be null");
        }

        // Reject an item without patch operations.
        {
            var operations = List.of(new BulkPatchOperation(getUuid(), null));

            assertThatThrownBy(() -> service.bulkPatch(host, operations))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("BulkPatchOperation[0].operations should not be null");
        }
    }

    private List<SampleEntity> createSampleEntities(int size) throws Exception {
        var result = new ArrayList<SampleEntity>();
        for (int i = 0; i < size; i++) {
            result.add(service.create(host, new SampleEntity(getUuid(), "before-" + i)));
        }
        return result;
    }

    private void purgeSampleEntities(List<SampleEntity> data) throws Exception {
        for (var item : data) {
            service.purge(host, item.id);
        }
    }

    private void assertSuccessful(BulkResult<SampleEntity> result, int expectedSize) {
        assertThat(result.fatalList).isEmpty();
        assertThat(result.retryList).isEmpty();
        assertThat(result.successList).hasSize(expectedSize);
    }

    private static class SampleEntityService extends BaseCRUDService<SampleEntity> {

        private SampleEntityService() {
            super(SampleEntity.class, "SampleEntities");
        }
    }

    public static class SampleEntity extends BaseData {

        public String name;

        public SampleEntity() {
        }

        private SampleEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
