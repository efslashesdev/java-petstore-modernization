package com.petstore.populate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.petstore.catalog.CatalogData;

class SeedDataReaderTest {

    private static SeedData seed;

    @BeforeAll
    static void readSeedFile() throws Exception {
        try (InputStream in = SeedDataReaderTest.class.getResourceAsStream("/seed/Populate-UTF8.xml")) {
            seed = new SeedDataReader().read(in);
        }
    }

    @Test
    void readsTheWholeLegacyDataSet() {
        assertThat(seed.users()).extracting(SeedData.SeedUser::username)
                .containsExactlyInAnyOrder("j2ee", "j2ee-ja", "shopper", "j2ee-zh");
        assertThat(seed.customers()).hasSize(4);
        assertThat(seed.catalog().categories()).hasSize(5);
        assertThat(seed.catalog().products()).hasSize(16);
        assertThat(seed.catalog().items()).hasSize(28);
    }

    @Test
    void localesAreStoredInLegacyForm() {
        CatalogData.CategoryRecord fish = seed.catalog().categories().getFirst();

        assertThat(fish.id()).isEqualTo("FISH");
        assertThat(fish.details()).containsOnlyKeys("en_US", "ja_JP", "zh_CN");
        assertThat(fish.details().get("ja_JP").name()).isEqualTo("魚");
    }

    @Test
    void itemPricesAndAttributesArePreservedExactly() {
        CatalogData.ItemRecord est1 = seed.catalog().items().getFirst();

        assertThat(est1.id()).isEqualTo("EST-1");
        assertThat(est1.seq()).isZero();
        assertThat(est1.details().get("en_US").listPrice()).isEqualByComparingTo("16.50");
        assertThat(est1.details().get("en_US").attributes()).containsExactly("Large", "Cuddly");
        assertThat(est1.details().get("ja_JP").listPrice()).isEqualByComparingTo("1951");
    }

    @Test
    void customerGraphIsFlattenedIntoTheAggregate() {
        var j2ee = seed.customers().stream().filter(c -> c.userId().equals("j2ee")).findFirst().orElseThrow();

        assertThat(j2ee.contactInfo().address().street1()).isEqualTo("1234 Anywhere Street");
        assertThat(j2ee.contactInfo().address().street2()).isEqualTo("Unit 555");
        assertThat(j2ee.creditCard().type()).isEqualTo("Meow Card");
        assertThat(j2ee.profile().favoriteCategory()).isEqualTo("REPTILES");
        assertThat(j2ee.profile().myList()).isTrue();
        assertThat(j2ee.profile().banners()).isFalse();
    }
}
