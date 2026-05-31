package dev.azhar.hishabi.calculators.tax.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;

class TaxEnumsJsonTest {

    private final ObjectMapper mapper = new JsonMapper();

    @Test
    void taxpayerCategorySerialisesAsUppercaseName() {
        String json = mapper.writeValueAsString(TaxpayerCategory.SENIOR_65_PLUS);
        assertThat(json).isEqualTo("\"SENIOR_65_PLUS\"");
    }

    @Test
    void taxpayerCategoryRoundtrips() {
        for (TaxpayerCategory value : TaxpayerCategory.values()) {
            String json = mapper.writeValueAsString(value);
            TaxpayerCategory back = mapper.readValue(json, TaxpayerCategory.class);
            assertThat(back).isEqualTo(value);
        }
    }

    @Test
    void locationRoundtrips() {
        for (Location value : Location.values()) {
            String json = mapper.writeValueAsString(value);
            Location back = mapper.readValue(json, Location.class);
            assertThat(back).isEqualTo(value);
        }
    }

    @Test
    void unknownTaxpayerCategoryValueFailsToDeserialise() {
        assertThatThrownBy(() -> mapper.readValue("\"NONESUCH\"", TaxpayerCategory.class))
                .isInstanceOf(InvalidFormatException.class);
    }
}
