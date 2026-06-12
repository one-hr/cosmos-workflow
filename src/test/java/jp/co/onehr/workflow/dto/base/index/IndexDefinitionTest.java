package jp.co.onehr.workflow.dto.base.index;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexDefinitionTest {

    @Test
    void indexDefinition_should_validate_supported_methods() {
        // Existing factory methods continue to create BTREE indexes.
        {
            var definition = IndexDefinition.of(IndexField.of("beginDate"));

            assertThat(definition.method).isEqualTo(IndexMethod.BTREE);
            assertThat(definition.unique).isFalse();
            assertThat(definition.fields).singleElement()
                    .satisfies(field -> assertThat(field.fieldName).isEqualTo("beginDate"));
        }

        // BOOLEAN is valid for scalar BTREE indexes.
        {
            var definition = IndexDefinition.of(IndexField.of("active", IndexFieldType.BOOLEAN));

            assertThat(definition.method).isEqualTo(IndexMethod.BTREE);
            assertThat(definition.unique).isFalse();
            assertThat(definition.fields).singleElement()
                    .satisfies(field -> {
                        assertThat(field.fieldName).isEqualTo("active");
                        assertThat(field.type).isEqualTo(IndexFieldType.BOOLEAN);
                    });
        }

        // GIN creates a non-unique index for one JSON-compatible field.
        {
            var definition = IndexDefinition.ofGin(IndexField.of("targetIdList", IndexFieldType.JSONB));

            assertThat(definition.method).isEqualTo(IndexMethod.GIN);
            assertThat(definition.unique).isFalse();
            assertThat(definition.fields).singleElement()
                    .satisfies(field -> {
                        assertThat(field.fieldName).isEqualTo("targetIdList");
                        assertThat(field.type).isEqualTo(IndexFieldType.JSONB);
                    });
        }

        // GIN does not support unique indexes.
        {
            assertThatThrownBy(() -> IndexDefinition.of(List.of(IndexField.of("targetIdList")), true, IndexMethod.GIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("GIN index does not support unique=true");
        }

        // GIN currently accepts only one field.
        {
            assertThatThrownBy(() -> IndexDefinition.of(List.of(IndexField.of("targetIdList"), IndexField.of("otherField")), false, IndexMethod.GIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("GIN index currently only supports a single field");
        }

        // Scalar field types cannot be used for a JSONB GIN expression.
        {
            assertThatThrownBy(() -> IndexDefinition.of(List.of(IndexField.of("targetIdList", IndexFieldType.NUMERIC)), false, IndexMethod.GIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("GIN index only supports TEXT or JSONB field type");
        }

        // BOOLEAN is scalar, so GIN should reject it.
        {
            assertThatThrownBy(() -> IndexDefinition.of(List.of(IndexField.of("active", IndexFieldType.BOOLEAN)), false, IndexMethod.GIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("GIN index only supports TEXT or JSONB field type");
        }
    }
}
